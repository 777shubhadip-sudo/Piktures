package com.example.domain.model

import android.net.Uri

enum class MediaType {
    IMAGE,
    VIDEO
}

data class GalleryMedia(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val relativePath: String,
    val bucketId: Long,
    val bucketName: String,
    val mimeType: String,
    val mediaType: MediaType,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val durationMs: Long,
    val dateAddedSec: Long,
    val dateTakenMs: Long,
    val isFavorite: Boolean = false,
    val isTrashed: Boolean = false,
    val dateExpiresSec: Long? = null
) {
    val effectiveDateMs: Long
        get() = if (dateTakenMs > 0L) dateTakenMs else dateAddedSec * 1000L
}

data class AlbumFolder(
    val bucketId: Long,
    val name: String,
    val coverUri: Uri,
    val itemCount: Int,
    val imageCount: Int,
    val videoCount: Int,
    val latestDateMs: Long,
    val previewUris: List<Uri> = emptyList()
)

data class VaultMedia(
    val id: String,
    val level: Int,
    val encryptedFileName: String,
    val encryptedThumbName: String,
    val originalName: String,
    val originalRelativePath: String,
    val mimeType: String,
    val mediaType: MediaType,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val durationMs: Long,
    val dateTakenMs: Long,
    val addedToVaultMs: Long
)

data class MediaDetails(
    val title: String,
    val path: String,
    val sizeBytes: Long,
    val dateTakenMs: Long,
    val resolution: String,
    val mimeType: String,
    val durationMs: Long,
    val cameraMakeModel: String? = null,
    val aperture: String? = null,
    val shutterSpeed: String? = null,
    val iso: String? = null,
    val focalLength: String? = null
)

sealed interface MediaItemRef {
    val key: String
    val displayName: String
    val mediaType: MediaType
    val durationMs: Long
    val width: Int
    val height: Int
    val sizeBytes: Long
    val dateMs: Long

    data class Gallery(val item: GalleryMedia) : MediaItemRef {
        override val key: String get() = "g_${item.id}"
        override val displayName: String get() = item.displayName
        override val mediaType: MediaType get() = item.mediaType
        override val durationMs: Long get() = item.durationMs
        override val width: Int get() = item.width
        override val height: Int get() = item.height
        override val sizeBytes: Long get() = item.sizeBytes
        override val dateMs: Long get() = item.effectiveDateMs
    }

    data class Vault(val item: VaultMedia) : MediaItemRef {
        override val key: String get() = "v_${item.id}"
        override val displayName: String get() = item.originalName
        override val mediaType: MediaType get() = item.mediaType
        override val durationMs: Long get() = item.durationMs
        override val width: Int get() = item.width
        override val height: Int get() = item.height
        override val sizeBytes: Long get() = item.sizeBytes
        override val dateMs: Long get() = item.dateTakenMs
    }
}
