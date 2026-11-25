package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.provider.MediaStore
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.CachePolicy
import coil.request.Options
import coil.size.Dimension
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.math.max

/**
 * Fast Coil Fetcher for MediaStore content URIs at thumbnail sizes.
 * Combines a fast app-local disk thumbnail cache with Android's MediaStore thumbnail cache
 * (`ContentResolver.loadThumbnail` / legacy `Thumbnails`) and sampled fallback decoding.
 */
class MediaStoreThumbnailFetcher(
    private val uri: Uri,
    private val targetWidth: Int,
    private val targetHeight: Int,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val context = options.context
        val diskReadEnabled = options.diskCachePolicy.readEnabled
        val diskWriteEnabled = options.diskCachePolicy.writeEnabled
        val bucket = max(targetWidth, targetHeight).coerceIn(128, 1024)
        val cacheKey = hashKey("${uri}_$bucket")
        val cacheDir = File(context.cacheDir, "grid_thumb_cache")
        val cacheFile = File(cacheDir, "$cacheKey.jpg")

        if (diskReadEnabled && cacheFile.exists() && cacheFile.length() > 0L) {
            val cached = withContext(Dispatchers.IO) {
                runCatching {
                    val opts = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }
                    BitmapFactory.decodeFile(cacheFile.absolutePath, opts)
                }.getOrNull()
            }
            if (cached != null) {
                cacheFile.setLastModified(System.currentTimeMillis())
                return DrawableResult(
                    drawable = BitmapDrawable(context.resources, cached),
                    isSampled = true,
                    dataSource = DataSource.DISK
                )
            }
        }

        val cr = context.contentResolver
        val isVideo = uri.toString().contains("/video/")
        val signal = CancellationSignal()
        val jobHandle = currentCoroutineContext()[Job]?.invokeOnCompletion { cause ->
            if (cause != null) signal.cancel()
        }

        val bitmap: Bitmap? = try {
            withContext(Dispatchers.IO) {
                val systemThumb = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        cr.loadThumbnail(uri, Size(targetWidth, targetHeight), signal)
                    } else {
                        val id = runCatching { ContentUris.parseId(uri) }.getOrNull()
                        if (id != null) {
                            @Suppress("DEPRECATION")
                            if (isVideo) {
                                MediaStore.Video.Thumbnails.getThumbnail(
                                    cr,
                                    id,
                                    MediaStore.Video.Thumbnails.MINI_KIND,
                                    null
                                )
                            } else {
                                MediaStore.Images.Thumbnails.getThumbnail(
                                    cr,
                                    id,
                                    MediaStore.Images.Thumbnails.MINI_KIND,
                                    null
                                )
                            }
                        } else null
                    }
                } catch (ce: CancellationException) {
                    throw ce
                } catch (_: Throwable) {
                    null
                }

                systemThumb ?: decodeFallbackSampled(context, uri, isVideo, targetWidth, targetHeight)
            }
        } finally {
            jobHandle?.dispose()
        }

        if (bitmap != null && diskWriteEnabled) {
            withContext(Dispatchers.IO) {
                writeThumbToCache(cacheDir, cacheFile, bitmap)
            }
        }

        return bitmap?.let {
            DrawableResult(
                drawable = BitmapDrawable(context.resources, it),
                isSampled = true,
                dataSource = DataSource.DISK
            )
        }
    }

    private fun decodeFallbackSampled(
        context: Context,
        uri: Uri,
        isVideo: Boolean,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        if (isVideo) {
            val retriever = MediaMetadataRetriever()
            return try {
                retriever.setDataSource(context, uri)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    retriever.getScaledFrameAtTime(
                        -1L,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        reqWidth,
                        reqHeight
                    )
                } else {
                    retriever.frameAtTime
                }
            } catch (_: Throwable) {
                null
            } finally {
                runCatching { retriever.release() }
            }
        }

        val cr = context.contentResolver
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val decoded = runCatching {
                    val src = android.graphics.ImageDecoder.createSource(cr, uri)
                    android.graphics.ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
                        val w = info.size.width
                        val h = info.size.height
                        if (w > 0 && h > 0) {
                            val ratio = minOf(
                                reqWidth.toFloat() / w.toFloat(),
                                reqHeight.toFloat() / h.toFloat(),
                                1f
                            )
                            decoder.setTargetSize(
                                (w * ratio).toInt().coerceAtLeast(1),
                                (h * ratio).toInt().coerceAtLeast(1)
                            )
                        }
                    }
                }.getOrNull()
                if (decoded != null) return decoded
            }

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            cr.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, reqWidth, reqHeight)
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            cr.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun calculateInSampleSize(srcW: Int, srcH: Int, reqW: Int, reqH: Int): Int {
        if (srcW <= 0 || srcH <= 0) return 4
        var inSampleSize = 1
        if (srcH > reqH || srcW > reqW) {
            val halfH = srcH / 2
            val halfW = srcW / 2
            while ((halfH / inSampleSize) >= reqH && (halfW / inSampleSize) >= reqW) {
                inSampleSize *= 2
            }
        }
        return max(1, inSampleSize)
    }

    private fun writeThumbToCache(cacheDir: File, targetFile: File, bitmap: Bitmap) {
        runCatching {
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val tempFile = File(cacheDir, "${targetFile.name}.tmp")
            FileOutputStream(tempFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            if (!tempFile.renameTo(targetFile)) {
                tempFile.delete()
            }
            trimCacheIfNeeded(cacheDir)
        }
    }

    private fun trimCacheIfNeeded(cacheDir: File) {
        synchronized(CacheTrimLock) {
            val now = System.currentTimeMillis()
            if (now - lastTrimAtMs < 30_000L) return
            lastTrimAtMs = now
        }
        val files = cacheDir.listFiles() ?: return
        if (files.size <= MAX_CACHED_FILES) return
        val sorted = files.sortedBy { it.lastModified() }
        val toDelete = sorted.size - MAX_CACHED_FILES
        for (i in 0 until toDelete) {
            sorted[i].delete()
        }
    }

    private fun hashKey(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        val sb = StringBuilder(digest.size * 2)
        for (b in digest) {
            sb.append(HEX[(b.toInt() ushr 4) and 0xF])
            sb.append(HEX[b.toInt() and 0xF])
        }
        return sb.toString()
    }

    class Factory : Fetcher.Factory<Uri> {
        override fun create(
            data: Uri,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher? {
            if (data.scheme != "content" || data.authority != MediaStore.AUTHORITY) {
                return null
            }
            val widthPx = (options.size.width as? Dimension.Pixels)?.px ?: return null
            val heightPx = (options.size.height as? Dimension.Pixels)?.px ?: return null
            if (widthPx !in 1..1024 || heightPx !in 1..1024) {
                return null
            }
            return MediaStoreThumbnailFetcher(data, widthPx, heightPx, options)
        }
    }

    private companion object {
        const val MAX_CACHED_FILES = 2500
        val HEX = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')
        val CacheTrimLock = Any()

        @Volatile
        var lastTrimAtMs: Long = 0L
    }
}
