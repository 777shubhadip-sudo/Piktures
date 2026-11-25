package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_media WHERE level = :level AND moveState = 'COMMITTED' ORDER BY addedToVaultMs DESC")
    fun observeVaultMediaByLevel(level: Int): Flow<List<VaultMediaEntity>>

    @Query("SELECT * FROM vault_media WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): VaultMediaEntity?

    @Query("SELECT * FROM vault_media WHERE moveState != 'COMMITTED'")
    suspend fun getUncommittedMoves(): List<VaultMediaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultMedia(entity: VaultMediaEntity)

    @Query("UPDATE vault_media SET moveState = :moveState WHERE id = :id")
    suspend fun updateMoveState(id: String, moveState: String)

    @Query("UPDATE vault_media SET level = :newLevel WHERE id = :id")
    suspend fun updateLevel(id: String, newLevel: Int)

    @Query("DELETE FROM vault_media WHERE id = :id")
    suspend fun deleteVaultMediaById(id: String)

    @Query("SELECT mediaStoreId FROM favorites")
    fun observeFavoriteIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE mediaStoreId = :mediaStoreId")
    suspend fun removeFavorite(mediaStoreId: Long)
}
