package com.example.domain

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class LicenseCheckResult {
    data class Success(val email: String, val deviceId: String, val isTransferred: Boolean = false) : LicenseCheckResult()
    data class DeviceMismatch(val email: String, val activeDeviceId: String, val currentDeviceId: String) : LicenseCheckResult()
    data class NotLicensed(val email: String, val message: String) : LicenseCheckResult()
    data class Error(val message: String) : LicenseCheckResult()
}

object FirebaseLicenseManager {

    private const val TAG = "FirebaseLicenseManager"
    private const val COLLECTION_LICENSES = "licenses"
    private var licenseListenerRegistration: ListenerRegistration? = null

    private fun normalizeEmail(email: String): String {
        return email.trim().lowercase()
    }

    private fun ensureAuthenticated() {
        try {
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser == null) {
                Log.d(TAG, "Current Firebase user is unauthenticated; requests will proceed with default credentials")
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Firebase auth state check notice: ${t.message}")
        }
    }

    suspend fun verifyAndActivateEmail(context: Context, email: String, currentDeviceId: String): LicenseCheckResult {
        return verifyAndActivateEmailWithFifo(context, email, currentDeviceId)
    }

    suspend fun verifyAndActivateEmailWithFifo(context: Context, email: String, currentDeviceId: String): LicenseCheckResult {
        val cleanEmail = normalizeEmail(email)
        if (cleanEmail.isEmpty() || !cleanEmail.contains("@")) {
            return LicenseCheckResult.Error(context.getString(R.string.licensing_error_invalid_email))
        }

        return try {
            ensureAuthenticated()
            val db = FirebaseFirestore.getInstance()
            val docRef = db.collection(COLLECTION_LICENSES).document(cleanEmail)

            var isTransferred = false

            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)

                if (!snapshot.exists()) {
                    throw IllegalStateException("NOT_REGISTERED")
                }

                val isActivated = snapshot.getBoolean("is_activated") ?: false
                if (!isActivated) {
                    throw IllegalStateException("ACCOUNT_DISABLED")
                }

                val devicesMax = (snapshot.getLong("devices_max") ?: 1L).toInt().coerceAtLeast(1)
                @Suppress("UNCHECKED_CAST")
                val activeDevices = (snapshot.get("active_devices") as? List<String>)?.toMutableList()
                    ?: mutableListOf()

                val legacyActiveDevice = snapshot.getString("active_device_id") ?: ""
                if (activeDevices.isEmpty() && legacyActiveDevice.isNotEmpty()) {
                    activeDevices.add(legacyActiveDevice)
                }

                // If current device is not in list
                if (!activeDevices.contains(currentDeviceId)) {
                    isTransferred = activeDevices.isNotEmpty()

                    // Apply FIFO queue: if max reached, remove oldest (index 0)
                    while (activeDevices.size >= devicesMax && activeDevices.isNotEmpty()) {
                        activeDevices.removeAt(0)
                    }
                    activeDevices.add(currentDeviceId)
                }

                val updates = mapOf(
                    "email" to cleanEmail,
                    "is_activated" to true,
                    "devices_max" to devicesMax,
                    "active_devices" to activeDevices,
                    "active_device_id" to currentDeviceId, // backward compatibility
                    "last_updated" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                )

                transaction.set(docRef, updates, SetOptions.merge())
            }.await()

            LicenseCheckResult.Success(email = cleanEmail, deviceId = currentDeviceId, isTransferred = isTransferred)
        } catch (t: Throwable) {
            Log.e(TAG, "Error verifying email license safely", t)
            LicenseCheckResult.NotLicensed(cleanEmail, context.getString(R.string.licensing_error_not_registered))
        }
    }

    /**
     * Realtime Snapshot Listener for continuous license & device authorization monitoring.
     */
    fun startRealtimeLicenseMonitoring(
        context: Context,
        email: String,
        currentDeviceId: String,
        onKickedOrDisabled: (reason: String) -> Unit
    ) {
        stopRealtimeLicenseMonitoring()
        val cleanEmail = normalizeEmail(email)
        if (cleanEmail.isEmpty()) return

        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection(COLLECTION_LICENSES).document(cleanEmail)

        licenseListenerRegistration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Realtime listener error", error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val isActivated = snapshot.getBoolean("is_activated") ?: false
                @Suppress("UNCHECKED_CAST")
                val activeDevices = snapshot.get("active_devices") as? List<String> ?: emptyList()
                val legacyActiveDevice = snapshot.getString("active_device_id") ?: ""

                val isDeviceAuthorized = activeDevices.contains(currentDeviceId) || legacyActiveDevice == currentDeviceId

                if (!isActivated) {
                    Log.w(TAG, "Account disabled remotely by Admin.")
                    onKickedOrDisabled(context.getString(R.string.licensing_error_account_disabled))
                } else if (!isDeviceAuthorized && (activeDevices.isNotEmpty() || legacyActiveDevice.isNotEmpty())) {
                    Log.w(TAG, "Device kicked out due to multi-device FIFO limit or unlinking.")
                    onKickedOrDisabled(context.getString(R.string.licensing_device_kicked))
                }
            } else if (snapshot != null && !snapshot.exists()) {
                Log.w(TAG, "License document deleted.")
                onKickedOrDisabled(context.getString(R.string.licensing_license_deleted))
            }
        }
    }

    fun stopRealtimeLicenseMonitoring() {
        licenseListenerRegistration?.remove()
        licenseListenerRegistration = null
    }

    suspend fun unlinkDevice(email: String): Boolean {
        val cleanEmail = normalizeEmail(email)
        if (cleanEmail.isEmpty()) return false
        return try {
            ensureAuthenticated()
            val db = FirebaseFirestore.getInstance()
            val docRef = db.collection(COLLECTION_LICENSES).document(cleanEmail)

            val updates = mapOf<String, Any>(
                "active_device_id" to "",
                "active_devices" to emptyList<String>(),
                "last_updated" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            )
            docRef.set(updates, SetOptions.merge()).await()
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Error unlinking device safely", t)
            false
        }
    }

    suspend fun syncAndVerifyLocalEmailLicense(context: Context): Boolean {
        val securityManager = AppSecurityManager.getInstance(context)
        val email = securityManager.getActivatedEmail()
        if (email.isBlank()) return false

        val currentDeviceId = LicenseManager.getOrGenerateUnifiedDeviceId(context)

        return try {
            ensureAuthenticated()
            val cleanEmail = normalizeEmail(email)
            val db = FirebaseFirestore.getInstance()
            val snapshot = db.collection(COLLECTION_LICENSES).document(cleanEmail).get().await()

            if (snapshot != null && snapshot.exists()) {
                val isActivated = snapshot.getBoolean("is_activated") ?: false
                val activeDeviceId = snapshot.getString("active_device_id") ?: ""
                @Suppress("UNCHECKED_CAST")
                val activeDevices = snapshot.get("active_devices") as? List<String> ?: emptyList()

                val isAuthorized = activeDevices.contains(currentDeviceId) || activeDeviceId == currentDeviceId

                if (isActivated && isAuthorized) {
                    // Valid on Firebase for this device! Ensure local cache is in sync
                    securityManager.setCachedActivation(true, currentDeviceId)
                    true
                } else {
                    // License transferred to another device or deactivated on Firestore! Clear local activation
                    securityManager.clearActivationData()
                    false
                }
            } else {
                // Document deleted or not existing
                securityManager.clearActivationData()
                false
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Sync email license check failed safely (offline or exception): ${t.message}")
            // Offline fallback: trust local cache if device matches
            val cachedIsActivated = securityManager.isActivatedCached()
            val cachedForDevice = securityManager.getCachedDeviceId()
            cachedIsActivated && (cachedForDevice == currentDeviceId || cachedForDevice.isBlank())
        }
    }
}
