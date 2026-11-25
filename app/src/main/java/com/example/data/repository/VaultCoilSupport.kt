package com.example.data.repository

import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.key.Keyer
import coil.request.Options
import com.example.security.VaultCryptoManager
import okio.Buffer
import java.io.File

data class VaultImageRequest(
    val file: File,
    val mimeType: String,
    val isThumbnail: Boolean
)

class VaultEncryptedKeyer : Keyer<VaultImageRequest> {
    override fun key(data: VaultImageRequest, options: Options): String {
        return "vault_${data.file.absolutePath}_${data.isThumbnail}_${data.file.lastModified()}"
    }
}

class VaultEncryptedFetcher(
    private val data: VaultImageRequest,
    private val options: Options,
    private val cryptoManager: VaultCryptoManager
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        if (!data.file.exists()) return null
        val plainBytes = cryptoManager.decryptBytes(data.file)
        val buffer = Buffer().apply { write(plainBytes) }
        plainBytes.fill(0)
        val mime = if (data.isThumbnail) "image/jpeg" else data.mimeType
        return SourceResult(
            source = ImageSource(source = buffer, context = options.context),
            mimeType = mime,
            dataSource = DataSource.MEMORY
        )
    }

    class Factory(
        private val cryptoManager: VaultCryptoManager
    ) : Fetcher.Factory<VaultImageRequest> {
        override fun create(
            data: VaultImageRequest,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher = VaultEncryptedFetcher(data, options, cryptoManager)
    }
}
