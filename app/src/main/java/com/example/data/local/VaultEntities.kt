package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.MediaType
import com.example.domain.model.VaultMedia

enum class MoveState {
    COPYING,
    VERIFIED,
    COMMITTED
}

@Entity(tableName = "vault_media")
data class VaultMediaEntity(
    @PrimaryKey val id: String,
    val level: Int,
    val encryptedFileName: String,
    val encryptedThumbName: String,
    val originalName: String,
    val originalRelativePath: String,
    val originalUriString: String,
    val mimeType: String,
    val mediaType: String,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val durationMs: Long,
    val dateTakenMs: Long,
    val addedToVaultMs: Long,
    val sha256Checksum: String,
    val moveState: String
) {
    fun toDomain(): VaultMedia = VaultMedia(
        id = id,
        level = level,
        encryptedFileName = encryptedFileName,
        encryptedThumbName = encryptedThumbName,
        originalName = originalName,
        originalRelativePath = originalRelativePath,
        mimeType = mimeType,
        mediaType = if (mediaType == MediaType.VIDEO.name) MediaType.VIDEO else MediaType.IMAGE,
        sizeBytes = sizeBytes,
        width = width,
        height = height,
        durationMs = durationMs,
        dateTakenMs = dateTakenMs,
        addedToVaultMs = addedToVaultMs
    )
}

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val mediaStoreId: Long,
    val addedAtMs: Long = System.currentTimeMillis()
)
