package com.example.domain

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * AppSecurityManager - Centralized Cryptographic Storage & Session Guard
 *
 * Implements hardware-backed EncryptedSharedPreferences (AES-256-GCM / AES-256-SIV)
 * with transparent migration from legacy stores and fail-safe fallback to standard
 * preferences if Keystore hardware is unavailable.
 */
class AppSecurityManager private constructor(context: Context) {

    private val appContext: Context = context.applicationContext

    private val securePrefs: SharedPreferences by lazy {
        initEncryptedPreferences()
    }

    private val legacyPrefs: SharedPreferences by lazy {
        appContext.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
    }

    init {
        migrateLegacyPreferencesIfNeeded()
    }

    private fun initEncryptedPreferences(): SharedPreferences {
        return try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

            EncryptedSharedPreferences.create(
                ENCRYPTED_PREFS_NAME,
                masterKeyAlias,
                appContext,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (t: Throwable) {
            Log.w(TAG, "EncryptedSharedPreferences initialization failed; falling back to legacy protected store: ${t.message}")
            legacyPrefs
        }
    }

    private fun migrateLegacyPreferencesIfNeeded() {
        try {
            if (legacyPrefs.all.isNotEmpty()) {
                val targetPrefs = if (securePrefs !== legacyPrefs) securePrefs else null
                if (targetPrefs != null) {
                    val editor = targetPrefs.edit()
                    for ((key, value) in legacyPrefs.all) {
                        if (!targetPrefs.contains(key)) {
                            when (value) {
                                is String -> editor.putString(key, value)
                                is Boolean -> editor.putBoolean(key, value)
                                is Int -> editor.putInt(key, value)
                                is Long -> editor.putLong(key, value)
                                is Float -> editor.putFloat(key, value)
                                is Set<*> -> {
                                    @Suppress("UNCHECKED_CAST")
                                    editor.putStringSet(key, value as Set<String>)
                                }
                            }
                        }
                    }
                    editor.apply()
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Migration of legacy security preferences completed with warning: ${t.message}")
        }
    }

    // --- LICENSE & ACTIVATION MANAGEMENT ---

    fun getActivationCode(): String {
        return securePrefs.getString(PREF_M_ACT_CODE, "") ?: legacyPrefs.getString(PREF_M_ACT_CODE, "") ?: ""
    }

    fun setActivationCode(code: String) {
        val clean = code.trim().uppercase()
        securePrefs.edit().putString(PREF_M_ACT_CODE, clean).apply()
        if (securePrefs !== legacyPrefs) {
            legacyPrefs.edit().putString(PREF_M_ACT_CODE, clean).apply()
        }
    }

    fun getActivatedEmail(): String {
        return securePrefs.getString(PREF_M_ACTIVATED_EMAIL, "") ?: legacyPrefs.getString(PREF_M_ACTIVATED_EMAIL, "") ?: ""
    }

    fun setActivatedEmail(email: String) {
        val clean = email.trim().lowercase()
        securePrefs.edit().putString(PREF_M_ACTIVATED_EMAIL, clean).apply()
        if (securePrefs !== legacyPrefs) {
            legacyPrefs.edit().putString(PREF_M_ACTIVATED_EMAIL, clean).apply()
        }
    }

    fun isActivatedCached(): Boolean {
        return securePrefs.getBoolean(PREF_IS_ACTIVATED_CACHED, false) || legacyPrefs.getBoolean(PREF_IS_ACTIVATED_CACHED, false)
    }

    fun getCachedDeviceId(): String {
        return securePrefs.getString(PREF_CACHED_FOR_DEVICE, "") ?: legacyPrefs.getString(PREF_CACHED_FOR_DEVICE, "") ?: ""
    }

    fun setCachedActivation(isActivated: Boolean, deviceId: String = "", code: String = "") {
        val editor = securePrefs.edit()
            .putBoolean(PREF_IS_ACTIVATED_CACHED, isActivated)
            .putBoolean(PREF_IS_PREMIUM, isActivated)

        if (deviceId.isNotBlank()) {
            editor.putString(PREF_CACHED_FOR_DEVICE, deviceId)
        }
        if (code.isNotBlank()) {
            editor.putString(PREF_CACHED_FOR_CODE, code)
        }
        editor.apply()

        if (securePrefs !== legacyPrefs) {
            val legacyEditor = legacyPrefs.edit()
                .putBoolean(PREF_IS_ACTIVATED_CACHED, isActivated)
                .putBoolean(PREF_IS_PREMIUM, isActivated)
            if (deviceId.isNotBlank()) legacyEditor.putString(PREF_CACHED_FOR_DEVICE, deviceId)
            if (code.isNotBlank()) legacyEditor.putString(PREF_CACHED_FOR_CODE, code)
            legacyEditor.apply()
        }
    }

    fun clearActivationData() {
        securePrefs.edit()
            .remove(PREF_M_ACTIVATED_EMAIL)
            .remove(PREF_M_ACT_CODE)
            .putBoolean(PREF_IS_PREMIUM, false)
            .putBoolean(PREF_IS_ACTIVATED_CACHED, false)
            .remove(PREF_CACHED_FOR_CODE)
            .remove(PREF_CACHED_FOR_DEVICE)
            .apply()

        if (securePrefs !== legacyPrefs) {
            legacyPrefs.edit()
                .remove(PREF_M_ACTIVATED_EMAIL)
                .remove(PREF_M_ACT_CODE)
                .putBoolean(PREF_IS_PREMIUM, false)
                .putBoolean(PREF_IS_ACTIVATED_CACHED, false)
                .remove(PREF_CACHED_FOR_CODE)
                .remove(PREF_CACHED_FOR_DEVICE)
                .apply()
        }
    }

    fun getUnifiedDeviceId(): String {
        val deviceId = securePrefs.getString(PREF_UNIFIED_DEVICE_ID, "") ?: ""
        if (deviceId.isNotBlank()) return deviceId
        return legacyPrefs.getString(PREF_UNIFIED_DEVICE_ID, "") ?: ""
    }

    fun setUnifiedDeviceId(deviceId: String) {
        securePrefs.edit().putString(PREF_UNIFIED_DEVICE_ID, deviceId).apply()
        if (securePrefs !== legacyPrefs) {
            legacyPrefs.edit().putString(PREF_UNIFIED_DEVICE_ID, deviceId).apply()
        }
    }

    // --- SECURITY & BIOMETRIC SETTINGS ---

    fun isFastPasscodeEnabled(): Boolean {
        return securePrefs.getBoolean(PREF_FAST_PASSCODE_ENABLED, false) || legacyPrefs.getBoolean(PREF_FAST_PASSCODE_ENABLED, false)
    }

    fun setFastPasscodeEnabled(enabled: Boolean) {
        securePrefs.edit().putBoolean(PREF_FAST_PASSCODE_ENABLED, enabled).apply()
        if (securePrefs !== legacyPrefs) {
            legacyPrefs.edit().putBoolean(PREF_FAST_PASSCODE_ENABLED, enabled).apply()
        }
    }

    fun isBiometricEnabled(): Boolean {
        // Default enabled when passcode is on
        return securePrefs.getBoolean(PREF_BIOMETRIC_ENABLED, true)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        securePrefs.edit().putBoolean(PREF_BIOMETRIC_ENABLED, enabled).apply()
        if (securePrefs !== legacyPrefs) {
            legacyPrefs.edit().putBoolean(PREF_BIOMETRIC_ENABLED, enabled).apply()
        }
    }

    // --- LISTENER REGISTRATION ---

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        securePrefs.registerOnSharedPreferenceChangeListener(listener)
        if (securePrefs !== legacyPrefs) {
            legacyPrefs.registerOnSharedPreferenceChangeListener(listener)
        }
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        securePrefs.unregisterOnSharedPreferenceChangeListener(listener)
        if (securePrefs !== legacyPrefs) {
            legacyPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    companion object {
        private const val TAG = "AppSecurityManager"
        private const val ENCRYPTED_PREFS_NAME = "mizan_encrypted_sec_prefs"
        private const val LEGACY_PREFS_NAME = "mizan_sec_prefs"

        const val PREF_M_ACT_CODE = "m_act_code"
        const val PREF_M_ACTIVATED_EMAIL = "m_activated_email"
        const val PREF_IS_ACTIVATED_CACHED = "is_activated_cached"
        const val PREF_CACHED_FOR_DEVICE = "cached_for_device"
        const val PREF_CACHED_FOR_CODE = "cached_for_code"
        const val PREF_IS_PREMIUM = "is_premium"
        const val PREF_IS_PERMANENT = "is_permanent"
        const val PREF_UNIFIED_DEVICE_ID = "unified_device_id"
        const val PREF_FAST_PASSCODE_ENABLED = "fast_passcode_enabled"
        const val PREF_BIOMETRIC_ENABLED = "biometric_enabled"

        @Volatile
        private var INSTANCE: AppSecurityManager? = null

        fun getInstance(context: Context): AppSecurityManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppSecurityManager(context).also { INSTANCE = it }
            }
        }
    }
}
