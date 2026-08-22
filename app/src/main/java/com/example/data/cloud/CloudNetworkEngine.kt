package com.example.data.cloud

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.BuildConfig
import com.example.data.serialization.BackupPayloadSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Unified Cloud Network Engine.
 * Centralizes OAuth2 token management, network error handling, exponential backoff retries,
 * and cryptographic SHA-256 payload checksum auditing (Zero-Diff deduplication).
 */
class CloudNetworkEngine(private val context: Context) {

    companion object {
        private const val TAG = "CloudNetworkEngine"
        private const val PREFS_NAME = "secure_google_drive_sync_prefs"
        private const val FALLBACK_PREFS_NAME = "google_drive_sync_prefs"
        private const val KEY_LAST_UPLOADED_HASH = "last_uploaded_payload_hash"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
        private const val KEY_EMAIL = "email"
        private const val KEY_CLIENT_ID_OVERRIDE = "client_id_override"
        private const val KEY_CLIENT_SECRET_OVERRIDE = "client_secret_override"

        private const val OAUTH_TOKEN_URL = "https://oauth2.googleapis.com/token"
        private const val GRANT_TYPE_REFRESH_TOKEN = "refresh_token"
        private const val PARAM_CLIENT_ID = "client_id"
        private const val PARAM_CLIENT_SECRET = "client_secret"
        private const val PARAM_REFRESH_TOKEN = "refresh_token"
        private const val PARAM_GRANT_TYPE = "grant_type"
        private const val RESPONSE_ACCESS_TOKEN = "access_token"
        private const val RESPONSE_EXPIRES_IN = "expires_in"
        private const val TOKEN_EXPIRY_BUFFER_MS = 300_000L
        private const val DEFAULT_EXPIRES_IN_SEC = 3600L

        @Volatile
        private var instance: CloudNetworkEngine? = null

        fun getInstance(context: Context): CloudNetworkEngine {
            return instance ?: synchronized(this) {
                instance ?: CloudNetworkEngine(context.applicationContext).also { instance = it }
            }
        }
    }

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val sharedPrefs: SharedPreferences by lazy {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating EncryptedSharedPreferences, fallback used", e)
            context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    // Credentials Resolvers
    val clientId: String
        get() = sharedPrefs.getString(KEY_CLIENT_ID_OVERRIDE, null)?.takeIf { it.isNotEmpty() } ?: BuildConfig.GOOGLE_CLIENT_ID

    val clientSecret: String
        get() = sharedPrefs.getString(KEY_CLIENT_SECRET_OVERRIDE, null)?.takeIf { it.isNotEmpty() } ?: BuildConfig.GOOGLE_CLIENT_SECRET

    // Zero-Diff Smart Deduplication Audit
    fun getStoredPayloadHash(): String? = sharedPrefs.getString(KEY_LAST_UPLOADED_HASH, null)

    fun saveLastUploadedPayloadHash(hash: String) {
        sharedPrefs.edit().putString(KEY_LAST_UPLOADED_HASH, hash).apply()
        Log.d(TAG, "Updated last uploaded cloud payload hash: $hash")
    }

    /**
     * Audit local payload against last uploaded cloud payload.
     * Returns true if local content is 100% identical to remote (Zero-Diff).
     */
    fun isPayloadIdentical(jsonContent: String): Boolean {
        val currentHash = BackupPayloadSerializer.calculateSha256Hash(jsonContent)
        val storedHash = getStoredPayloadHash()
        val match = storedHash != null && storedHash == currentHash
        if (match) {
            Log.i(TAG, "Zero-Diff Check: Local payload hash matches last cloud upload ($currentHash). Skipping redundant upload.")
        } else {
            Log.d(TAG, "Zero-Diff Check: Payload diff detected (Current: $currentHash, Stored: $storedHash). Proceeding with upload.")
        }
        return match
    }

    // Token Management
    fun storeTokens(accessToken: String, refreshToken: String?, expiresInSec: Long) {
        val editor = sharedPrefs.edit()
        editor.putString(KEY_ACCESS_TOKEN, accessToken)
        if (!refreshToken.isNullOrEmpty()) {
            editor.putString(KEY_REFRESH_TOKEN, refreshToken)
        }
        editor.putLong(KEY_TOKEN_EXPIRY, System.currentTimeMillis() + (expiresInSec * 1000))
        editor.apply()
    }

    fun storeEmail(email: String) {
        sharedPrefs.edit().putString(KEY_EMAIL, email).apply()
        com.example.domain.GoogleAuthSessionManager.updateEmail(email)
    }

    fun getStoredAccessToken(): String? = sharedPrefs.getString(KEY_ACCESS_TOKEN, null)
    fun getStoredRefreshToken(): String? = sharedPrefs.getString(KEY_REFRESH_TOKEN, null)
    fun getStoredEmail(): String? = sharedPrefs.getString(KEY_EMAIL, null)

    fun isTokenExpired(): Boolean {
        val expiry = sharedPrefs.getLong(KEY_TOKEN_EXPIRY, 0)
        return System.currentTimeMillis() >= (expiry - TOKEN_EXPIRY_BUFFER_MS)
    }

    fun clearAuthSession() {
        sharedPrefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_TOKEN_EXPIRY)
            .remove(KEY_EMAIL)
            .apply()
        com.example.domain.GoogleAuthSessionManager.clearSession()
    }

    /**
     * Executes any suspended network operation with exponential backoff retry.
     */
    suspend fun <T> executeWithRetry(
        maxRetries: Int = 3,
        initialDelayMs: Long = 1000L,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T = withContext(Dispatchers.IO) {
        var currentDelay = initialDelayMs
        var lastException: Exception? = null

        for (attempt in 1..maxRetries) {
            try {
                return@withContext block()
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Network action failed on attempt $attempt of $maxRetries: ${e.message}")
                if (attempt < maxRetries) {
                    delay(currentDelay)
                    currentDelay = (currentDelay * factor).toLong()
                }
            }
        }
        throw lastException ?: IllegalStateException("Execution failed after $maxRetries retries")
    }

    /**
     * Refreshes OAuth2 access token securely using stored refresh token with retry policy.
     */
    suspend fun refreshAccessTokenIfNeeded(): String? = withContext(Dispatchers.IO) {
        val refreshToken = getStoredRefreshToken()
        if (refreshToken.isNullOrEmpty()) {
            return@withContext null
        }

        if (!isTokenExpired()) {
            val currentToken = getStoredAccessToken()
            if (!currentToken.isNullOrEmpty()) {
                return@withContext currentToken
            }
        }

        try {
            executeWithRetry(maxRetries = 2, initialDelayMs = 500L) {
                val formBuilder = FormBody.Builder()
                    .add(PARAM_CLIENT_ID, clientId)
                    .add(PARAM_REFRESH_TOKEN, refreshToken)
                    .add(PARAM_GRANT_TYPE, GRANT_TYPE_REFRESH_TOKEN)
                if (clientSecret.isNotEmpty()) {
                    formBuilder.add(PARAM_CLIENT_SECRET, clientSecret)
                }
                val requestBody = formBuilder.build()

                val request = Request.Builder()
                    .url(OAUTH_TOKEN_URL)
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val rawBody = response.body?.string() ?: ""
                        val json = JSONObject(rawBody)
                        val accessToken = json.getString(RESPONSE_ACCESS_TOKEN)
                        val expiresIn = json.optLong(RESPONSE_EXPIRES_IN, DEFAULT_EXPIRES_IN_SEC)

                        storeTokens(accessToken, refreshToken, expiresIn)
                        accessToken
                    } else {
                        Log.e(TAG, "AccessToken refresh returned error status: ${response.code}")
                        if (response.code == 400 || response.code == 401) {
                            clearAuthSession()
                        }
                        null
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error renewing credentials in CloudNetworkEngine", e)
            null
        }
    }
}
