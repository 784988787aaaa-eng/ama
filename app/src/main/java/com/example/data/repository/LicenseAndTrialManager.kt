package com.example.data.repository

import android.content.Context
import com.example.domain.AppSecurityManager
import com.example.domain.GoogleAuthSessionManager
import com.example.domain.HashUtils
import java.security.MessageDigest
import java.util.UUID

/**
 * LicenseAndTrialManager - Unified Domain Licensing Authority & Single Source of Truth
 *
 * Consolidates all trial quota management, cryptographic activation code verification,
 * device fingerprinting, and hardware-secured license caching.
 */
class LicenseAndTrialManager(context: Context) {

    private val appContext: Context = context.applicationContext
    private val securityManager: AppSecurityManager = AppSecurityManager.getInstance(appContext)

    companion object {
        const val PREFIX_TEMP = "ACT-T-"
        const val PREFIX_PERM = "ACT-P-"
        const val SECURE_LIMIT_VAL: Int = 100 // Trial limit ceiling

        private val HEX_CHARS = "0123456789ABCDEF".toCharArray()

        private val cachedSalt: String by lazy {
            val mask = 0x7F
            val obfuscatedSalt = byteArrayOf(
                50, 22, 5, 30, 17, 62, 19, 59, 30, 13,
                44, 26, 28, 10, 13, 26, 44, 30, 19, 11,
                77, 79, 77, 73, 32, 50, 30, 17, 12, 16,
                10, 13
            )
            val decrypted = ByteArray(obfuscatedSalt.size)
            for (i in obfuscatedSalt.indices) {
                decrypted[i] = (obfuscatedSalt[i].toInt() xor mask).toByte()
            }
            String(decrypted, Charsets.UTF_8)
        }

        /**
         * Verifies offline cryptographic activation code against the unified device ID.
         */
        fun verifyActivationCode(deviceId: String, enteredCode: String): Boolean {
            val cleanEntered = enteredCode.trim().uppercase()
            val parts = deviceId.split("-")
            val tempPart = if (parts.size >= 3) parts[1] else ""
            val permPart = if (parts.size >= 3) parts[2] else ""

            val isTemp = cleanEntered.startsWith(PREFIX_TEMP)
            val isPerm = cleanEntered.startsWith(PREFIX_PERM)

            if (!isTemp && !isPerm) return false

            val prefixLength = if (isTemp) PREFIX_TEMP.length else PREFIX_PERM.length
            val targetPart = if (isTemp) tempPart else permPart
            val enteredPayload = cleanEntered.substring(prefixLength)

            val combined = targetPart + cachedSalt
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = md.digest(combined.toByteArray(Charsets.UTF_8))

            // Convert first 4 bytes (8 hex chars) directly
            val hexChars = CharArray(8)
            for (i in 0 until 4) {
                val v = bytes[i].toInt() and 0xFF
                hexChars[i * 2] = HEX_CHARS[v ushr 4]
                hexChars[i * 2 + 1] = HEX_CHARS[v and 0x0F]
            }
            val shaPrefix = String(hexChars)
            val isMatch = HashUtils.secureEquals(enteredPayload, shaPrefix)
            HashUtils.wipeCharArray(hexChars)
            return isMatch
        }

        /**
         * Generates or retrieves the unified device fingerprint securely.
         */
        fun getOrGenerateUnifiedDeviceId(context: Context): String {
            val secManager = AppSecurityManager.getInstance(context)
            var deviceId = secManager.getUnifiedDeviceId()

            if (deviceId.isBlank()) {
                val tempPart = UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
                val androidId = android.provider.Settings.Secure.getString(
                    context.contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                )
                val permPart = if (!androidId.isNullOrBlank()) {
                    androidId.take(8).uppercase()
                } else {
                    "A1B2C3D4"
                }
                deviceId = "MZ-$tempPart-$permPart"
                secManager.setUnifiedDeviceId(deviceId)
            }
            return deviceId
        }
    }

    /**
     * Checks if the app is currently licensed and activated on this device.
     */
    fun isAppActivated(): Boolean {
        val deviceId = getOrGenerateUnifiedDeviceId(appContext)

        val cachedIsActivated = securityManager.isActivatedCached()
        val cachedForDevice = securityManager.getCachedDeviceId()

        val activatedEmail = securityManager.getActivatedEmail()
        val enteredCode = securityManager.getActivationCode()
        val isCodeValid = enteredCode.isNotBlank() && verifyActivationCode(deviceId, enteredCode)

        val isEmailActivated = activatedEmail.isNotBlank()

        if (cachedIsActivated && (cachedForDevice == deviceId || cachedForDevice.isBlank()) && (isCodeValid || isEmailActivated)) {
            return true
        }

        if (isEmailActivated) {
            securityManager.setCachedActivation(true, deviceId)
            return true
        }

        if (isCodeValid) {
            securityManager.setCachedActivation(true, deviceId, enteredCode)
            return true
        }

        return false
    }

    /**
     * Checks if trial period has ended based on real transaction count.
     */
    fun isTrialExpiredDirect(realTotalTransactionsCount: Int): Boolean {
        if (isAppActivated()) {
            return false
        }
        return realTotalTransactionsCount >= SECURE_LIMIT_VAL
    }

    /**
     * Activates the app using a manual activation code.
     */
    fun activateLicenseWithCode(code: String): Boolean {
        val cleanCode = code.trim().uppercase()
        val deviceId = getOrGenerateUnifiedDeviceId(appContext)
        val isValid = verifyActivationCode(deviceId, cleanCode)
        if (isValid) {
            securityManager.setActivationCode(cleanCode)
            securityManager.setCachedActivation(true, deviceId, cleanCode)
        }
        return isValid
    }

    /**
     * Saves cloud email activation locally.
     */
    fun saveEmailActivation(email: String, deviceId: String) {
        securityManager.setActivatedEmail(email)
        securityManager.setCachedActivation(true, deviceId)
    }

    /**
     * Clears local license activation data safely.
     */
    fun clearLocalActivation() {
        securityManager.clearActivationData()
    }

    fun getActivatedEmail(): String = securityManager.getActivatedEmail()

    fun getDeviceId(): String = getOrGenerateUnifiedDeviceId(appContext)
}
