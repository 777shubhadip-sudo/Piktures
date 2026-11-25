package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.security.PassVerifyResult
import com.example.security.PasscodeManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PasscodeManagerTest {

    private lateinit var passcodeManager: PasscodeManager

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("passcode_manager_unit_test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        passcodeManager = PasscodeManager { prefs }
    }

    @Test
    fun setAndVerifyPasscode_succeedsOnCorrectPinAndClearsCleanly() {
        assertFalse(passcodeManager.hasExtraPass())

        passcodeManager.setExtraPass("2468")
        assertTrue(passcodeManager.hasExtraPass())

        assertEquals(PassVerifyResult.Success, passcodeManager.verifyExtraPass("2468", nowMs = 1_000L))

        passcodeManager.clearExtraPass()
        assertFalse(passcodeManager.hasExtraPass())
    }

    @Test
    fun verifyExtraPass_locksOutAfterFiveFailedAttemptsAndEscalatesTiers() {
        passcodeManager.setExtraPass("1357")

        for (attempt in 1..4) {
            val res = passcodeManager.verifyExtraPass("0000", nowMs = 10_000L + attempt)
            assertEquals(PassVerifyResult.Failed(5 - attempt), res)
        }

        // 5th wrong attempt triggers 30s lockout (tier 0)
        val firstLock = passcodeManager.verifyExtraPass("0000", nowMs = 20_000L)
        assertEquals(PassVerifyResult.LockedOut(30L), firstLock)
        assertTrue(passcodeManager.getLockoutRemainingSeconds(25_000L) > 0L)

        // Still locked out before 30s passes
        val duringLock = passcodeManager.verifyExtraPass("1357", nowMs = 40_000L)
        assertTrue(duringLock is PassVerifyResult.LockedOut)

        // Unlock after lockout window expires, then fail 5 more times -> 60s tier 1
        for (attempt in 1..4) {
            val res = passcodeManager.verifyExtraPass("9999", nowMs = 60_000L + attempt)
            assertEquals(PassVerifyResult.Failed(5 - attempt), res)
        }
        val secondLock = passcodeManager.verifyExtraPass("9999", nowMs = 60_100L)
        assertEquals(PassVerifyResult.LockedOut(60L), secondLock)

        // After 60s expires, valid passcode succeeds and resets lockout state
        assertEquals(PassVerifyResult.Success, passcodeManager.verifyExtraPass("1357", nowMs = 125_000L))
        assertEquals(0L, passcodeManager.getLockoutRemainingSeconds(125_000L))
    }
}
