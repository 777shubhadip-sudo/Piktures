package com.example.data.repository

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import com.example.security.VaultCryptoManager
import java.io.File

@OptIn(UnstableApi::class)
class VaultMediaDataSource(
    private val encryptedFile: File,
    private val cryptoManager: VaultCryptoManager
) : BaseDataSource(false) {

    private var dataUri: Uri? = null
    private var readPosition: Long = 0L
    private var bytesRemaining: Long = 0L
    private var opened = false

    override fun open(dataSpec: DataSpec): Long {
        dataUri = dataSpec.uri
        transferInitializing(dataSpec)

        val totalSize = cryptoManager.getPlaintextSize(encryptedFile)
        if (dataSpec.position > totalSize) {
            throw IllegalStateException("Seek position out of bounds")
        }

        readPosition = dataSpec.position
        bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else {
            totalSize - dataSpec.position
        }

        opened = true
        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT

        val toRead = minOf(length.toLong(), bytesRemaining).toInt()
        val bytesRead = cryptoManager.readDecryptedRange(
            file = encryptedFile,
            offset = readPosition,
            buffer = buffer,
            bufferOffset = offset,
            length = toRead
        )

        if (bytesRead <= 0) {
            return C.RESULT_END_OF_INPUT
        }

        readPosition += bytesRead
        bytesRemaining -= bytesRead
        bytesTransferred(bytesRead)
        return bytesRead
    }

    override fun getUri(): Uri? = dataUri

    override fun close() {
        dataUri = null
        if (opened) {
            opened = false
            transferEnded()
        }
    }

    class Factory(
        private val encryptedFile: File,
        private val cryptoManager: VaultCryptoManager
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource =
            VaultMediaDataSource(encryptedFile, cryptoManager)
    }
}
