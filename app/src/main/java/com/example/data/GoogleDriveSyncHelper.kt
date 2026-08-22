package com.example.data

import android.content.Context
import android.util.Log
import com.example.data.cloud.CloudNetworkEngine
import com.example.data.local.AppDatabase
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class CloudSyncState {
    object Idle : CloudSyncState()
    object Authenticating : CloudSyncState()
    data class Authenticated(val email: String) : CloudSyncState()
    object Syncing : CloudSyncState()
    object Success : CloudSyncState()
    object Skipped : CloudSyncState()
    data class Error(val message: String) : CloudSyncState()
    object SessionExpired : CloudSyncState()
}

data class CloudBackupFile(
    val id: String,
    val name: String,
    val size: Long,
    val createdTime: String
)

/**
 * واجهة التنسيق والمزامنة السحابية مع Google Drive (Google Drive Sync Facade)
 *
 * المسؤوليات المعمارية الحاكمة:
 * 1. تعمل هذه الفئة كـ Facade موحد يجمع وينسق بين الخدمات المتخصصة:
 *    - GoogleDriveAuthManager: لإدارة الجلسات والرموز المميزة (OAuth & Refresh Tokens).
 *    - GoogleDriveFolderNavigator: للبحث وإنشاء المجلدات وسرد النسخ الاحتياطية.
 *    - GoogleDriveNetworkUploader: لمعالجة تدفقات الرفع والتنزيل وحذف الملفات السحابية.
 * 2. الحفاظ على واجهة برمجة تطبيقات مستقرة لـ ViewModels والـ Workers مع عزل تفاصيل الشبكة.
 * 3. حماية بيانات الاعتماد وتصفير الجلسات بأمان في حالة انتهاء الصلاحية أو عدم التطابق عند إعادة التثبيت.
 */
class GoogleDriveSyncHelper(private val context: Context) {

    companion object {
        private const val TAG = "GoogleDriveSyncHelper"

        private const val LOG_REINSTALL_MISMATCH = "Force Reset On Reinstall Mismatch detected. Executing silent logout."
        private const val LOG_ERROR_INIT_MISMATCH = "Failed to resolve reinstall mismatch in init"
        private const val LOG_ERROR_CHECK_ACCOUNT = "Uncaught error checking getLastSignedInAccount in init"

        suspend fun disconnectAndSignOut(context: Context) = withContext(Dispatchers.IO) {
            try {
                val syncHelper = GoogleDriveSyncHelper(context.applicationContext)
                syncHelper.authManager.disableCloudSyncInSettings()
                syncHelper.authManager.clearAuthData()
                syncHelper.authManager.logout()
                Log.d(TAG, "Unified Session: Disconnected Google Drive and wiped cloud session tokens successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Error in GoogleDriveSyncHelper.disconnectAndSignOut", e)
            }
        }
    }

    private val helperScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val cloudEngine = CloudNetworkEngine.getInstance(context)
    private val client = cloudEngine.client

    private val _syncState = MutableStateFlow<CloudSyncState>(CloudSyncState.Idle)
    val syncState: StateFlow<CloudSyncState> = _syncState.asStateFlow()

    private val authManager = GoogleDriveAuthManager(context, client) { state ->
        _syncState.value = state
    }

    private val folderNavigator = GoogleDriveFolderNavigator(client)
    private val networkUploader = GoogleDriveNetworkUploader(context, client)

    val clientId: String
        get() = authManager.clientId

    val clientSecret: String
        get() = authManager.clientSecret

    val scope: String
        get() = authManager.scope

    fun getClientIdOverride(): String = authManager.getClientIdOverride()
    fun getClientSecretOverride(): String = authManager.getClientSecretOverride()

    fun saveClientCredentialsOverride(clientIdStr: String?, clientSecretStr: String?) {
        authManager.saveClientCredentialsOverride(clientIdStr, clientSecretStr)
    }

    fun getAppSignatureSHA1(): String = authManager.getAppSignatureSHA1()

    init {
        val email = getStoredEmail()
        val refreshToken = getStoredRefreshToken()
        if (!email.isNullOrEmpty() && !refreshToken.isNullOrEmpty()) {
            _syncState.value = CloudSyncState.Authenticated(email)
        }

        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account != null) {
                helperScope.launch {
                    try {
                        val db = AppDatabase.getDatabase(context)
                        val settings = db.settingsDao().getSettingsDirect()
                        if (getStoredRefreshToken().isNullOrEmpty() && (settings == null || !settings.isCloudSyncEnabled)) {
                            Log.d(TAG, LOG_REINSTALL_MISMATCH)
                            getGoogleSignInClient().signOut()
                            authManager.clearAuthData()
                            _syncState.value = CloudSyncState.Idle
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, LOG_ERROR_INIT_MISMATCH, e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, LOG_ERROR_CHECK_ACCOUNT, e)
        }
    }

    fun isUserTrulySignedIn(): Boolean = authManager.isUserTrulySignedIn()
    fun isUserTrulySignedIn(context: Context): Boolean = isUserTrulySignedIn()

    fun getGoogleSignInClient(): GoogleSignInClient = authManager.getGoogleSignInClient()
    fun getGoogleSignInClient(context: Context): GoogleSignInClient = getGoogleSignInClient()

    fun getAuthUrl(): String = authManager.getAuthUrl()

    fun logout() {
        authManager.logout()
    }

    fun logoutAsync(onComplete: () -> Unit) {
        authManager.logoutAsync(onComplete)
    }

    fun getStoredAccessToken(): String? = authManager.getStoredAccessToken()
    fun getStoredRefreshToken(): String? = authManager.getStoredRefreshToken()
    fun getStoredEmail(): String? = authManager.getStoredEmail()
    fun storeEmail(email: String) {
        authManager.storeEmail(email)
    }

    private suspend fun handleSessionExpired() {
        _syncState.value = CloudSyncState.SessionExpired
        authManager.disableCloudSyncInSettings()
        authManager.logout()
    }

    private fun updateSyncState(state: CloudSyncState) {
        _syncState.value = state
    }

    private suspend fun getValidAccessTokenOrExpired(): String? {
        val token = authManager.refreshAccessTokenIfNeeded()
        if (token == null) {
            handleSessionExpired()
        }
        return token
    }

    suspend fun handleAuthorizationCode(code: String, inputEmail: String? = null, redirectUri: String = ""): Boolean {
        return authManager.handleAuthorizationCode(code, inputEmail, redirectUri)
    }

    suspend fun uploadBackupToDrive(backupJsonContent: String): Boolean = withContext(Dispatchers.IO) {
        _syncState.value = CloudSyncState.Syncing
        val accessToken = getValidAccessTokenOrExpired() ?: return@withContext false
        val email = authManager.getStoredEmail()

        networkUploader.uploadBackupToDrive(
            backupJsonContent = backupJsonContent,
            accessToken = accessToken,
            folderNavigator = folderNavigator,
            updateState = ::updateSyncState,
            onAuthError = { handleSessionExpired() },
            email = email
        )
    }

    suspend fun downloadBackupFromDrive(): String? = withContext(Dispatchers.IO) {
        _syncState.value = CloudSyncState.Syncing
        val accessToken = getValidAccessTokenOrExpired() ?: return@withContext null
        val email = authManager.getStoredEmail()

        networkUploader.downloadBackupFromDrive(
            accessToken = accessToken,
            folderNavigator = folderNavigator,
            updateState = ::updateSyncState,
            onAuthError = { handleSessionExpired() },
            email = email
        )
    }

    suspend fun listCloudBackups(): List<CloudBackupFile> = withContext(Dispatchers.IO) {
        val accessToken = authManager.refreshAccessTokenIfNeeded()
        if (accessToken == null) {
            handleSessionExpired()
            return@withContext emptyList()
        }
        val result = folderNavigator.listCloudBackups(accessToken)
        when (result) {
            is GoogleDriveFolderNavigator.ListBackupsResult.Success -> result.backups
            is GoogleDriveFolderNavigator.ListBackupsResult.Error -> {
                if (result.isAuthError) {
                    handleSessionExpired()
                }
                emptyList()
            }
        }
    }

    suspend fun uploadBackupToDriveWithFilename(filename: String, backupJsonContent: String): Boolean = withContext(Dispatchers.IO) {
        _syncState.value = CloudSyncState.Syncing
        val accessToken = getValidAccessTokenOrExpired() ?: return@withContext false
        val email = authManager.getStoredEmail()

        networkUploader.uploadBackupToDriveWithFilename(
            filename = filename,
            backupJsonContent = backupJsonContent,
            accessToken = accessToken,
            updateState = ::updateSyncState,
            email = email
        )
    }

    suspend fun downloadBackupFromDriveById(fileId: String): String? = withContext(Dispatchers.IO) {
        _syncState.value = CloudSyncState.Syncing
        val accessToken = getValidAccessTokenOrExpired() ?: return@withContext null
        val email = authManager.getStoredEmail()

        networkUploader.downloadBackupFromDriveById(
            fileId = fileId,
            accessToken = accessToken,
            updateState = ::updateSyncState,
            onAuthError = { handleSessionExpired() },
            email = email
        )
    }

    suspend fun deleteBackupFromDriveById(fileId: String): Boolean = withContext(Dispatchers.IO) {
        val accessToken = authManager.refreshAccessTokenIfNeeded() ?: return@withContext false
        networkUploader.deleteBackupFromDriveById(fileId, accessToken)
    }
}

object GoogleDriveHelper {
    suspend fun disconnectAndSignOut(context: Context) {
        GoogleDriveSyncHelper.disconnectAndSignOut(context)
    }
}
