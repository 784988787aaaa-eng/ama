package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

class GoogleDriveFolderNavigator(private val client: OkHttpClient) {

    companion object {
        private const val TAG = "GoogleDriveFolderNavigator"

        private const val DRIVE_FILES_API_URL = "https://www.googleapis.com/drive/v3/files"
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
        private const val ENCODING_UTF8 = "UTF-8"

        private const val SPACE_APP_DATA_FOLDER = "appDataFolder"
        private const val QUERY_LATEST_BACKUP = "name contains 'Mzd_' and name contains '.mzd' and trashed = false"
        private const val QUERY_ALL_MZD_BACKUPS = "name contains '.mzd' and trashed = false"
        private const val ORDER_BY_CREATED_TIME_DESC = "createdTime desc"
        private const val FIELDS_BACKUPS_LIST = "files(id,name,size,createdTime)"

        private const val JSON_KEY_FILES = "files"
        private const val JSON_KEY_ID = "id"
        private const val JSON_KEY_NAME = "name"
        private const val JSON_KEY_SIZE = "size"
        private const val JSON_KEY_CREATED_TIME = "createdTime"
    }

    sealed class FileSearchResult {
        data class Success(val fileId: String?) : FileSearchResult()
        data class Error(val isAuthError: Boolean, val code: Int) : FileSearchResult()
    }

    sealed class ListBackupsResult {
        data class Success(val backups: List<CloudBackupFile>) : ListBackupsResult()
        data class Error(val isAuthError: Boolean, val code: Int) : ListBackupsResult()
    }

    private fun buildAuthorizedRequest(url: String, accessToken: String): Request {
        return Request.Builder()
            .url(url)
            .header(HEADER_AUTHORIZATION, "$BEARER_PREFIX$accessToken")
            .get()
            .build()
    }

    private fun isAuthError(code: Int): Boolean = code == 401 || code == 403

    /**
     * Searches inside appDataFolder for the latest .mzd file matching name contains 'Mzd_' and contains '.mzd'
     */
    suspend fun findLatestBackupFileId(accessToken: String): FileSearchResult = withContext(Dispatchers.IO) {
        try {
            val searchUrl = "$DRIVE_FILES_API_URL?spaces=$SPACE_APP_DATA_FOLDER" +
                    "&orderBy=${URLEncoder.encode(ORDER_BY_CREATED_TIME_DESC, ENCODING_UTF8)}" +
                    "&q=${URLEncoder.encode(QUERY_LATEST_BACKUP, ENCODING_UTF8)}"

            val searchRequest = buildAuthorizedRequest(searchUrl, accessToken)

            client.newCall(searchRequest).execute().use { searchResponse ->
                if (searchResponse.isSuccessful) {
                    val rawBody = searchResponse.body?.string() ?: ""
                    val searchResult = JSONObject(rawBody)
                    val files = searchResult.optJSONArray(JSON_KEY_FILES)
                    val existingFileId = if (files != null && files.length() > 0) {
                        files.getJSONObject(0).getString(JSON_KEY_ID)
                    } else {
                        null
                    }
                    FileSearchResult.Success(existingFileId)
                } else {
                    FileSearchResult.Error(isAuthError(searchResponse.code), searchResponse.code)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing file search on Drive", e)
            FileSearchResult.Error(false, -1)
        }
    }

    /**
     * Lists all .mzd backups in Google Drive inside appDataFolder
     */
    suspend fun listCloudBackups(accessToken: String): ListBackupsResult = withContext(Dispatchers.IO) {
        try {
            val url = "$DRIVE_FILES_API_URL?spaces=$SPACE_APP_DATA_FOLDER" +
                    "&fields=${URLEncoder.encode(FIELDS_BACKUPS_LIST, ENCODING_UTF8)}" +
                    "&q=${URLEncoder.encode(QUERY_ALL_MZD_BACKUPS, ENCODING_UTF8)}" +
                    "&orderBy=${URLEncoder.encode(ORDER_BY_CREATED_TIME_DESC, ENCODING_UTF8)}" +
                    "&pageSize=1000"

            val request = buildAuthorizedRequest(url, accessToken)

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val rawBody = response.body?.string() ?: ""
                    val json = JSONObject(rawBody)
                    val filesArray = json.optJSONArray(JSON_KEY_FILES) ?: return@use ListBackupsResult.Success(emptyList())
                    val list = mutableListOf<CloudBackupFile>()
                    for (i in 0 until filesArray.length()) {
                        val obj = filesArray.getJSONObject(i)
                        val id = obj.getString(JSON_KEY_ID)
                        val name = obj.getString(JSON_KEY_NAME)
                        val size = obj.optLong(JSON_KEY_SIZE, 0L)
                        val createdTime = obj.optString(JSON_KEY_CREATED_TIME, "")
                        list.add(CloudBackupFile(id, name, size, createdTime))
                    }
                    ListBackupsResult.Success(list.sortedByDescending { it.name })
                } else {
                    ListBackupsResult.Error(isAuthError(response.code), response.code)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing remote cloud backups", e)
            ListBackupsResult.Error(false, -1)
        }
    }
}
