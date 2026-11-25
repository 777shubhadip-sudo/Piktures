package com.example.security

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

sealed interface AuthResult {
    data object Success : AuthResult
    data object Cancelled : AuthResult
    data class Error(val message: String) : AuthResult
    data class NeedLegacyCredentialIntent(val intent: Intent) : AuthResult
}

object BiometricAuthHelper {

    fun isDeviceSecured(context: Context): Boolean {
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return keyguard?.isDeviceSecure == true
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onResult: (AuthResult) -> Unit
    ) {
        val biometricManager = BiometricManager.from(activity)
        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        } else {
            BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        }

        val canAuth = biometricManager.canAuthenticate(authenticators)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            val keyguard = activity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            if (keyguard?.isDeviceSecure == true) {
                @Suppress("DEPRECATION")
                val intent = keyguard.createConfirmDeviceCredentialIntent(title, subtitle)
                if (intent != null) {
                    onResult(AuthResult.NeedLegacyCredentialIntent(intent))
                    return
                }
            }
            val reason = when (canAuth) {
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                    "Set up a screen lock PIN, pattern, or fingerprint in device Settings to unlock."
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                    "Device lock authentication is unavailable on this device."
                else -> "Please configure a screen lock in device Settings."
            }
            onResult(AuthResult.Error(reason))
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onResult(AuthResult.Success)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                    ) {
                        onResult(AuthResult.Cancelled)
                    } else {
                        onResult(AuthResult.Error(errString.toString()))
                    }
                }

                override fun onAuthenticationFailed() {
                    // BiometricPrompt handles retry UI internally until error/cancel
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)
            .build()

        prompt.authenticate(promptInfo)
    }
}
