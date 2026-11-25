package com.example.di

import android.content.Context
import androidx.room.Room
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.data.local.LuminaDatabase
import com.example.data.local.VaultDao
import com.example.data.repository.MediaStoreThumbnailFetcher
import com.example.data.repository.VaultEncryptedFetcher
import com.example.data.repository.VaultEncryptedKeyer
import com.example.security.VaultCryptoManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LuminaDatabase {
        return Room.databaseBuilder(
            context,
            LuminaDatabase::class.java,
            "lumina_vault.db"
        ).build()
    }

    @Provides
    fun provideVaultDao(database: LuminaDatabase): VaultDao = database.vaultDao()

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        cryptoManager: VaultCryptoManager
    ): ImageLoader {
        val decodeThreads = minOf(8, maxOf(4, Runtime.getRuntime().availableProcessors()))
        return ImageLoader.Builder(context)
            .components {
                add(MediaStoreThumbnailFetcher.Factory())
                add(VideoFrameDecoder.Factory())
                add(VaultEncryptedKeyer())
                add(VaultEncryptedFetcher.Factory(cryptoManager))
            }
            .bitmapFactoryMaxParallelism(decodeThreads)
            .allowRgb565(true)
            .respectCacheHeaders(false)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.22)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("coil_gallery_cache"))
                    .maxSizeBytes(200L * 1024L * 1024L)
                    .build()
            }
            .crossfade(140)
            .build()
    }
}
