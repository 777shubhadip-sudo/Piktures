package com.example.security

import android.content.SharedPreferences
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

sealed interface PassVerifyResult {
    data object Success : PassVerifyResult
    data class Failed(val attemptsRemainingBeforeLockout: Int) : PassVerifyResult
    data class LockedOut(val remainingSeconds: Long) : PassVerifyResult
}

@Singleton
class PasscodeManager internal constructor(
    private val prefsProvider: () -> SharedPreferences
) {
    @Inject
    constructor(cryptoManager: VaultCryptoManager) : this({ cryptoManager.encryptedPrefs })

    companion object {
        private const val KEY_HASH = "extra_pass_hash_v1"
        private const val KEY_SALT = "extra_pass_salt_v1"
        private const val KEY_FAILED_COUNT = "extra_pass_failed_count"
        private const val KEY_LOCKOUT_UNTIL_MS = "extra_pass_lockout_until_ms"
        private const val KEY_LOCKOUT_TIER = "extra_pass_lockout_tier"

        private const val ITERATIONS = 120_000
        private const val KEY_LENGTH_BITS = 256
        private const val MAX_ATTEMPTS_PER_TIER = 5
    }

    private val prefs: SharedPreferences get() = prefsProvider()
    private val secureRandom = SecureRandom()

    fun hasExtraPass(): Boolean {
        val hash = prefs.getString(KEY_HASH, null)
        val salt = prefs.getString(KEY_SALT, null)
        return !hash.isNullOrEmpty() && !salt.isNullOrEmpty()
    }

    fun setExtraPass(passcode: String) {
        val salt = ByteArray(16).apply { secureRandom.nextBytes(this) }
        val derived = deriveKey(passcode, salt)
        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, Base64.encodeToString(derived, Base64.NO_WRAP))
            .putInt(KEY_FAILED_COUNT, 0)
            .putLong(KEY_LOCKOUT_UNTIL_MS, 0L)
            .putInt(KEY_LOCKOUT_TIER, 0)
            .apply()
        derived.fill(0)
    }

    fun clearExtraPass() {
        prefs.edit()
            .remove(KEY_HASH)
            .remove(KEY_SALT)
            .remove(KEY_FAILED_COUNT)
            .remove(KEY_LOCKOUT_UNTIL_MS)
            .remove(KEY_LOCKOUT_TIER)
            .apply()
    }

    fun getLockoutRemainingSeconds(nowMs: Long = System.currentTimeMillis()): Long {
        val until = prefs.getLong(KEY_LOCKOUT_UNTIL_MS, 0L)
        if (until <= nowMs) return 0L
        return ((until - nowMs) + 999L) / 1000L
    }

    fun verifyExtraPass(passcode: String, nowMs: Long = System.currentTimeMillis()): PassVerifyResult {
        val lockoutRem = getLockoutRemainingSeconds(nowMs)
        if (lockoutRem > 0L) {
            return PassVerifyResult.LockedOut(lockoutRem)
        }

        val storedHashB64 = prefs.getString(KEY_HASH, null) ?: return PassVerifyResult.Failed(MAX_ATTEMPTS_PER_TIER)
        val storedSaltB64 = prefs.getString(KEY_SALT, null) ?: return PassVerifyResult.Failed(MAX_ATTEMPTS_PER_TIER)

        val storedHash = Base64.decode(storedHashB64, Base64.NO_WRAP)
        val salt = Base64.decode(storedSaltB64, Base64.NO_WRAP)
        val candidateHash = deriveKey(passcode, salt)

        val matches = MessageDigest.isEqual(storedHash, candidateHash)
        candidateHash.fill(0)

        if (matches) {
            prefs.edit()
                .putInt(KEY_FAILED_COUNT, 0)
                .putLong(KEY_LOCKOUT_UNTIL_MS, 0L)
                .putInt(KEY_LOCKOUT_TIER, 0)
                .apply()
            return PassVerifyResult.Success
        }

        val failedCount = prefs.getInt(KEY_FAILED_COUNT, 0) + 1
        return if (failedCount >= MAX_ATTEMPTS_PER_TIER) {
            val tier = prefs.getInt(KEY_LOCKOUT_TIER, 0)
            val lockoutSec = when (tier) {
                0 -> 30L
                1 -> 60L
                2 -> 120L
                else -> 300L
            }
            val lockoutUntil = nowMs + lockoutSec * 1000L
            prefs.edit()
                .putInt(KEY_FAILED_COUNT, 0)
                .putLong(KEY_LOCKOUT_UNTIL_MS, lockoutUntil)
                .putInt(KEY_LOCKOUT_TIER, tier + 1)
                .apply()
            PassVerifyResult.LockedOut(lockoutSec)
        } else {
            prefs.edit()
                .putInt(KEY_FAILED_COUNT, failedCount)
                .apply()
            PassVerifyResult.Failed(MAX_ATTEMPTS_PER_TIER - failedCount)
        }
    }

    private fun deriveKey(passcode: String, salt: ByteArray): ByteArray {
        val chars = passcode.toCharArray()
        return try {
            val spec = PBEKeySpec(chars, salt, ITERATIONS, KEY_LENGTH_BITS)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            factory.generateSecret(spec).encoded
        } finally {
            chars.fill('\u0000')
        }
    }
}
