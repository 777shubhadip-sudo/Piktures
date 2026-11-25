package com.example.domain.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserCloudProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String? = null,
    val email: String? = null,
    val themeMode: String = ThemeMode.DARK.name,
    val gridColumns: Long = 4L,
    val relockTimeout: String = RelockTimeout.IMMEDIATE.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SyncedMediaItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mediaKey: String = "",
    val displayName: String = "",
    val bucketName: String = "",
    val mediaType: String = MediaType.IMAGE.name,
    val favorite: Boolean = false,
    val note: String? = null,
    val sizeBytes: Long = 0L,
    val dateTakenMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

sealed interface CloudUiState<out T> {
    data object Loading : CloudUiState<Nothing>
    data class Success<T>(val data: T) : CloudUiState<T>
    data class Error(val message: String) : CloudUiState<Nothing>
}
