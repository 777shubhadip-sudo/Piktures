package com.example.data.repository

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.local.MoveState
import com.example.data.local.VaultDao
import com.example.data.local.VaultMediaEntity
import com.example.domain.model.GalleryMedia
import com.example.domain.model.MediaType
import com.example.domain.model.VaultMedia
import com.example.security.VaultCryptoManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class StagedHideBatch(
    val vaultIds: List<String>,
    val sourceUris: List<Uri>
)

@Singleton
class VaultRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vaultDao: VaultDao,
    private val cryptoManager: VaultCryptoManager,
    private val mediaStoreRepository: MediaStoreRepository
) {
    private val contentResolver: ContentResolver = context.contentResolver

    private val level1Dir: File by lazy {
        File(context.filesDir, "vault/level1").apply { mkdirs() }
    }
    private val level2Dir: File by lazy {
        File(context.filesDir, "vault/level2").apply { mkdirs() }
    }

    fun getEncryptedFile(level: Int, fileName: String): File {
        val dir = if (level == 2) level2Dir else level1Dir
        return File(dir, fileName)
    }

    fun observeLevelMedia(level: Int): Flow<List<VaultMedia>> =
        vaultDao.observeVaultMediaByLevel(level).map { list -> list.map { it.toDomain() } }

    suspend fun recoverIncompleteMoves() = withContext(Dispatchers.IO) {
        val uncommitted = vaultDao.getUncommittedMoves()
        for (entity in uncommitted) {
            val encFile = getEncryptedFile(entity.level, entity.encryptedFileName)
            val thumbFile = getEncryptedFile(entity.level, entity.encryptedThumbName)
            val originalStillExists = mediaStoreRepository.uriExists(entity.originalUriString)

            if (entity.moveState == MoveState.VERIFIED.name && !originalStillExists) {
                val ok = cryptoManager.verifyEncryptedFile(
                    encFile,
                    entity.sizeBytes,
                    entity.sha256Checksum
                )
                if (ok) {
                    vaultDao.updateMoveState(entity.id, MoveState.COMMITTED.name)
                } else {
                    cryptoManager.secureWipeAndDelete(encFile)
                    cryptoManager.secureWipeAndDelete(thumbFile)
                    vaultDao.deleteVaultMediaById(entity.id)
                }
            } else {
                // Original still exists or copy never finished -> safely roll back partial vault copy
                cryptoManager.secureWipeAndDelete(encFile)
                cryptoManager.secureWipeAndDelete(thumbFile)
                vaultDao.deleteVaultMediaById(entity.id)
            }
        }
    }

    /**
     * Step 1 & 2 of Hide flow:
     * Encrypt original media + thumbnail into level 1 vault, verify GCM tags & SHA-256 checksum,
     * and record VERIFIED state in Room. Does NOT delete originals yet.
     */
    suspend fun stageHideToLevel1(items: List<GalleryMedia>): StagedHideBatch =
        withContext(Dispatchers.IO) {
            val stagedIds = mutableListOf<String>()
            val verifiedUris = mutableListOf<Uri>()

            for (item in items) {
                val id = UUID.randomUUID().toString()
                val randomSuffix = UUID.randomUUID().toString().replace("-", "")
                val encFileName = "v_$randomSuffix.enc"
                val encThumbName = "t_$randomSuffix.enc"
                val destFile = getEncryptedFile(1, encFileName)
                val destThumb = getEncryptedFile(1, encThumbName)

                try {
                    val encResult = contentResolver.openInputStream(item.uri)?.use { input ->
                        cryptoManager.encryptStreamToFile(input, destFile)
                    } ?: continue

                    val thumbBytes = generateThumbnailBytes(item)
                    if (thumbBytes != null) {
                        cryptoManager.encryptBytesToFile(thumbBytes, destThumb)
                        thumbBytes.fill(0)
                    }

                    val initialEntity = VaultMediaEntity(
                        id = id,
                        level = 1,
                        encryptedFileName = encFileName,
                        encryptedThumbName = encThumbName,
                        originalName = item.displayName,
                        originalRelativePath = item.relativePath,
                        originalUriString = item.uri.toString(),
                        mimeType = item.mimeType,
                        mediaType = item.mediaType.name,
                        sizeBytes = encResult.plaintextSize,
                        width = item.width,
                        height = item.height,
                        durationMs = item.durationMs,
                        dateTakenMs = item.effectiveDateMs,
                        addedToVaultMs = System.currentTimeMillis(),
                        sha256Checksum = encResult.sha256Hex,
                        moveState = MoveState.COPYING.name
                    )
                    vaultDao.insertVaultMedia(initialEntity)

                    val verified = cryptoManager.verifyEncryptedFile(
                        destFile,
                        encResult.plaintextSize,
                        encResult.sha256Hex
                    )
                    if (!verified) {
                        cryptoManager.secureWipeAndDelete(destFile)
                        cryptoManager.secureWipeAndDelete(destThumb)
                        vaultDao.deleteVaultMediaById(id)
                        continue
                    }

                    vaultDao.updateMoveState(id, MoveState.VERIFIED.name)
                    stagedIds.add(id)
                    verifiedUris.add(item.uri)
                } catch (_: Throwable) {
                    cryptoManager.secureWipeAndDelete(destFile)
                    cryptoManager.secureWipeAndDelete(destThumb)
                    vaultDao.deleteVaultMediaById(id)
                }
            }
            StagedHideBatch(vaultIds = stagedIds, sourceUris = verifiedUris)
        }

    suspend fun commitHideBatch(vaultIds: List<String>) = withContext(Dispatchers.IO) {
        for (id in vaultIds) {
            vaultDao.updateMoveState(id, MoveState.COMMITTED.name)
        }
    }

    suspend fun rollbackHideBatch(vaultIds: List<String>) = withContext(Dispatchers.IO) {
        for (id in vaultIds) {
            val entity = vaultDao.getById(id) ?: continue
            cryptoManager.secureWipeAndDelete(getEncryptedFile(entity.level, entity.encryptedFileName))
            cryptoManager.secureWipeAndDelete(getEncryptedFile(entity.level, entity.encryptedThumbName))
            vaultDao.deleteVaultMediaById(id)
        }
    }

    /**
     * Move encrypted media & thumbnail from Level 1 to Level 2 (Double Hidden Folder).
     */
    suspend fun moveToDoubleHiddenFolder(vaultId: String): Boolean = withContext(Dispatchers.IO) {
        val entity = vaultDao.getById(vaultId) ?: return@withContext false
        if (entity.level == 2) return@withContext true

        val srcFile = getEncryptedFile(1, entity.encryptedFileName)
        val srcThumb = getEncryptedFile(1, entity.encryptedThumbName)
        val dstFile = getEncryptedFile(2, entity.encryptedFileName)
        val dstThumb = getEncryptedFile(2, entity.encryptedThumbName)

        return@withContext try {
            dstFile.parentFile?.mkdirs()
            srcFile.copyTo(dstFile, overwrite = true)
            if (srcThumb.exists()) {
                srcThumb.copyTo(dstThumb, overwrite = true)
            }

            val verified = cryptoManager.verifyEncryptedFile(
                dstFile,
                entity.sizeBytes,
                entity.sha256Checksum
            )
            if (!verified) {
                cryptoManager.secureWipeAndDelete(dstFile)
                cryptoManager.secureWipeAndDelete(dstThumb)
                return@withContext false
            }

            vaultDao.updateLevel(entity.id, 2)
            cryptoManager.secureWipeAndDelete(srcFile)
            cryptoManager.secureWipeAndDelete(srcThumb)
            true
        } catch (_: Throwable) {
            cryptoManager.secureWipeAndDelete(dstFile)
            cryptoManager.secureWipeAndDelete(dstThumb)
            false
        }
    }

    /**
     * Decrypt and restore item back to its original folder in MediaStore, then wipe encrypted vault copy.
     */
    suspend fun unhideAndRestore(vaultId: String): Boolean = withContext(Dispatchers.IO) {
        val entity = vaultDao.getById(vaultId) ?: return@withContext false
        val encFile = getEncryptedFile(entity.level, entity.encryptedFileName)
        val encThumb = getEncryptedFile(entity.level, entity.encryptedThumbName)
        if (!encFile.exists()) return@withContext false

        val isVideo = entity.mediaType == MediaType.VIDEO.name
        val defaultDir = if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
        val targetRelPath = entity.originalRelativePath
            .takeIf { it.isNotBlank() }
            ?: "$defaultDir/PikturesRestored/"

        return@withContext try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val collection = if (isVideo) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                }
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, entity.originalName)
                    put(MediaStore.MediaColumns.MIME_TYPE, entity.mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, targetRelPath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val newUri = contentResolver.insert(collection, values) ?: return@withContext false
                try {
                    val written = contentResolver.openOutputStream(newUri)?.use { out ->
                        cryptoManager.decryptToStream(encFile, out)
                    } ?: throw IllegalStateException("Cannot write restored MediaStore file")

                    if (written != entity.sizeBytes) {
                        throw IllegalStateException("Restored byte count mismatch")
                    }
                    val finish = ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_PENDING, 0)
                    }
                    contentResolver.update(newUri, finish, null, null)
                } catch (t: Throwable) {
                    contentResolver.delete(newUri, null, null)
                    throw t
                }
            } else {
                @Suppress("DEPRECATION")
                val baseDir = Environment.getExternalStoragePublicDirectory(defaultDir)
                val outDir = File(baseDir, "PikturesRestored").apply { mkdirs() }
                val outFile = File(outDir, entity.originalName)
                val written = FileOutputStream(outFile).use { out ->
                    cryptoManager.decryptToStream(encFile, out)
                }
                if (written != entity.sizeBytes) {
                    outFile.delete()
                    return@withContext false
                }
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, entity.originalName)
                    put(MediaStore.MediaColumns.MIME_TYPE, entity.mimeType)
                    @Suppress("DEPRECATION")
                    put(MediaStore.MediaColumns.DATA, outFile.absolutePath)
                }
                val collection = if (isVideo) {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
                contentResolver.insert(collection, values)
            }

            cryptoManager.secureWipeAndDelete(encFile)
            cryptoManager.secureWipeAndDelete(encThumb)
            vaultDao.deleteVaultMediaById(entity.id)
            true
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun deletePermanentlyFromVault(vaultIds: List<String>) = withContext(Dispatchers.IO) {
        for (id in vaultIds) {
            val entity = vaultDao.getById(id) ?: continue
            cryptoManager.secureWipeAndDelete(getEncryptedFile(entity.level, entity.encryptedFileName))
            cryptoManager.secureWipeAndDelete(getEncryptedFile(entity.level, entity.encryptedThumbName))
            vaultDao.deleteVaultMediaById(id)
        }
    }

    private fun generateThumbnailBytes(item: GalleryMedia): ByteArray? {
        return try {
            val bitmap: Bitmap? = if (item.mediaType == MediaType.VIDEO) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, item.uri)
                    retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                } finally {
                    retriever.release()
                }
            } else {
                contentResolver.openInputStream(item.uri)?.use { stream ->
                    val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(stream, null, boundsOpts)
                    val maxDim = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
                    val sample = if (maxDim > 360) (maxDim / 360).coerceAtLeast(1) else 1
                    contentResolver.openInputStream(item.uri)?.use { stream2 ->
                        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
                        BitmapFactory.decodeStream(stream2, null, decodeOpts)
                    }
                }
            }
            if (bitmap == null) return null
            val scaled = scaleDownBitmap(bitmap, 360)
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 82, baos)
            if (scaled !== bitmap) scaled.recycle()
            bitmap.recycle()
            baos.toByteArray()
        } catch (_: Throwable) {
            null
        }
    }

    private fun scaleDownBitmap(src: Bitmap, maxEdge: Int): Bitmap {
        val w = src.width
        val h = src.height
        if (w <= maxEdge && h <= maxEdge) return src
        val ratio = maxEdge.toFloat() / maxOf(w, h).toFloat()
        val nw = (w * ratio).toInt().coerceAtLeast(1)
        val nh = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, nw, nh, true)
    }
}
