package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

data class EncryptedFileResult(
    val plaintextSize: Long,
    val sha256Hex: String
)

@Singleton
class VaultCryptoManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "lumina_vault_master_kek"
        private const val PREFS_NAME = "lumina_vault_secrets"
        private const val KEY_WRAPPED_DEK = "wrapped_vault_dek_v1"
        private const val KEY_WRAPPED_DEK_IV = "wrapped_vault_dek_iv_v1"

        private val MAGIC = byteArrayOf('L'.code.toByte(), 'V'.code.toByte(), 'T'.code.toByte(), '1'.code.toByte())
        private const val HEADER_SIZE = 16
        private const val DEFAULT_CHUNK_SIZE = 524_288 // 512 KB
        private const val IV_SIZE = 12
        private const val TAG_BITS = 128
        private const val TAG_BYTES = 16
    }

    private val secureRandom = SecureRandom()

    val encryptedPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    @Volatile
    private var cachedDek: SecretKey? = null

    @Synchronized
    private fun getOrCreateDataKey(): SecretKey {
        cachedDek?.let { return it }

        val masterKey = getOrCreateMasterKey()
        val wrappedB64 = encryptedPrefs.getString(KEY_WRAPPED_DEK, null)
        val ivB64 = encryptedPrefs.getString(KEY_WRAPPED_DEK_IV, null)

        if (wrappedB64 != null && ivB64 != null) {
            val wrapped = Base64.decode(wrappedB64, Base64.NO_WRAP)
            val iv = Base64.decode(ivB64, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, masterKey, GCMParameterSpec(TAG_BITS, iv))
            val rawDek = cipher.doFinal(wrapped)
            val key = SecretKeySpec(rawDek, "AES")
            cachedDek = key
            return key
        }

        val rawDek = ByteArray(32).apply { secureRandom.nextBytes(this) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, masterKey)
        val wrapped = cipher.doFinal(rawDek)
        val iv = cipher.iv

        encryptedPrefs.edit()
            .putString(KEY_WRAPPED_DEK, Base64.encodeToString(wrapped, Base64.NO_WRAP))
            .putString(KEY_WRAPPED_DEK_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .apply()

        val key = SecretKeySpec(rawDek, "AES")
        rawDek.fill(0)
        cachedDek = key
        return key
    }

    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        if (keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            val entry = keyStore.getEntry(MASTER_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) return entry.secretKey
        }
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun encryptStreamToFile(inputStream: InputStream, destFile: File): EncryptedFileResult {
        destFile.parentFile?.mkdirs()
        val tempFile = File(destFile.parentFile, "${destFile.name}.tmp")
        val dek = getOrCreateDataKey()
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_CHUNK_SIZE)
        var totalPlaintext = 0L
        var chunkIndex = 0L

        try {
            RandomAccessFile(tempFile, "rw").use { raf ->
                raf.setLength(0)
                // Reserve 16-byte header
                raf.write(ByteArray(HEADER_SIZE))

                while (true) {
                    val read = readFullChunk(inputStream, buffer)
                    if (read <= 0) break

                    digest.update(buffer, 0, read)
                    totalPlaintext += read

                    val iv = ByteArray(IV_SIZE).apply { secureRandom.nextBytes(this) }
                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    cipher.init(Cipher.ENCRYPT_MODE, dek, GCMParameterSpec(TAG_BITS, iv))
                    cipher.updateAAD(longToBytes(chunkIndex))
                    val cipherBytes = cipher.doFinal(buffer, 0, read)

                    raf.write(iv)
                    raf.write(cipherBytes)
                    chunkIndex++
                }

                // Write final header with actual plaintext size
                raf.seek(0)
                val header = ByteBuffer.allocate(HEADER_SIZE)
                    .put(MAGIC)
                    .putInt(DEFAULT_CHUNK_SIZE)
                    .putLong(totalPlaintext)
                    .array()
                raf.write(header)
                raf.fd.sync()
            }

            if (destFile.exists()) destFile.delete()
            if (!tempFile.renameTo(destFile)) {
                tempFile.copyTo(destFile, overwrite = true)
                tempFile.delete()
            }
        } catch (t: Throwable) {
            tempFile.delete()
            throw t
        } finally {
            buffer.fill(0)
        }

        val shaHex = digest.digest().joinToString("") { "%02x".format(it) }
        return EncryptedFileResult(plaintextSize = totalPlaintext, sha256Hex = shaHex)
    }

    fun encryptBytesToFile(plainBytes: ByteArray, destFile: File): EncryptedFileResult {
        return plainBytes.inputStream().use { encryptStreamToFile(it, destFile) }
    }

    fun verifyEncryptedFile(
        file: File,
        expectedPlaintextSize: Long,
        expectedSha256Hex: String
    ): Boolean {
        if (!file.exists() || file.length() < HEADER_SIZE) return false
        return try {
            val dek = getOrCreateDataKey()
            val digest = MessageDigest.getInstance("SHA-256")
            var verifiedSize = 0L

            RandomAccessFile(file, "r").use { raf ->
                val header = readHeader(raf) ?: return false
                if (header.plaintextSize != expectedPlaintextSize) return false
                if (header.plaintextSize == 0L) return true

                val totalChunks = ((header.plaintextSize + header.chunkSize - 1) / header.chunkSize).toInt()
                for (i in 0 until totalChunks) {
                    val plainChunkLen = if (i == totalChunks - 1) {
                        val rem = (header.plaintextSize % header.chunkSize).toInt()
                        if (rem == 0) header.chunkSize else rem
                    } else {
                        header.chunkSize
                    }

                    val iv = ByteArray(IV_SIZE)
                    raf.readFully(iv)
                    val cipherLen = plainChunkLen + TAG_BYTES
                    val cipherBuf = ByteArray(cipherLen)
                    raf.readFully(cipherBuf)

                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    cipher.init(Cipher.DECRYPT_MODE, dek, GCMParameterSpec(TAG_BITS, iv))
                    cipher.updateAAD(longToBytes(i.toLong()))
                    val plain = cipher.doFinal(cipherBuf)
                    digest.update(plain)
                    verifiedSize += plain.size
                    plain.fill(0)
                }
            }

            val actualSha = digest.digest().joinToString("") { "%02x".format(it) }
            verifiedSize == expectedPlaintextSize && actualSha.equals(expectedSha256Hex, ignoreCase = true)
        } catch (_: Throwable) {
            false
        }
    }

    fun decryptBytes(file: File): ByteArray {
        val out = ByteArrayOutputStream()
        decryptToStream(file, out)
        return out.toByteArray()
    }

    fun decryptToStream(file: File, outputStream: OutputStream): Long {
        val dek = getOrCreateDataKey()
        var totalWritten = 0L
        RandomAccessFile(file, "r").use { raf ->
            val header = readHeader(raf) ?: throw IllegalStateException("Invalid encrypted vault header")
            if (header.plaintextSize == 0L) return 0L

            val totalChunks = ((header.plaintextSize + header.chunkSize - 1) / header.chunkSize).toInt()
            for (i in 0 until totalChunks) {
                val plainChunkLen = if (i == totalChunks - 1) {
                    val rem = (header.plaintextSize % header.chunkSize).toInt()
                    if (rem == 0) header.chunkSize else rem
                } else {
                    header.chunkSize
                }

                val iv = ByteArray(IV_SIZE)
                raf.readFully(iv)
                val cipherBuf = ByteArray(plainChunkLen + TAG_BYTES)
                raf.readFully(cipherBuf)

                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, dek, GCMParameterSpec(TAG_BITS, iv))
                cipher.updateAAD(longToBytes(i.toLong()))
                val plain = cipher.doFinal(cipherBuf)
                outputStream.write(plain)
                totalWritten += plain.size
                plain.fill(0)
            }
        }
        return totalWritten
    }

    fun getPlaintextSize(file: File): Long {
        if (!file.exists() || file.length() < HEADER_SIZE) return 0L
        RandomAccessFile(file, "r").use { raf ->
            return readHeader(raf)?.plaintextSize ?: 0L
        }
    }

    fun readDecryptedRange(
        file: File,
        offset: Long,
        buffer: ByteArray,
        bufferOffset: Int,
        length: Int
    ): Int {
        if (length <= 0) return 0
        val dek = getOrCreateDataKey()
        RandomAccessFile(file, "r").use { raf ->
            val header = readHeader(raf) ?: return -1
            if (offset >= header.plaintextSize) return -1

            val chunkIdx = (offset / header.chunkSize).toInt()
            val offsetInChunk = (offset % header.chunkSize).toInt()
            val totalChunks = ((header.plaintextSize + header.chunkSize - 1) / header.chunkSize).toInt()
            if (chunkIdx >= totalChunks) return -1

            val plainChunkLen = if (chunkIdx == totalChunks - 1) {
                val rem = (header.plaintextSize % header.chunkSize).toInt()
                if (rem == 0) header.chunkSize else rem
            } else {
                header.chunkSize
            }

            val encryptedRecordSize = IV_SIZE.toLong() + header.chunkSize.toLong() + TAG_BYTES.toLong()
            val recordStart = HEADER_SIZE.toLong() + chunkIdx.toLong() * encryptedRecordSize
            raf.seek(recordStart)

            val iv = ByteArray(IV_SIZE)
            raf.readFully(iv)
            val cipherBuf = ByteArray(plainChunkLen + TAG_BYTES)
            raf.readFully(cipherBuf)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, dek, GCMParameterSpec(TAG_BITS, iv))
            cipher.updateAAD(longToBytes(chunkIdx.toLong()))
            val plain = cipher.doFinal(cipherBuf)

            val available = plain.size - offsetInChunk
            val toCopy = minOf(length, available)
            System.arraycopy(plain, offsetInChunk, buffer, bufferOffset, toCopy)
            plain.fill(0)
            return toCopy
        }
    }

    fun secureWipeAndDelete(file: File): Boolean {
        if (!file.exists()) return true
        return try {
            val len = file.length()
            if (len > 0) {
                RandomAccessFile(file, "rw").use { raf ->
                    val zeros = ByteArray(minOf(65_536L, len).toInt())
                    var remaining = len
                    raf.seek(0)
                    while (remaining > 0) {
                        val writeNow = minOf(zeros.size.toLong(), remaining).toInt()
                        raf.write(zeros, 0, writeNow)
                        remaining -= writeNow
                    }
                    raf.fd.sync()
                }
            }
            file.delete()
        } catch (_: Throwable) {
            file.delete()
        }
    }

    private data class VaultHeader(val chunkSize: Int, val plaintextSize: Long)

    private fun readHeader(raf: RandomAccessFile): VaultHeader? {
        if (raf.length() < HEADER_SIZE) return null
        raf.seek(0)
        val headerBytes = ByteArray(HEADER_SIZE)
        raf.readFully(headerBytes)
        for (i in MAGIC.indices) {
            if (headerBytes[i] != MAGIC[i]) return null
        }
        val bb = ByteBuffer.wrap(headerBytes, 4, 12)
        val chunkSize = bb.int
        val plaintextSize = bb.long
        if (chunkSize <= 0 || plaintextSize < 0L) return null
        return VaultHeader(chunkSize, plaintextSize)
    }

    private fun readFullChunk(input: InputStream, buffer: ByteArray): Int {
        var totalRead = 0
        while (totalRead < buffer.size) {
            val count = input.read(buffer, totalRead, buffer.size - totalRead)
            if (count == -1) break
            totalRead += count
        }
        return totalRead
    }

    private fun longToBytes(value: Long): ByteArray =
        ByteBuffer.allocate(8).putLong(value).array()
}
