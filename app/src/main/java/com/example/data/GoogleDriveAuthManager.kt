package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.cloud.CloudNetworkEngine
import com.example.data.local.AppDatabase
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Manages Google Drive OAuth2 authentication and token persistence.
 * Delegates token management and network operations to [CloudNetworkEngine].
 */
class GoogleDriveAuthManager(
    private val context: Context,
    private val client: OkHttpClient,
    private val updateState: (CloudSyncState) -> Unit
) {
    companion object {
        private const val TAG = "GoogleDriveAuthManager"
        const val SCOPE_DRIVE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"
        const val SCOPE_DRIVE_FILE = "https://www.googleapis.com/auth/drive.file"
        const val SCOPE_USERINFO_EMAIL = "https://www.googleapis.com/auth/userinfo.email"
        const val FULL_OAUTH_SCOPES = "$SCOPE_DRIVE_APPDATA $SCOPE_DRIVE_FILE $SCOPE_USERINFO_EMAIL"

        const val AUTH_ENDPOINT_URL = "https://accounts.google.com/o/oauth2/v2/auth"
        const val TOKEN_ENDPOINT_URL = "https://oauth2.googleapis.com/token"
        const val USERINFO_ENDPOINT_URL = "https://www.googleapis.com/oauth2/v2/userinfo"
        const val REDIRECT_URI_LOCAL = "http://localhost/oauth2callback"
    }

    private val cloudEngine = CloudNetworkEngine.getInstance(context)

    val sharedPrefs: SharedPreferences
        get() = cloudEngine.sharedPrefs

    val clientId: String
        get() = cloudEngine.clientId

    val clientSecret: String
        get() = cloudEngine.clientSecret

    val scope: String
        get() = FULL_OAUTH_SCOPES

    fun getClientIdOverride(): String = sharedPrefs.getString("client_id_override", "") ?: ""
    fun getClientSecretOverride(): String = sharedPrefs.getString("client_secret_override", "") ?: ""

    fun saveClientCredentialsOverride(clientIdStr: String?, clientSecretStr: String?) {
        val editor = sharedPrefs.edit()
        if (clientIdStr.isNullOrEmpty()) {
            editor.remove("client_id_override")
        } else {
            editor.putString("client_id_override", clientIdStr.trim())
        }
        if (clientSecretStr.isNullOrEmpty()) {
            editor.remove("client_secret_override")
        } else {
            editor.putString("client_secret_override", clientSecretStr.trim())
        }
        editor.apply()
    }

    fun getAppSignatureSHA1(): String {
        try {
            val info = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.GET_SIGNATURES
                )
            }

            val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                info.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                info.signatures
            }

            if (signatures != null && signatures.isNotEmpty()) {
                val sig = signatures[0]
                val md = java.security.MessageDigest.getInstance("SHA-1")
                val publicKey = md.digest(sig.toByteArray())
                return publicKey.joinToString(":") { String.format("%02X", it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching SHA-1", e)
        }
        return context.getString(com.example.R.string.gdrive_sha1_fetch_failed)
    }

    fun isUserTrulySignedIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        val requiredScope = Scope(SCOPE_DRIVE_FILE)
        return account != null && GoogleSignIn.hasPermissions(account, requiredScope)
    }

    fun getGoogleSignInClient(): GoogleSignInClient {
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                Scope(SCOPE_DRIVE_APPDATA),
                Scope(SCOPE_DRIVE_FILE)
            )
        if (!clientId.isNullOrBlank()) {
            try {
                builder.requestServerAuthCode(clientId, false)
            } catch (e: Exception) {
                Log.e(TAG, "Error setting server auth code on GoogleSignInOptions", e)
            }
        }
        return GoogleSignIn.getClient(context, builder.build())
    }

    fun getAuthUrl(): String {
        return "$AUTH_ENDPOINT_URL" +
                "?client_id=${URLEncoder.encode(clientId, "UTF-8")}" +
                "&redirect_uri=${URLEncoder.encode(REDIRECT_URI_LOCAL, "UTF-8")}" +
                "&response_type=code" +
                "&scope=${URLEncoder.encode(scope, "UTF-8")}" +
                "&prompt=consent" +
                "&access_type=offline"
    }

    fun clearAuthData() {
        cloudEngine.clearAuthSession()
    }

    fun logout() {
        logoutAsync(onComplete = null)
    }

    fun logoutAsync(onComplete: (() -> Unit)? = null) {
        try {
            val signInClient = getGoogleSignInClient()
            signInClient.revokeAccess().addOnCompleteListener {
                signInClient.signOut().addOnCompleteListener {
                    clearAuthData()
                    updateState(CloudSyncState.Idle)
                    onComplete?.invoke()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during deep revokeAccess and signOut on logoutAsync", e)
            clearAuthData()
            updateState(CloudSyncState.Idle)
            onComplete?.invoke()
        }
    }

    fun storeTokens(accessToken: String, refreshToken: String?, expiresInSec: Long) {
        cloudEngine.storeTokens(accessToken, refreshToken, expiresInSec)
    }

    fun storeEmail(email: String) {
        cloudEngine.storeEmail(email)
    }

    fun getStoredAccessToken(): String? = cloudEngine.getStoredAccessToken()
    fun getStoredRefreshToken(): String? = cloudEngine.getStoredRefreshToken()
    fun getStoredEmail(): String? = cloudEngine.getStoredEmail()

    fun isTokenExpired(): Boolean = cloudEngine.isTokenExpired()

    suspend fun disableCloudSyncInSettings() {
        try {
            val db = AppDatabase.getDatabase(context)
            val settings = db.settingsDao().getSettingsDirect()
            if (settings != null && settings.isCloudSyncEnabled) {
                db.settingsDao().insertOrUpdateSettings(settings.copy(isCloudSyncEnabled = false))
                Log.d(TAG, "Successfully deactivated isCloudSyncEnabled in DB due to Security/Auth failure.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed disabling cloud sync from helper", e)
        }
    }

    suspend fun handleAuthorizationCode(code: String, inputEmail: String? = null, redirectUri: String = ""): Boolean = withContext(Dispatchers.IO) {
        updateState(CloudSyncState.Authenticating)
        val sanitizedCode = code.trim()
            .replace("\\s".toRegex(), "")
            .filter { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it in listOf('-', '_', '/', '.') }
        
        Log.d(TAG, "Exchanging auth code (Original length: ${code.length}, Sanitized length: ${sanitizedCode.length})")
        
        try {
            cloudEngine.executeWithRetry(maxRetries = 2, initialDelayMs = 500L) {
                val builder = FormBody.Builder()
                    .add("code", sanitizedCode)
                    .add("client_id", clientId.trim())
                    .add("grant_type", "authorization_code")

                val trimmedSecret = clientSecret.trim()
                if (trimmedSecret.isNotEmpty() && trimmedSecret != "YOUR_GOOGLE_CLIENT_SECRET") {
                    builder.add("client_secret", trimmedSecret)
                }
                if (redirectUri.isNotEmpty()) {
                    builder.add("redirect_uri", redirectUri.trim())
                }
                
                val requestBody = builder.build()

                val request = Request.Builder()
                    .url(TOKEN_ENDPOINT_URL)
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val rawBody = response.body?.string() ?: ""
                        val json = JSONObject(rawBody)
                        val accessToken = json.getString("access_token")
                        val refreshToken = json.optString("refresh_token", "").takeIf { it.isNotEmpty() } ?: getStoredRefreshToken()
                        val expiresIn = json.optLong("expires_in", 3600L)

                        storeTokens(accessToken, refreshToken, expiresIn)

                        val email = inputEmail ?: fetchUserEmail(accessToken) ?: "account@google.com"
                        storeEmail(email)

                        updateState(CloudSyncState.Authenticated(email))
                        true
                    } else {
                        val errorMsg = response.body?.string() ?: "Unknown OAuth code exchange error"
                        Log.e(TAG, "Authorization code exchange failed. Error: $errorMsg")
                        var detailedError = ""
                        try {
                            val json = JSONObject(errorMsg)
                            val err = json.optString("error")
                            val desc = json.optString("error_description")
                            detailedError = if (err.isNotEmpty() && desc.isNotEmpty()) {
                                "$err: $desc"
                            } else if (err.isNotEmpty()) {
                                err
                            } else {
                                errorMsg
                            }
                        } catch (e: Exception) {
                            detailedError = errorMsg
                        }
                        updateState(
                            CloudSyncState.Error(
                                context.getString(com.example.R.string.gdrive_error_link_failed) + "\n($detailedError)"
                            )
                        )
                        false
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network failure during authorization code exchange", e)
            updateState(CloudSyncState.Error(context.getString(com.example.R.string.gdrive_error_network_unstable) + "\n(${e.localizedMessage ?: ""})"))
            false
        }
    }

    private suspend fun fetchUserEmail(accessToken: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(USERINFO_ENDPOINT_URL)
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val rawBody = response.body?.string() ?: ""
                    val json = JSONObject(rawBody)
                    if (json.has("email")) json.getString("email") else null
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed retrieving user account info safely", e)
            null
        }
    }

    suspend fun refreshAccessTokenIfNeeded(): String? = cloudEngine.refreshAccessTokenIfNeeded()
}
