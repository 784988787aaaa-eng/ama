package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CloudBackupFile
import com.example.data.CloudSyncState
import com.example.data.GoogleDriveSyncHelper
import com.example.data.local.AppDatabase
import com.example.data.local.entities.AppSettings
import com.example.data.repository.FinanceRepository
import com.example.ui.viewmodel.backup.BackupPayloadBuilder
import com.example.ui.viewmodel.backup.BackupSearchMatcher
import com.example.ui.viewmodel.backup.OAuthCodeParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupSyncViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinanceRepository
    private val backupRestoreMutex = Mutex()
    val showActivationRequired = MutableStateFlow(false)
    val googleDriveSyncHelper: GoogleDriveSyncHelper
    val googleDriveSyncState: StateFlow<CloudSyncState>
    val storedEmailState: StateFlow<String?>

    private val _cloudBackupsList = MutableStateFlow<List<CloudBackupFile>>(emptyList())
    val cloudBackupsList: StateFlow<List<CloudBackupFile>> = _cloudBackupsList.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    val filteredCloudBackups: StateFlow<List<CloudBackupFile>> = combine(
        _cloudBackupsList,
        _searchQuery.debounce(300)
    ) { backups, query ->
        if (query.isBlank()) {
            backups
        } else {
            backups.filter { backup ->
                BackupSearchMatcher.matchesFlexibleQuery(backup.name, query)
            }
        }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isFetchingCloudBackups = MutableStateFlow(false)
    val isFetchingCloudBackups: StateFlow<Boolean> = _isFetchingCloudBackups.asStateFlow()

    private val _localBackups = MutableStateFlow<List<File>>(emptyList())
    val localBackups: StateFlow<List<File>> = _localBackups.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = FinanceRepository(database, application)
        googleDriveSyncHelper = GoogleDriveSyncHelper(application)
        googleDriveSyncState = googleDriveSyncHelper.syncState
        storedEmailState = com.example.domain.GoogleAuthSessionManager.currentEmail
        refreshLocalBackups()
    }

    // إدارة الأدلة والملفات المحلية للنسخ الاحتياطي
    fun getBaseBackupDirectory(): File = repository.getBaseBackupDirectory()
    fun getBackupDirectory(): File = repository.getBackupDirectory()

    fun refreshLocalBackups() {
        viewModelScope.launch(Dispatchers.IO) {
            val baseDir = repository.getBaseBackupDirectory()
            val files = repository.getAllMzdFilesRecursively(baseDir)
            _localBackups.value = files.sortedByDescending { it.lastModified() }
        }
    }

    // مصادقة Google Drive والمزامنة
    fun getClientIdOverride(): String = googleDriveSyncHelper.getClientIdOverride()
    fun getClientSecretOverride(): String = googleDriveSyncHelper.getClientSecretOverride()
    fun getAppSignatureSHA1(): String = googleDriveSyncHelper.getAppSignatureSHA1()

    fun saveClientCredentialsOverride(clientId: String?, clientSecret: String?) {
        googleDriveSyncHelper.saveClientCredentialsOverride(clientId, clientSecret)
    }

    fun handleGoogleOAuthCode(code: String, email: String? = null, redirectUri: String = "", onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val success = googleDriveSyncHelper.handleAuthorizationCode(code, email, redirectUri)
            if (success) {
                val current = repository.getSettingsDirect() ?: AppSettings()
                repository.saveSettings(current.copy(isCloudSyncEnabled = true))
            }
            onComplete?.invoke(success)
        }
    }

    fun handleRawOAuthCodeOrUrl(input: String, email: String? = null, redirectUri: String = "", onComplete: ((Boolean) -> Unit)? = null) {
        val finalCode = OAuthCodeParser.extractCodeFromInput(input)
        if (finalCode.isEmpty()) return
        handleGoogleOAuthCode(finalCode, email, redirectUri, onComplete)
    }

    fun googleDriveLogout(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            val current = repository.getSettingsDirect() ?: AppSettings()
            repository.saveSettings(current.copy(isCloudSyncEnabled = false))
        }
        googleDriveSyncHelper.logoutAsync {
            _cloudBackupsList.value = emptyList()
            onComplete?.invoke()
        }
    }

    fun fetchCloudBackupsList() {
        viewModelScope.launch {
            try {
                _isFetchingCloudBackups.value = true
                val list = googleDriveSyncHelper.listCloudBackups()
                _cloudBackupsList.value = list
                _isFetchingCloudBackups.value = false
            } catch (e: Exception) {
                e.printStackTrace()
                _isFetchingCloudBackups.value = false
            }
        }
    }

    private val backupPrefs by lazy {
        getApplication<Application>().getSharedPreferences("mizan_backup_prefs", Context.MODE_PRIVATE)
    }

    private suspend fun buildBackupJson(isMzd: Boolean, context: Context = getApplication()): String {
        return BackupPayloadBuilder.buildBackupJson(repository, isMzd, context)
    }

    // استخراج بيانات قاعدة البيانات لتصديرها كـ JSON
    fun getBackupJsonForClipboard(onComplete: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonStr = buildBackupJson(isMzd = false)
                launch(Dispatchers.Main) {
                    onComplete(jsonStr)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // عمليات الرفع المباشر والنسخ السحابي والمحلي
    fun uploadBackupToGoogleDrive(onComplete: (Boolean) -> Unit) {
        val sdfName = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US)
        val dateStr = sdfName.format(Date())
        val newFileName = "Mzd_$dateStr.mzd"
        uploadBackupToGoogleDriveWithFilename(newFileName, onComplete)
    }

    fun uploadBackupToGoogleDriveWithFilename(filename: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            backupRestoreMutex.withLock {
                try {
                    val isTrulySignedIn = googleDriveSyncHelper.isUserTrulySignedIn()
                    val refreshToken = googleDriveSyncHelper.getStoredRefreshToken()
                    val isConnected = isTrulySignedIn || !refreshToken.isNullOrEmpty()
                    if (!isConnected) {
                        launch(Dispatchers.Main) {
                            onComplete(false)
                        }
                        return@withLock
                    }

                    val jsonStr = buildBackupJson(isMzd = true)
                    val success = googleDriveSyncHelper.uploadBackupToDriveWithFilename(filename, jsonStr)
                    if (success) {
                        com.example.ui.helper.VibrationHelper.triggerSuccessVibration(getApplication())
                        backupPrefs.edit().putLong("last_successful_backup_timestamp", System.currentTimeMillis()).apply()
                        fetchCloudBackupsList()
                    }
                    launch(Dispatchers.Main) {
                        onComplete(success)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    launch(Dispatchers.Main) {
                        onComplete(false)
                    }
                }
            }
        }
    }

    fun backupToGoogleDriveDirect(onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            backupRestoreMutex.withLock {
                try {
                    val isTrulySignedIn = googleDriveSyncHelper.isUserTrulySignedIn()
                    val refreshToken = googleDriveSyncHelper.getStoredRefreshToken()
                    val isConnected = isTrulySignedIn || !refreshToken.isNullOrEmpty()
                    if (!isConnected) {
                        launch(Dispatchers.Main) {
                            onComplete?.invoke(false)
                        }
                        return@withLock
                    }

                    val jsonStr = buildBackupJson(isMzd = true)
                    val success = googleDriveSyncHelper.uploadBackupToDrive(jsonStr)
                    if (success) {
                        com.example.ui.helper.VibrationHelper.triggerSuccessVibration(getApplication())
                        backupPrefs.edit().putLong("last_successful_backup_timestamp", System.currentTimeMillis()).apply()
                    }
                    launch(Dispatchers.Main) {
                        onComplete?.invoke(success)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    launch(Dispatchers.Main) {
                        onComplete?.invoke(false)
                    }
                }
            }
        }
    }

    fun restoreFromGoogleDriveDirect(context: Context, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            backupRestoreMutex.withLock {
                try {
                    val isTrulySignedIn = googleDriveSyncHelper.isUserTrulySignedIn()
                    val refreshToken = googleDriveSyncHelper.getStoredRefreshToken()
                    val isConnected = isTrulySignedIn || !refreshToken.isNullOrEmpty()
                    if (!isConnected) {
                        launch(Dispatchers.Main) {
                            onComplete(false)
                        }
                        return@withLock
                    }

                    val jsonStr = googleDriveSyncHelper.downloadBackupFromDrive()
                    if (jsonStr != null) {
                        val result = repository.executeMasterRestore(jsonStr)
                        refreshLocalBackups()
                        launch(Dispatchers.Main) {
                            onComplete(true)
                        }
                    } else {
                        launch(Dispatchers.Main) {
                            onComplete(false)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    launch(Dispatchers.Main) {
                        onComplete(false)
                    }
                }
            }
        }
    }

    fun restoreFromGoogleDriveById(context: Context, fileId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            backupRestoreMutex.withLock {
                try {
                    val isTrulySignedIn = googleDriveSyncHelper.isUserTrulySignedIn()
                    val refreshToken = googleDriveSyncHelper.getStoredRefreshToken()
                    val isConnected = isTrulySignedIn || !refreshToken.isNullOrEmpty()
                    if (!isConnected) {
                        launch(Dispatchers.Main) {
                            onComplete(false)
                        }
                        return@withLock
                    }

                    val jsonStr = googleDriveSyncHelper.downloadBackupFromDriveById(fileId)
                    if (jsonStr != null) {
                        val result = repository.executeMasterRestore(jsonStr)
                        refreshLocalBackups()
                        launch(Dispatchers.Main) {
                            onComplete(true)
                        }
                    } else {
                        launch(Dispatchers.Main) {
                            onComplete(false)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    launch(Dispatchers.Main) {
                        onComplete(false)
                    }
                }
            }
        }
    }

    fun deleteCloudBackupById(fileId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            backupRestoreMutex.withLock {
                try {
                    val success = googleDriveSyncHelper.deleteBackupFromDriveById(fileId)
                    if (success) {
                        fetchCloudBackupsList()
                    }
                    launch(Dispatchers.Main) {
                        onComplete(success)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    launch(Dispatchers.Main) {
                        onComplete(false)
                    }
                }
            }
        }
    }

    fun deleteMultipleCloudBackupsByIds(fileIds: List<String>, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            backupRestoreMutex.withLock {
                try {
                    var allSuccess = true
                    for (fileId in fileIds) {
                        val success = googleDriveSyncHelper.deleteBackupFromDriveById(fileId)
                        if (!success) {
                            allSuccess = false
                        }
                    }
                    if (fileIds.isNotEmpty()) {
                        fetchCloudBackupsList()
                    }
                    launch(Dispatchers.Main) {
                        onComplete(allSuccess)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    launch(Dispatchers.Main) {
                        onComplete(false)
                    }
                }
            }
        }
    }

    fun createLocalBackup(context: Context, onComplete: (File?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            backupRestoreMutex.withLock {
                try {
                    val jsonStr = buildBackupJson(isMzd = false, context = context)
                    val dir = getBackupDirectory()
                    val sdfName = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US)
                    val dateStr = sdfName.format(Date())
                    val fileName = "Mizan_$dateStr.mzd"
                    val file = File(dir, fileName)
                    file.writeText(jsonStr)

                    if (file.exists() && file.length() > 0) {
                        com.example.ui.helper.VibrationHelper.triggerSuccessVibration(context)
                        backupPrefs.edit().putLong("last_successful_backup_timestamp", System.currentTimeMillis()).apply()
                        refreshLocalBackups()
                        launch(Dispatchers.Main) {
                            onComplete(file)
                        }
                    } else {
                        throw java.io.IOException("File verification failed.")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    launch(Dispatchers.Main) {
                        onComplete(null)
                    }
                }
            }
        }
    }

    private var lastSilentBackupTime: Long = 0L

    fun triggerSilentLocalBackup() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSilentBackupTime < 600000) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            if (!backupRestoreMutex.tryLock()) {
                return@launch
            }
            try {
                val jsonStr = buildBackupJson(isMzd = false)
                val dir = getBackupDirectory()
                val file = File(dir, "Mizan_Silent_Backup.mzd")
                file.writeText(jsonStr)
                if (file.exists() && file.length() > 0) {
                    lastSilentBackupTime = currentTime
                    refreshLocalBackups()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                backupRestoreMutex.unlock()
            }
        }
    }

    fun clearLocalCopyAndWipeMemory(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            backupRestoreMutex.withLock {
                repository.deleteAllData()
                refreshLocalBackups()
            }
        }
    }

    fun executeMasterRestore(rawJsonString: String, context: Context, onComplete: (Boolean, AppSettings?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            backupRestoreMutex.withLock {
                try {
                    val result = repository.executeMasterRestore(rawJsonString)

                    if (repository.isTrialExpiredDirect()) {
                        showActivationRequired.value = true
                    }

                    val successMessageRes = if (result.isLegacy) com.example.R.string.toast_restore_legacy_migrated else com.example.R.string.cloud_toast_restore_success

                    launch(Dispatchers.Main) {
                        com.example.ui.helper.VibrationHelper.triggerSuccessVibration(context)
                        android.widget.Toast.makeText(context, successMessageRes, android.widget.Toast.LENGTH_SHORT).show()
                        onComplete(true, result.settings)
                    }
                } catch (e: org.json.JSONException) {
                    e.printStackTrace()
                    launch(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, com.example.R.string.backup_schema_mismatch, android.widget.Toast.LENGTH_LONG).show()
                        onComplete(false, null)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    launch(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, com.example.R.string.cloud_toast_restore_failed, android.widget.Toast.LENGTH_LONG).show()
                        onComplete(false, null)
                    }
                }
            }
        }
    }

    fun restoreFromMzdContent(jsonContent: String, context: Context, onComplete: (Boolean) -> Unit) {
        executeMasterRestore(jsonContent, context) { success, _ ->
            onComplete(success)
        }
    }

    fun restoreFromLocalFile(file: File, context: Context, onComplete: (Boolean, AppSettings?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (file.exists()) {
                    val content = file.readText()
                    executeMasterRestore(content, context) { success, restoredSettings ->
                        onComplete(success, restoredSettings)
                    }
                } else {
                    launch(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, com.example.R.string.cloud_toast_restore_failed, android.widget.Toast.LENGTH_SHORT).show()
                        onComplete(false, null)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                launch(Dispatchers.Main) {
                    onComplete(false, null)
                }
            }
        }
    }
}
