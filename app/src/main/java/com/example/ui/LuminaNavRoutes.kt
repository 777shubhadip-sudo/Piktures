package com.example.ui

import kotlinx.serialization.Serializable

@Serializable
data object GalleryRoute

@Serializable
data class AlbumDetailRoute(val bucketId: Long, val albumName: String)

@Serializable
data object SearchRoute

@Serializable
data object TrashRoute

@Serializable
data object SettingsRoute

@Serializable
data object CloudSyncRoute

@Serializable
data object VaultLevel1Route

@Serializable
data object VaultPasscodeRoute

@Serializable
data object VaultLevel2Route

@Serializable
data class MediaViewerRoute(
    val source: String,
    val initialIndex: Int,
    val bucketId: Long = 0L
)

@Serializable
data class PhotoEditorRoute(
    val uriString: String,
    val displayName: String
)

@Serializable
data class VideoTrimmerRoute(
    val uriString: String,
    val displayName: String,
    val durationMs: Long = 0L
)
