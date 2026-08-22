package com.example.data

import android.content.Context
import android.util.Log
import com.example.data.cloud.CloudNetworkEngine
import com.example.data.serialization.BackupPayloadSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Handles network uploads, updates, downloads, and deletions for Google Drive cloud backups.
 * Features SHA-256 Zero-Diff payload auditing to eliminate redundant network transfers.
 */
class GoogleDriveNetworkUploader(
    private val context: Context,
    private val client: OkHttpClient
) {
    companion object {
        private const val TAG = "GoogleDriveNetworkUploader"

        private const val DRIVE_FILES_BASE_URL = "https://www.googleapis.com/drive/v3/files"
        private const val DRIVE_UPLOAD_BASE_URL = "https://www.googleapis.com/upload/drive/v3/files"
        private const val DEFAULT_ACCOUNT_EMAIL = "account@google.com"
        private const val MIRROR_FILE_NAME = "google_drive_mirror.mzd"
        private const val MIME_TYPE_OCTET_STREAM = "application/octet-stream"

        private val MEDIA_TYPE_JSON = "application/json; charset=utf-8".toMediaType()
        private const val HEADER_AUTHORIZATION = "Authorization"
        private fun bearer(accessToken: String) = "Bearer $accessToken"

        private val DATE_FORMATTER = ThreadLocal.withInitial {
            SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US)
        }

        private fun formatDate(date: Date): String {
            return DATE_FORMATTER.get()?.format(date) ?: ""
        }
    }

    private val cloudEngine = CloudNetworkEngine.getInstance(context)

    private fun createAndUploadNewFile(
        filename: String,
        backupJsonContent: String,
        accessToken: String
    ): Boolean {
        val createMetaUrl = DRIVE_FILES_BASE_URL
        val metaJson = JSONObject()
        metaJson.put("name", filename)
        metaJson.put("parents", org.json.JSONArray().put("appDataFolder"))
        metaJson.put("mimeType", MIME_TYPE_OCTET_STREAM)
        val metaBody = metaJson.toString().toRequestBody(MEDIA_TYPE_JSON)

        val createMetaRequest = Request.Builder()
            .url(createMetaUrl)
            .header(HEADER_AUTHORIZATION, bearer(accessToken))
            .post(metaBody)
            .build()

        return client.newCall(createMetaRequest).execute().use { createMetaResponse ->
            if (createMetaResponse.isSuccessful) {
                val rawBody = createMetaResponse.body?.string() ?: ""
                val createdFile = JSONObject(rawBody)
                val newFileId = createdFile.getString("id")

                val uploadMediaUrl = "$DRIVE_UPLOAD_BASE_URL/$newFileId?uploadType=media"
                val fileBody = backupJsonContent.toRequestBody(MEDIA_TYPE_JSON)

                val uploadMediaRequest = Request.Builder()
                    .url(uploadMediaUrl)
                    .header(HEADER_AUTHORIZATION, bearer(accessToken))
                    .patch(fileBody)
                    .build()

                client.newCall(uploadMediaRequest).execute().use { uploadMediaResponse ->
                    uploadMediaResponse.isSuccessful
                }
            } else {
                Log.e(TAG, "Failed creating upload metadata structure on Drive.")
                false
            }
        }
    }

    suspend fun uploadBackupToDrive(
        backupJsonContent: String,
        accessToken: String,
        folderNavigator: GoogleDriveFolderNavigator,
        updateState: (CloudSyncState) -> Unit,
        onAuthError: suspend () -> Unit,
        email: String?
    ): Boolean = withContext(Dispatchers.IO) {
        // Keep local mirror always matching
        try {
            val mirrorFile = File(context.filesDir, MIRROR_FILE_NAME)
            mirrorFile.bufferedWriter().use { writer ->
                writer.write(backupJsonContent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing local cache mirror file securely", e)
        }

        // SHA-256 Zero-Diff Deduplication Audit
        val currentHash = BackupPayloadSerializer.calculateSha256Hash(backupJsonContent)
        if (cloudEngine.isPayloadIdentical(backupJsonContent)) {
            Log.i(TAG, "Zero-Diff: Backup content hash unchanged ($currentHash). Skipping network upload.")
            updateState(CloudSyncState.Skipped)
            delay(800)
            updateState(CloudSyncState.Authenticated(email ?: DEFAULT_ACCOUNT_EMAIL))
            return@withContext true
        }

        try {
            cloudEngine.executeWithRetry(maxRetries = 3, initialDelayMs = 1000L) {
                // Find latest backup file ID via folder navigator
                val searchResult = folderNavigator.findLatestBackupFileId(accessToken)
                val existingFileId: String?
                when (searchResult) {
                    is GoogleDriveFolderNavigator.FileSearchResult.Success -> {
                        existingFileId = searchResult.fileId
                    }
                    is GoogleDriveFolderNavigator.FileSearchResult.Error -> {
                        if (searchResult.isAuthError) {
                            onAuthError()
                        } else {
                            updateState(CloudSyncState.Error(context.getString(com.example.R.string.gdrive_error_server_failed)))
                        }
                        return@executeWithRetry false
                    }
                }

                val dateStr = formatDate(Date())
                val newFileName = "Mzd_$dateStr.mzd"

                var success = false
                if (existingFileId != null) {
                    // Override/Update existing file
                    val updateUrl = "$DRIVE_UPLOAD_BASE_URL/$existingFileId?uploadType=media"
                    val mediaBody = backupJsonContent.toRequestBody(MEDIA_TYPE_JSON)

                    val updateRequest = Request.Builder()
                        .url(updateUrl)
                        .header(HEADER_AUTHORIZATION, bearer(accessToken))
                        .patch(mediaBody)
                        .build()

                    client.newCall(updateRequest).execute().use { updateResponse ->
                        success = updateResponse.isSuccessful
                        if (!success) {
                            Log.e(TAG, "Failed patching file on Google Drive.")
                        } else {
                            val metaUrl = "$DRIVE_FILES_BASE_URL/$existingFileId"
                            val metaJson = JSONObject()
                            metaJson.put("name", newFileName)
                            val metaBody = metaJson.toString().toRequestBody(MEDIA_TYPE_JSON)
                            val metaRequest = Request.Builder()
                                .url(metaUrl)
                                .header(HEADER_AUTHORIZATION, bearer(accessToken))
                                .patch(metaBody)
                                .build()
                            client.newCall(metaRequest).execute().use { /* automatically closed */ }
                        }
                    }
                } else {
                    // Create new file inside appDataFolder
                    success = createAndUploadNewFile(newFileName, backupJsonContent, accessToken)
                }

                if (success) {
                    cloudEngine.saveLastUploadedPayloadHash(currentHash)
                    updateState(CloudSyncState.Success)
                    delay(1200)
                    updateState(CloudSyncState.Authenticated(email ?: DEFAULT_ACCOUNT_EMAIL))
                    true
                } else {
                    updateState(CloudSyncState.Error(context.getString(com.example.R.string.gdrive_error_server_failed)))
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Uncaught networking error during background cloud synchronization", e)
            updateState(CloudSyncState.Error(context.getString(com.example.R.string.gdrive_error_server_failed)))
            false
        }
    }

    suspend fun downloadBackupFromDrive(
        accessToken: String,
        folderNavigator: GoogleDriveFolderNavigator,
        updateState: (CloudSyncState) -> Unit,
        onAuthError: suspend () -> Unit,
        email: String?
    ): String? = withContext(Dispatchers.IO) {
        try {
            cloudEngine.executeWithRetry(maxRetries = 3, initialDelayMs = 1000L) {
                // Find latest backup file ID via folder navigator
                val searchResult = folderNavigator.findLatestBackupFileId(accessToken)
                val existingFileId: String?
                when (searchResult) {
                    is GoogleDriveFolderNavigator.FileSearchResult.Success -> {
                        existingFileId = searchResult.fileId
                    }
                    is GoogleDriveFolderNavigator.FileSearchResult.Error -> {
                        if (searchResult.isAuthError) {
                            onAuthError()
                        } else {
                            updateState(CloudSyncState.Error(context.getString(com.example.R.string.gdrive_error_server_failed)))
                        }
                        return@executeWithRetry null
                    }
                }

                if (existingFileId == null) {
                    updateState(CloudSyncState.Error(context.getString(com.example.R.string.gdrive_error_backups_not_found)))
                    return@executeWithRetry null
                }

                // Download media payload
                val downloadUrl = "$DRIVE_FILES_BASE_URL/$existingFileId?alt=media"
                val downloadRequest = Request.Builder()
                    .url(downloadUrl)
                    .header(HEADER_AUTHORIZATION, bearer(accessToken))
                    .get()
                    .build()

                client.newCall(downloadRequest).execute().use { downloadResponse ->
                    if (downloadResponse.isSuccessful) {
                        val content = downloadResponse.body?.string()
                        if (content != null && isValidBackupJson(content)) {
                            val downloadedHash = BackupPayloadSerializer.calculateSha256Hash(content)
                            cloudEngine.saveLastUploadedPayloadHash(downloadedHash)
                            updateState(CloudSyncState.Authenticated(email ?: DEFAULT_ACCOUNT_EMAIL))
                            content
                        } else {
                            Log.e(TAG, "Downloaded file failed JSON integrity or schema validation.")
                            updateState(CloudSyncState.Error(context.getString(com.example.R.string.backup_schema_mismatch)))
                            null
                        }
                    } else {
                        if (downloadResponse.code == 401 || downloadResponse.code == 403) {
                            onAuthError()
                        } else {
                            updateState(CloudSyncState.Error(context.getString(com.example.R.string.gdrive_error_server_failed)))
                        }
                        null
                    }
                }
            }
        } catch (e: java.io.IOException) {
            Log.e(TAG, "Network IO error during download processing", e)
            updateState(CloudSyncState.Error(context.getString(com.example.R.string.gdrive_error_network_failed)))
            null
        } catch (e: Exception) {
            Log.e(TAG, "Uncaught error during download processing", e)
            updateState(CloudSyncState.Error(context.getString(com.example.R.string.gdrive_error_server_failed)))
            null
        }
    }

    private fun isValidBackupJson(content: String): Boolean {
        if (content.isBlank()) return false
        return try {
            val json = JSONObject(content)
            val sourceObj = if (json.has("mizan_al_dar_db")) json.getJSONObject("mizan_al_dar_db") else json
            sourceObj.has("settings") || sourceObj.has("transactions") || sourceObj.has("commitments") ||
                    sourceObj.has("fixed_commitments") || sourceObj.has("habayeb_debts") || sourceObj.has("habayeb_debts_db")
        } catch (_: Exception) {
            false
        }
    }

    suspend fun uploadBackupToDriveWithFilename(
        filename: String,
        backupJsonContent: String,
        accessToken: String,
        updateState: (CloudSyncState) -> Unit,
        email: String?
    ): Boolean = withContext(Dispatchers.IO) {
        val currentHash = BackupPayloadSerializer.calculateSha256Hash(backupJsonContent)
        if (cloudEngine.isPayloadIdentical(backupJsonContent)) {
            Log.i(TAG, "Zero-Diff: Custom named backup unchanged ($currentHash). Skipping upload.")
            updateState(CloudSyncState.Skipped)
            delay(800)
            updateState(CloudSyncState.Authenticated(email ?: DEFAULT_ACCOUNT_EMAIL))
            return@withContext true
        }

        try {
            cloudEngine.executeWithRetry(maxRetries = 3, initialDelayMs = 1000L) {
                val success = createAndUploadNewFile(filename, backupJsonContent, accessToken)

                if (success) {
                    cloudEngine.saveLastUploadedPayloadHash(currentHash)
                    updateState(CloudSyncState.Success)
                    delay(1200)
                    updateState(CloudSyncState.Authenticated(email ?: DEFAULT_ACCOUNT_EMAIL))
                    true
                } else {
                    updateState(CloudSyncState.Error(context.getString(com.example.R.string.gdrive_error_server_failed)))
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing file creation with specialized filename on Drive", e)
            updateState(CloudSyncState.Error(context.getString(com.example.R.string.gdrive_error_server_failed)))
            false
        }
    }

    suspend fun downloadBackupFromDriveById(
        fileId: String,
        accessToken: String,
        updateState: (CloudSyncState) -> Unit,
        onAuthError: suspend () -> Unit,
        email: String?
    ): String? = withContext(Dispatchers.IO) {
        try {
            cloudEngine.executeWithRetry(maxRetries = 3, initialDelayMs = 1000L) {
                val downloadUrl = "$DRIVE_FILES_BASE_URL/$fileId?alt=media"
                val downloadRequest = Request.Builder()
                    .url(downloadUrl)
                    .header(HEADER_AUTHORIZATION, bearer(accessToken))
                    .get()
                    .build()

                client.newCall(downloadRequest).execute().use { downloadResponse ->
                    if (downloadResponse.isSuccessful) {
                        val content = downloadResponse.body?.string()
                        if (content != null && isValidBackupJson(content)) {
                            val downloadedHash = BackupPayloadSerializer.calculateSha256Hash(content)
                            cloudEngine.saveLastUploadedPayloadHash(downloadedHash)
                            updateState(CloudSyncState.Authenticated(email ?: DEFAULT_ACCOUNT_EMAIL))
                            content
                        } else {
                            Log.e(TAG, "Downloaded backup by ID failed integrity or schema validation.")
                            updateState(CloudSyncState.Error(context.getString(com.example.R.string.backup_schema_mismatch)))
                            null
                        }
                    } else {
                        if (downloadResponse.code == 401 || downloadResponse.code == 403) {
                            onAuthError()
                        } else {
                            updateState(CloudSyncState.Error(context.getString(com.example.R.string.gdrive_error_server_failed)))
                        }
                        null
                    }
                }
            }
        } catch (e: java.io.IOException) {
            Log.e(TAG, "Network IO error downloading file by ID", e)
            updateState(CloudSyncState.Error(context.getString(com.example.R.string.gdrive_error_network_failed)))
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading custom file content by database ID", e)
            updateState(CloudSyncState.Error(context.getString(com.example.R.string.gdrive_error_server_failed)))
            null
        }
    }

    suspend fun deleteBackupFromDriveById(fileId: String, accessToken: String): Boolean = withContext(Dispatchers.IO) {
        try {
            cloudEngine.executeWithRetry(maxRetries = 2, initialDelayMs = 500L) {
                val url = "$DRIVE_FILES_BASE_URL/$fileId"
                val request = Request.Builder()
                    .url(url)
                    .header(HEADER_AUTHORIZATION, bearer(accessToken))
                    .delete()
                    .build()

                client.newCall(request).execute().use { response ->
                    response.isSuccessful
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing file from remote drive folder", e)
            false
        }
    }
}
