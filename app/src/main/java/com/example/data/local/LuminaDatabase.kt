package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [VaultMediaEntity::class, FavoriteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LuminaDatabase : RoomDatabase() {
    abstract fun vaultDao(): VaultDao
}
