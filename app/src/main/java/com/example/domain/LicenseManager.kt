package com.example.domain

import android.content.Context
import com.example.data.repository.LicenseAndTrialManager

/**
 * LicenseManager isolates and protects license validation, cryptographic hashing,
 * and device fingerprinting logic inside the Domain Layer.
 *
 * Directs all operations to LicenseAndTrialManager as the consolidated single source of truth.
 */
object LicenseManager {
    const val PREFIX_TEMP = LicenseAndTrialManager.PREFIX_TEMP
    const val PREFIX_PERM = LicenseAndTrialManager.PREFIX_PERM
    const val SECURE_LIMIT_VAL: Int = LicenseAndTrialManager.SECURE_LIMIT_VAL

    fun verifyActivationCode(deviceId: String, enteredCode: String): Boolean {
        return LicenseAndTrialManager.verifyActivationCode(deviceId, enteredCode)
    }

    fun getOrGenerateUnifiedDeviceId(context: Context): String {
        return LicenseAndTrialManager.getOrGenerateUnifiedDeviceId(context)
    }
}
