package com.example.domain

import android.content.Context
import android.util.Log
import com.example.data.cloud.CloudNetworkEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * GoogleAuthSessionManager serves as the single source of truth for the Google Auth session across the entire app.
 * It ensures that logging in/out of a Google account anywhere (Activation, Settings, Backup) synchronizes immediately.
 */
object GoogleAuthSessionManager {
    private const val TAG = "GoogleAuthSession"
    private val _currentEmail = MutableStateFlow<String?>(null)
    val currentEmail: StateFlow<String?> = _currentEmail.asStateFlow()

    fun initialize(context: Context) {
        try {
            val engine = CloudNetworkEngine.getInstance(context)
            val email = engine.getStoredEmail()
            _currentEmail.value = email.takeIf { !it.isNullOrBlank() }
            Log.d(TAG, "Initialized unified Google Auth Session with email: ${_currentEmail.value}")
        } catch (t: Throwable) {
            Log.e(TAG, "Error initializing GoogleAuthSessionManager", t)
        }
    }

    fun updateEmail(email: String?) {
        val cleanEmail = email?.trim()?.lowercase()
        _currentEmail.value = cleanEmail.takeIf { !it.isNullOrBlank() }
        Log.d(TAG, "Unified Google Auth Session email updated to: ${_currentEmail.value}")
    }

    fun clearSession() {
        _currentEmail.value = null
        Log.d(TAG, "Unified Google Auth Session cleared.")
    }
}
