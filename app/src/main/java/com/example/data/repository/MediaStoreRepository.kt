package com.example.data.repository

import android.Manifest
import android.app.PendingIntent
import android.app.WallpaperManager
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.example.domain.model.GalleryMedia
import com.example.domain.model.MediaDetails
import com.example.domain.model.MediaPermissionState
import com.example.domain.model.MediaType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val contentResolver: ContentResolver = context.contentResolver

    fun checkPermissionState(): MediaPermissionState {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                val hasImages = hasPerm(Manifest.permission.READ_MEDIA_IMAGES)
                val hasVideo = hasPerm(Manifest.permission.READ_MEDIA_VIDEO)
                val hasPartial = hasPerm(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                when {
                    hasImages && hasVideo -> MediaPermissionState.FULL_ACCESS
                    hasPartial || hasImages || hasVideo -> MediaPermissionState.PARTIAL_ACCESS
                    else -> MediaPermissionState.DENIED
                }
            }
            Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU -> {
                val hasImages = hasPerm(Manifest.permission.READ_MEDIA_IMAGES)
                val hasVideo = hasPerm(Manifest.permission.READ_MEDIA_VIDEO)
                when {
                    hasImages && hasVideo -> MediaPermissionState.FULL_ACCESS
                    hasImages || hasVideo -> MediaPermissionState.PARTIAL_ACCESS
                    else -> MediaPermissionState.DENIED
                }
            }
            else -> {
                if (hasPerm(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    MediaPermissionState.FULL_ACCESS
                } else {
                    MediaPermissionState.DENIED
                }
            }
        }
    }

    fun requiredPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                Manifest.permission.ACCESS_MEDIA_LOCATION
            )
            Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.ACCESS_MEDIA_LOCATION
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_MEDIA_LOCATION
            )
            else -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    private fun hasPerm(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private sealed interface RefreshSignal {
        data object ResetAndFirstPage : RefreshSignal
        data class LoadMore(val loadAll: Boolean) : RefreshSignal
    }

    private val refreshSignalTrigger = Channel<RefreshSignal>(Channel.UNLIMITED)
    private val _hasMoreMedia = kotlinx.coroutines.flow.MutableStateFlow(false)
    val hasMoreMedia: kotlinx.coroutines.flow.StateFlow<Boolean> = _hasMoreMedia

    @Volatile
    private var lastLoadedStamp: String? = null

    @Volatile
    private var lastLoadedPermState: MediaPermissionState? = null

    private fun currentMediaStoreStamp(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                "${MediaStore.getGeneration(context, MediaStore.VOLUME_EXTERNAL)}:${MediaStore.getVersion(context)}"
            } else {
                MediaStore.getVersion(context)
            }
        } catch (_: Throwable) {
            ""
        }
    }

    fun requestRefresh() {
        lastLoadedStamp = null
        refreshSignalTrigger.trySend(RefreshSignal.ResetAndFirstPage)
    }

    fun requestLoadNextPage() {
        if (_hasMoreMedia.value) {
            refreshSignalTrigger.trySend(RefreshSignal.LoadMore(loadAll = false))
        }
    }

    fun requestLoadAllRemaining() {
        if (_hasMoreMedia.value) {
            refreshSignalTrigger.trySend(RefreshSignal.LoadMore(loadAll = true))
        }
    }

    fun requestRefreshIfChanged() {
        val currentPerm = checkPermissionState()
        if (currentPerm == MediaPermissionState.DENIED) {
            requestRefresh()
            return
        }
        val currentStamp = currentMediaStoreStamp()
        val prevStamp = lastLoadedStamp
        if (lastLoadedPermState != currentPerm ||
            prevStamp == null ||
            currentStamp.isEmpty() ||
            currentStamp != prevStamp
        ) {
            refreshSignalTrigger.trySend(RefreshSignal.ResetAndFirstPage)
        }
    }

    fun observeGalleryMedia(): Flow<List<GalleryMedia>> = callbackFlow {
        val observerTrigger = Channel<Unit>(Channel.CONFLATED)

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                lastLoadedStamp = null
                observerTrigger.trySend(Unit)
            }
        }

        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )
        contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )

        val worker = launch(Dispatchers.IO) {
            var loadedImages = emptyList<GalleryMedia>()
            var loadedVideos = emptyList<GalleryMedia>()
            var imageOffset = 0
            var videoOffset = 0
            var hasMoreImages = false
            var hasMoreVideos = false

            suspend fun loadInitialPage() {
                val perm = checkPermissionState()
                if (perm == MediaPermissionState.DENIED) {
                    loadedImages = emptyList()
                    loadedVideos = emptyList()
                    imageOffset = 0
                    videoOffset = 0
                    hasMoreImages = false
                    hasMoreVideos = false
                    _hasMoreMedia.value = false
                    lastLoadedPermState = perm
                    trySend(emptyList())
                    return
                }
                val stampBefore = currentMediaStoreStamp()
                val (firstImages, firstVideos) = queryImagesAndVideosParallel(
                    trashedOnly = false,
                    imageLimit = INITIAL_IMAGE_BATCH,
                    imageOffset = 0,
                    videoLimit = INITIAL_VIDEO_BATCH,
                    videoOffset = 0
                )
                loadedImages = firstImages
                loadedVideos = firstVideos
                imageOffset = firstImages.size
                videoOffset = firstVideos.size
                hasMoreImages = firstImages.size >= INITIAL_IMAGE_BATCH
                hasMoreVideos = firstVideos.size >= INITIAL_VIDEO_BATCH
                _hasMoreMedia.value = hasMoreImages || hasMoreVideos
                lastLoadedStamp = currentMediaStoreStamp().ifEmpty { stampBefore }
                lastLoadedPermState = perm
                trySend(mergeDescending(loadedImages, loadedVideos))
            }

            suspend fun loadNextPageBatch() {
                if (!hasMoreImages && !hasMoreVideos) {
                    _hasMoreMedia.value = false
                    return
                }
                val imgLimit = if (hasMoreImages) PAGE_IMAGE_BATCH else 0
                val vidLimit = if (hasMoreVideos) PAGE_VIDEO_BATCH else 0
                val (nextImgs, nextVids) = queryImagesAndVideosParallel(
                    trashedOnly = false,
                    imageLimit = imgLimit.takeIf { it > 0 },
                    imageOffset = imageOffset,
                    videoLimit = vidLimit.takeIf { it > 0 },
                    videoOffset = videoOffset
                )
                val filteredImgs = if (imgLimit > 0) nextImgs else emptyList()
                val filteredVids = if (vidLimit > 0) nextVids else emptyList()

                if (filteredImgs.isNotEmpty()) {
                    loadedImages = loadedImages + filteredImgs
                    imageOffset += filteredImgs.size
                }
                if (filteredVids.isNotEmpty()) {
                    loadedVideos = loadedVideos + filteredVids
                    videoOffset += filteredVids.size
                }
                hasMoreImages = imgLimit > 0 && filteredImgs.size >= imgLimit
                hasMoreVideos = vidLimit > 0 && filteredVids.size >= vidLimit
                _hasMoreMedia.value = hasMoreImages || hasMoreVideos
                if (filteredImgs.isNotEmpty() || filteredVids.isNotEmpty()) {
                    trySend(mergeDescending(loadedImages, loadedVideos))
                }
            }

            suspend fun drainAllRemainingBatches(): Boolean {
                while (hasMoreImages || hasMoreVideos) {
                    val incoming = refreshSignalTrigger.tryReceive().getOrNull()
                    if (incoming is RefreshSignal.ResetAndFirstPage) {
                        // Re-enqueue so the main signal loop processes the full refresh immediately
                        refreshSignalTrigger.trySend(incoming)
                        return false
                    }
                    loadNextPageBatch()
                    delay(16L)
                    kotlinx.coroutines.yield()
                }
                return true
            }

            // Coalesce ContentObserver bursts into ResetAndFirstPage
            launch {
                for (unit in observerTrigger) {
                    delay(400L)
                    while (observerTrigger.tryReceive().isSuccess) { /* drain burst */ }
                    lastLoadedStamp = null
                    refreshSignalTrigger.trySend(RefreshSignal.ResetAndFirstPage)
                }
            }

            // Cold-start Page 0: render first screen immediately, then eagerly fetch all remaining media
            loadInitialPage()
            if (hasMoreImages || hasMoreVideos) {
                delay(24L)
                drainAllRemainingBatches()
            }

            for (sig in refreshSignalTrigger) {
                var needReset = sig is RefreshSignal.ResetAndFirstPage
                var needLoadMore = sig is RefreshSignal.LoadMore
                var needLoadAll = (sig as? RefreshSignal.LoadMore)?.loadAll == true

                while (true) {
                    val next = refreshSignalTrigger.tryReceive().getOrNull() ?: break
                    when (next) {
                        is RefreshSignal.ResetAndFirstPage -> needReset = true
                        is RefreshSignal.LoadMore -> {
                            needLoadMore = true
                            if (next.loadAll) needLoadAll = true
                        }
                    }
                }

                if (needReset) {
                    loadInitialPage()
                    if (hasMoreImages || hasMoreVideos) {
                        delay(20L)
                        drainAllRemainingBatches()
                    }
                }
                if (needLoadMore && (hasMoreImages || hasMoreVideos)) {
                    if (needLoadAll) {
                        drainAllRemainingBatches()
                    } else {
                        loadNextPageBatch()
                    }
                }
            }
        }

        awaitClose {
            worker.cancel()
            observerTrigger.close()
            contentResolver.unregisterContentObserver(observer)
        }
    }.conflate().flowOn(Dispatchers.IO)

    suspend fun loadGalleryMedia(): List<GalleryMedia> = withContext(Dispatchers.IO) {
        if (checkPermissionState() == MediaPermissionState.DENIED) return@withContext emptyList()
        val (images, videos) = queryImagesAndVideosParallel(
            trashedOnly = false,
            imageLimit = null,
            imageOffset = 0,
            videoLimit = null,
            videoOffset = 0
        )
        mergeDescending(images, videos)
    }

    suspend fun loadTrashedMedia(): List<GalleryMedia> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
            checkPermissionState() == MediaPermissionState.DENIED
        ) {
            return@withContext emptyList()
        }
        val (images, videos) = queryImagesAndVideosParallel(
            trashedOnly = true,
            imageLimit = null,
            imageOffset = 0,
            videoLimit = null,
            videoOffset = 0
        )
        mergeDescending(images, videos)
    }

    private suspend fun queryImagesAndVideosParallel(
        trashedOnly: Boolean,
        imageLimit: Int?,
        imageOffset: Int = 0,
        videoLimit: Int?,
        videoOffset: Int = 0
    ): Pair<List<GalleryMedia>, List<GalleryMedia>> = coroutineScope {
        val canReadImages = canQueryImages() && (imageLimit == null || imageLimit > 0)
        val canReadVideos = canQueryVideos() && (videoLimit == null || videoLimit > 0)

        val imagesDeferred = async(Dispatchers.IO) {
            if (canReadImages) {
                querySingleCollection(
                    isVideo = false,
                    trashedOnly = trashedOnly,
                    limit = imageLimit,
                    offset = imageOffset
                )
            } else emptyList()
        }
        val videosDeferred = async(Dispatchers.IO) {
            if (canReadVideos) {
                querySingleCollection(
                    isVideo = true,
                    trashedOnly = trashedOnly,
                    limit = videoLimit,
                    offset = videoOffset
                )
            } else emptyList()
        }
        imagesDeferred.await() to videosDeferred.await()
    }

    private fun canQueryImages(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                hasPerm(Manifest.permission.READ_MEDIA_IMAGES) ||
                    hasPerm(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU ->
                hasPerm(Manifest.permission.READ_MEDIA_IMAGES)
            else -> hasPerm(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun canQueryVideos(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                hasPerm(Manifest.permission.READ_MEDIA_VIDEO) ||
                    hasPerm(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU ->
                hasPerm(Manifest.permission.READ_MEDIA_VIDEO)
            else -> hasPerm(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun mergeDescending(
        images: List<GalleryMedia>,
        videos: List<GalleryMedia>
    ): List<GalleryMedia> {
        if (videos.isEmpty()) return images
        if (images.isEmpty()) return videos
        val imgCount = images.size
        val vidCount = videos.size
        val merged = ArrayList<GalleryMedia>(imgCount + vidCount)
        var i = 0
        var j = 0
        while (i < imgCount && j < vidCount) {
            val img = images[i]
            val vid = videos[j]
            if (img.dateAddedSec >= vid.dateAddedSec) {
                merged.add(img)
                i++
            } else {
                merged.add(vid)
                j++
            }
        }
        while (i < imgCount) {
            merged.add(images[i++])
        }
        while (j < vidCount) {
            merged.add(videos[j++])
        }
        return merged
    }

    private fun querySingleCollection(
        isVideo: Boolean,
        trashedOnly: Boolean,
        limit: Int?,
        offset: Int = 0
    ): List<GalleryMedia> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (isVideo) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            else MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val baseItemUri = if (isVideo) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val projection = if (isVideo) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (trashedOnly) VIDEO_PROJECTION_R_TRASHED else VIDEO_PROJECTION_R
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                VIDEO_PROJECTION_Q
            } else {
                VIDEO_PROJECTION_LEGACY
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (trashedOnly) IMAGE_PROJECTION_R_TRASHED else IMAGE_PROJECTION_R
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                IMAGE_PROJECTION_Q
            } else {
                IMAGE_PROJECTION_LEGACY
            }
        }

        var providerAppliedLimitOffset = true
        val cursor = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                fun buildQueryArgs(includeLimit: Boolean): Bundle = Bundle().apply {
                    putString(
                        ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
                        "${MediaStore.MediaColumns.DATE_ADDED} DESC"
                    )
                    if (trashedOnly) {
                        putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
                    } else {
                        putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_EXCLUDE)
                    }
                    if (includeLimit && limit != null) {
                        putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                        if (offset > 0) {
                            putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
                        }
                    }
                }
                try {
                    contentResolver.query(collection, projection, buildQueryArgs(includeLimit = true), null)
                } catch (_: IllegalArgumentException) {
                    providerAppliedLimitOffset = false
                    contentResolver.query(collection, projection, buildQueryArgs(includeLimit = false), null)
                }
            } else {
                if (trashedOnly) return emptyList()
                val baseSort = "${MediaStore.MediaColumns.DATE_ADDED} DESC"
                if (limit != null) {
                    try {
                        val clause = if (offset > 0) "$baseSort LIMIT $limit OFFSET $offset" else "$baseSort LIMIT $limit"
                        contentResolver.query(collection, projection, null, null, clause)
                    } catch (_: Throwable) {
                        providerAppliedLimitOffset = false
                        contentResolver.query(collection, projection, null, null, baseSort)
                    }
                } else {
                    contentResolver.query(collection, projection, null, null, baseSort)
                }
            }
        } catch (_: SecurityException) {
            null
        } catch (_: Throwable) {
            null
        } ?: return emptyList()

        cursor.use { c ->
            val count = c.count.coerceAtLeast(0)
            if (count == 0) return emptyList()
            val targetCapacity = if (limit != null) minOf(limit, count) else count
            val result = ArrayList<GalleryMedia>(targetCapacity)

            val idCol = c.getColumnIndex(MediaStore.MediaColumns._ID)
            if (idCol < 0) return result
            val nameCol = c.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeCol = c.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
            val sizeCol = c.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val widthCol = c.getColumnIndex(MediaStore.MediaColumns.WIDTH)
            val heightCol = c.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
            val dateAddedCol = c.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
            val dateTakenCol = c.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
            val bucketIdCol = c.getColumnIndex(MediaStore.MediaColumns.BUCKET_ID)
            val bucketNameCol = c.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            val durationCol = if (isVideo) c.getColumnIndex(MediaStore.Video.Media.DURATION) else -1
            val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                c.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
            } else {
                @Suppress("DEPRECATION")
                c.getColumnIndex(MediaStore.MediaColumns.DATA)
            }
            val trashedCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                c.getColumnIndex(MediaStore.MediaColumns.IS_TRASHED)
            } else -1
            val expiresCol = if (trashedOnly && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                c.getColumnIndex(MediaStore.MediaColumns.DATE_EXPIRES)
            } else -1

            val defaultMime = if (isVideo) "video/mp4" else "image/jpeg"
            val mediaTypeEnum = if (isVideo) MediaType.VIDEO else MediaType.IMAGE
            val isQOrAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

            if (!providerAppliedLimitOffset && offset > 0) {
                if (!c.move(offset)) return result
            }

            while (c.moveToNext()) {
                if (limit != null && result.size >= limit) break
                try {
                    val isTrashed = trashedCol >= 0 && c.getInt(trashedCol) == 1
                    if (trashedOnly && !isTrashed) continue
                    if (!trashedOnly && isTrashed) continue

                    val id = c.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(baseItemUri, id)
                    val displayName = if (nameCol >= 0) c.getString(nameCol) ?: "Media_$id" else "Media_$id"
                    val mimeType = if (mimeCol >= 0) c.getString(mimeCol) ?: defaultMime else defaultMime
                    val sizeBytes = if (sizeCol >= 0) c.getLong(sizeCol) else 0L
                    val width = if (widthCol >= 0) c.getInt(widthCol) else 0
                    val height = if (heightCol >= 0) c.getInt(heightCol) else 0
                    val dateAdded = if (dateAddedCol >= 0) c.getLong(dateAddedCol) else 0L
                    val dateTaken = if (dateTakenCol >= 0) c.getLong(dateTakenCol) else 0L
                    val bucketId = if (bucketIdCol >= 0) c.getLong(bucketIdCol) else 0L
                    val bucketName = if (bucketNameCol >= 0) c.getString(bucketNameCol) ?: "Camera" else "Camera"
                    val duration = if (durationCol >= 0) c.getLong(durationCol) else 0L

                    val rawPath = if (relPathCol >= 0) c.getString(relPathCol) ?: "" else ""
                    val relativePath = if (isQOrAbove) {
                        rawPath
                    } else {
                        val lastSlash = rawPath.lastIndexOf('/')
                        if (lastSlash >= 0) rawPath.substring(0, lastSlash + 1) else ""
                    }

                    val expires = if (expiresCol >= 0 && !c.isNull(expiresCol)) c.getLong(expiresCol) else null

                    result.add(
                        GalleryMedia(
                            id = id,
                            uri = contentUri,
                            displayName = displayName,
                            relativePath = relativePath,
                            bucketId = bucketId,
                            bucketName = bucketName,
                            mimeType = mimeType,
                            mediaType = mediaTypeEnum,
                            sizeBytes = sizeBytes,
                            width = width,
                            height = height,
                            durationMs = duration,
                            dateAddedSec = dateAdded,
                            dateTakenMs = dateTaken,
                            isTrashed = isTrashed,
                            dateExpiresSec = expires
                        )
                    )
                } catch (_: Throwable) {
                    // Skip malformed MediaStore row safely
                }
            }
            return result
        }
    }

    private companion object {
        const val INITIAL_IMAGE_BATCH = 60
        const val INITIAL_VIDEO_BATCH = 24
        const val PAGE_IMAGE_BATCH = 360
        const val PAGE_VIDEO_BATCH = 100

        private val BASE_IMAGE_COLS = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
        )

        @Suppress("DEPRECATION")
        private val IMAGE_PROJECTION_LEGACY = BASE_IMAGE_COLS + MediaStore.MediaColumns.DATA
        private val IMAGE_PROJECTION_Q = BASE_IMAGE_COLS + MediaStore.MediaColumns.RELATIVE_PATH
        private val IMAGE_PROJECTION_R = IMAGE_PROJECTION_Q + MediaStore.MediaColumns.IS_TRASHED
        private val IMAGE_PROJECTION_R_TRASHED = IMAGE_PROJECTION_R + MediaStore.MediaColumns.DATE_EXPIRES

        private val BASE_VIDEO_COLS = BASE_IMAGE_COLS + MediaStore.Video.Media.DURATION
        @Suppress("DEPRECATION")
        private val VIDEO_PROJECTION_LEGACY = BASE_VIDEO_COLS + MediaStore.MediaColumns.DATA
        private val VIDEO_PROJECTION_Q = BASE_VIDEO_COLS + MediaStore.MediaColumns.RELATIVE_PATH
        private val VIDEO_PROJECTION_R = VIDEO_PROJECTION_Q + MediaStore.MediaColumns.IS_TRASHED
        private val VIDEO_PROJECTION_R_TRASHED = VIDEO_PROJECTION_R + MediaStore.MediaColumns.DATE_EXPIRES
    }

    suspend fun getMediaDetails(item: GalleryMedia): MediaDetails = withContext(Dispatchers.IO) {
        var makeModel: String? = null
        var aperture: String? = null
        var shutter: String? = null
        var iso: String? = null
        var focal: String? = null

        if (item.mediaType == MediaType.IMAGE) {
            try {
                contentResolver.openInputStream(item.uri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    val make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim()
                    val model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim()
                    makeModel = listOfNotNull(make, model)
                        .filter { it.isNotBlank() }
                        .distinct()
                        .joinToString(" ")
                        .takeIf { it.isNotBlank() }

                    aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { "f/$it" }
                    shutter = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.toDoubleOrNull()?.let { sec ->
                        if (sec > 0 && sec < 1.0) "1/${(1.0 / sec).toInt()}s" else "${sec}s"
                    }
                    iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)?.let { "ISO $it" }
                    focal = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, -1.0)
                        .takeIf { it > 0 }?.let { "${"%.1f".format(it)} mm" }
                }
            } catch (_: Throwable) {
            }
        }

        MediaDetails(
            title = item.displayName,
            path = "${item.relativePath}${item.displayName}".ifBlank { item.bucketName },
            sizeBytes = item.sizeBytes,
            dateTakenMs = item.effectiveDateMs,
            resolution = if (item.width > 0 && item.height > 0) "${item.width} × ${item.height}" else "Unknown",
            mimeType = item.mimeType,
            durationMs = item.durationMs,
            cameraMakeModel = makeModel,
            aperture = aperture,
            shutterSpeed = shutter,
            iso = iso,
            focalLength = focal
        )
    }

    fun createTrashRequest(uris: List<Uri>, trashed: Boolean): PendingIntent? {
        if (uris.isEmpty()) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createTrashRequest(contentResolver, uris, trashed)
        } else {
            null
        }
    }

    fun createDeleteRequest(uris: List<Uri>): PendingIntent? {
        if (uris.isEmpty()) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createDeleteRequest(contentResolver, uris)
        } else {
            null
        }
    }

    fun createWriteRequest(uris: List<Uri>): PendingIntent? {
        if (uris.isEmpty()) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createWriteRequest(contentResolver, uris)
        } else {
            null
        }
    }

    suspend fun deleteDirectLegacy(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        var deleted = 0
        for (uri in uris) {
            try {
                deleted += contentResolver.delete(uri, null, null)
            } catch (_: Throwable) {
            }
        }
        deleted
    }

    suspend fun renameMedia(uri: Uri, newDisplayName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, newDisplayName)
            }
            contentResolver.update(uri, values, null, null) > 0
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun setWallpaper(uri: Uri, flags: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val wm = WallpaperManager.getInstance(context)
            contentResolver.openInputStream(uri)?.use { stream ->
                wm.setStream(stream, null, true, flags)
            }
            true
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun saveEditedBitmapToGallery(
        bitmap: Bitmap,
        originalName: String
    ): Uri? = withContext(Dispatchers.IO) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val base = originalName.substringBeforeLast('.').ifBlank { "Piktures_Edit" }
        val fileName = "${base}_edit_$timestamp.jpg"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Piktures")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext null
            try {
                contentResolver.openOutputStream(uri)?.use { out ->
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)) {
                        throw IllegalStateException("Bitmap compression failed")
                    }
                } ?: throw IllegalStateException("Cannot open output stream")

                val done = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                contentResolver.update(uri, done, null, null)
                uri
            } catch (t: Throwable) {
                contentResolver.delete(uri, null, null)
                null
            }
        } else {
            @Suppress("DEPRECATION")
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "Piktures"
            ).apply { mkdirs() }
            val outFile = File(dir, fileName)
            try {
                FileOutputStream(outFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    @Suppress("DEPRECATION")
                    put(MediaStore.Images.Media.DATA, outFile.absolutePath)
                }
                contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            } catch (_: Throwable) {
                outFile.delete()
                null
            }
        }
    }

    sealed class OverwriteImageResult {
        data object Success : OverwriteImageResult()
        data class NeedsWritePermission(val pendingIntent: PendingIntent) : OverwriteImageResult()
        data object Failure : OverwriteImageResult()
    }

    suspend fun overwriteEditedBitmapToUri(
        targetUri: Uri,
        bitmap: Bitmap
    ): OverwriteImageResult = withContext(Dispatchers.IO) {
        try {
            contentResolver.openOutputStream(targetUri, "wt")?.use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)) {
                    return@withContext OverwriteImageResult.Failure
                }
            } ?: return@withContext OverwriteImageResult.Failure
            OverwriteImageResult.Success
        } catch (se: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pending = runCatching {
                    MediaStore.createWriteRequest(contentResolver, listOf(targetUri))
                }.getOrNull()
                if (pending != null) {
                    return@withContext OverwriteImageResult.NeedsWritePermission(pending)
                }
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && se is android.app.RecoverableSecurityException) {
                return@withContext OverwriteImageResult.NeedsWritePermission(se.userAction.actionIntent)
            }
            OverwriteImageResult.Failure
        } catch (_: Throwable) {
            OverwriteImageResult.Failure
        }
    }

    suspend fun overwriteEditedFileToUri(
        targetUri: Uri,
        sourceFile: File
    ): OverwriteImageResult = withContext(Dispatchers.IO) {
        if (!sourceFile.exists() || sourceFile.length() <= 0L) {
            return@withContext OverwriteImageResult.Failure
        }
        try {
            contentResolver.openOutputStream(targetUri, "wt")?.use { out ->
                FileInputStream(sourceFile).use { input -> input.copyTo(out) }
            } ?: return@withContext OverwriteImageResult.Failure
            OverwriteImageResult.Success
        } catch (se: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pending = runCatching {
                    MediaStore.createWriteRequest(contentResolver, listOf(targetUri))
                }.getOrNull()
                if (pending != null) {
                    return@withContext OverwriteImageResult.NeedsWritePermission(pending)
                }
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && se is android.app.RecoverableSecurityException) {
                return@withContext OverwriteImageResult.NeedsWritePermission(se.userAction.actionIntent)
            }
            OverwriteImageResult.Failure
        } catch (_: Throwable) {
            OverwriteImageResult.Failure
        }
    }

    suspend fun saveVideoFileToGallery(
        sourceFile: File,
        originalName: String
    ): Uri? = withContext(Dispatchers.IO) {
        if (!sourceFile.exists() || sourceFile.length() <= 0L) return@withContext null
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val base = originalName.substringBeforeLast('.').ifBlank { "Piktures_Edit" }
        val fileName = "${base}_edit_$timestamp.mp4"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/Piktures")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val outUri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext null
            try {
                contentResolver.openOutputStream(outUri)?.use { out ->
                    FileInputStream(sourceFile).use { input -> input.copyTo(out) }
                } ?: throw IllegalStateException("Cannot open output stream")

                val done = ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
                contentResolver.update(outUri, done, null, null)
                outUri
            } catch (_: Throwable) {
                contentResolver.delete(outUri, null, null)
                null
            }
        } else {
            @Suppress("DEPRECATION")
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "Piktures"
            ).apply { mkdirs() }
            val outFile = File(dir, fileName)
            try {
                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(outFile).use { out -> input.copyTo(out) }
                }
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    @Suppress("DEPRECATION")
                    put(MediaStore.Video.Media.DATA, outFile.absolutePath)
                }
                contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            } catch (_: Throwable) {
                outFile.delete()
                null
            }
        }
    }

    suspend fun saveTrimmedVideoToGallery(
        sourceUri: Uri,
        originalName: String,
        startMs: Long,
        endMs: Long,
        keepAudio: Boolean = true
    ): Uri? = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "trim_${System.currentTimeMillis()}.mp4")
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        var muxerStarted = false

        try {
            val rotationDegrees = run {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, sourceUri)
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                        ?.toIntOrNull() ?: 0
                } catch (_: Throwable) {
                    0
                } finally {
                    runCatching { retriever.release() }
                }
            }

            extractor.setDataSource(context, sourceUri, null)
            var videoTrackIndex = -1
            var audioTrackIndex = -1
            var maxBufferSize = 1024 * 1024

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("video/") && videoTrackIndex == -1) {
                    videoTrackIndex = i
                    if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                        maxBufferSize = maxOf(maxBufferSize, format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE))
                    }
                } else if (keepAudio && mime.startsWith("audio/") && audioTrackIndex == -1) {
                    audioTrackIndex = i
                    if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                        maxBufferSize = maxOf(maxBufferSize, format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE))
                    }
                }
            }

            if (videoTrackIndex == -1) return@withContext null

            val localMuxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer = localMuxer
            if (rotationDegrees != 0) {
                localMuxer.setOrientationHint(rotationDegrees)
            }

            val muxerVideoTrack = localMuxer.addTrack(extractor.getTrackFormat(videoTrackIndex))
            val muxerAudioTrack = if (audioTrackIndex != -1) {
                runCatching { localMuxer.addTrack(extractor.getTrackFormat(audioTrackIndex)) }.getOrDefault(-1)
            } else -1

            localMuxer.start()
            muxerStarted = true

            val buffer = ByteBuffer.allocate(maxBufferSize.coerceIn(256 * 1024, 4 * 1024 * 1024))
            val bufferInfo = MediaCodec.BufferInfo()
            val startUs = startMs.coerceAtLeast(0L) * 1000L
            val endUs = endMs.coerceAtLeast(startMs + 250L) * 1000L

            var firstVideoPtsUs = -1L
            extractor.selectTrack(videoTrackIndex)
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            while (true) {
                buffer.clear()
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break
                val sampleTime = extractor.sampleTime
                if (sampleTime > endUs) break

                if (firstVideoPtsUs < 0L) {
                    firstVideoPtsUs = sampleTime
                }
                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = (sampleTime - firstVideoPtsUs).coerceAtLeast(0L)
                bufferInfo.flags = extractor.sampleFlags
                localMuxer.writeSampleData(muxerVideoTrack, buffer, bufferInfo)
                extractor.advance()
            }
            extractor.unselectTrack(videoTrackIndex)

            if (firstVideoPtsUs < 0L) return@withContext null

            if (muxerAudioTrack != -1 && audioTrackIndex != -1) {
                extractor.selectTrack(audioTrackIndex)
                extractor.seekTo(firstVideoPtsUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                while (true) {
                    buffer.clear()
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break
                    val sampleTime = extractor.sampleTime
                    if (sampleTime > endUs) break
                    if (sampleTime >= firstVideoPtsUs) {
                        bufferInfo.offset = 0
                        bufferInfo.size = sampleSize
                        bufferInfo.presentationTimeUs = (sampleTime - firstVideoPtsUs).coerceAtLeast(0L)
                        bufferInfo.flags = extractor.sampleFlags
                        localMuxer.writeSampleData(muxerAudioTrack, buffer, bufferInfo)
                    }
                    extractor.advance()
                }
                extractor.unselectTrack(audioTrackIndex)
            }

            localMuxer.stop()
            muxerStarted = false
            localMuxer.release()
            muxer = null

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val base = originalName.substringBeforeLast('.').ifBlank { "Piktures_Trim" }
            val fileName = "${base}_trim_$timestamp.mp4"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/Piktures")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
                val outUri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext null
                try {
                    contentResolver.openOutputStream(outUri)?.use { out ->
                        FileInputStream(tempFile).use { input -> input.copyTo(out) }
                    } ?: throw IllegalStateException("Cannot open output stream")

                    val done = ContentValues().apply {
                        put(MediaStore.Video.Media.IS_PENDING, 0)
                    }
                    contentResolver.update(outUri, done, null, null)
                    outUri
                } catch (_: Throwable) {
                    contentResolver.delete(outUri, null, null)
                    null
                }
            } else {
                @Suppress("DEPRECATION")
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    "Piktures"
                ).apply { mkdirs() }
                val outFile = File(dir, fileName)
                try {
                    FileInputStream(tempFile).use { input ->
                        FileOutputStream(outFile).use { out -> input.copyTo(out) }
                    }
                    val values = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                        @Suppress("DEPRECATION")
                        put(MediaStore.Video.Media.DATA, outFile.absolutePath)
                    }
                    contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                } catch (_: Throwable) {
                    outFile.delete()
                    null
                }
            }
        } catch (_: Throwable) {
            null
        } finally {
            if (muxerStarted) {
                runCatching { muxer?.stop() }
            }
            runCatching { muxer?.release() }
            runCatching { extractor.release() }
            runCatching { tempFile.delete() }
        }
    }

    fun uriExists(uriString: String): Boolean {
        return try {
            val uri = Uri.parse(uriString)
            contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
        } catch (_: Throwable) {
            false
        }
    }
}
