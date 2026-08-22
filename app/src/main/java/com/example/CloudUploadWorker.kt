package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.GoogleDriveSyncHelper
import java.io.File

class CloudUploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "CloudUploadWorker"
        const val WORK_NAME = "MizanDelayedCloudUpload"
        const val KEY_FILE_PATH = "backup_file_path"
        const val KEY_FILE_NAME = "backup_file_name"

        fun enqueueUpload(context: Context, filePath: String, fileName: String) {
            val sharedPrefs = context.getSharedPreferences(AutoBackupWorker.PREFS_NAME, Context.MODE_PRIVATE)
            sharedPrefs.edit()
                .putBoolean("pending_cloud_upload", true)
                .putString("pending_cloud_file_path", filePath)
                .putString("pending_cloud_file_name", fileName)
                .apply()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val data = Data.Builder()
                .putString(KEY_FILE_PATH, filePath)
                .putString(KEY_FILE_NAME, fileName)
                .build()

            val uploadWorkRequest = OneTimeWorkRequestBuilder<CloudUploadWorker>()
                .setConstraints(constraints)
                .setInputData(data)
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    5,
                    java.util.concurrent.TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                uploadWorkRequest
            )
            Log.d(TAG, "Enqueued cloud upload for $fileName to trigger on internet connection")
        }

        fun enqueueUploadLatest(context: Context) {
            val sharedPrefs = context.getSharedPreferences(AutoBackupWorker.PREFS_NAME, Context.MODE_PRIVATE)
            val path = sharedPrefs.getString("pending_cloud_file_path", null)
            val name = sharedPrefs.getString("pending_cloud_file_name", null)

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val dataBuilder = Data.Builder()
            if (!path.isNullOrEmpty()) dataBuilder.putString(KEY_FILE_PATH, path)
            if (!name.isNullOrEmpty()) dataBuilder.putString(KEY_FILE_NAME, name)

            val uploadWorkRequest = OneTimeWorkRequestBuilder<CloudUploadWorker>()
                .setConstraints(constraints)
                .setInputData(dataBuilder.build())
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    5,
                    java.util.concurrent.TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                uploadWorkRequest
            )
            Log.d(TAG, "Enqueued latest cloud upload to trigger on internet connection")
        }
    }

    override suspend fun doWork(): Result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val context = applicationContext
        val sharedPrefs = context.getSharedPreferences(AutoBackupWorker.PREFS_NAME, Context.MODE_PRIVATE)

        var filePath = inputData.getString(KEY_FILE_PATH)
            ?: sharedPrefs.getString("pending_cloud_file_path", null)
        var fileName = inputData.getString(KEY_FILE_NAME)
            ?: sharedPrefs.getString("pending_cloud_file_name", null)

        try {
            var file: File? = if (!filePath.isNullOrEmpty()) File(filePath) else null

            // If target file doesn't exist or wasn't provided, search for the latest .mzd file in backup folders
            if (file == null || !file.exists()) {
                val appName = context.getString(R.string.app_name)
                val documentsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS)
                val publicMainDir = File(documentsDir, appName)
                val privateMainDir = File(context.getExternalFilesDir(null) ?: context.filesDir, appName)

                val candidates = mutableListOf<File>()
                fun scanDir(dir: File) {
                    if (dir.exists() && dir.isDirectory) {
                        dir.walkTopDown().forEach { f ->
                            if (f.isFile && f.extension == "mzd") {
                                candidates.add(f)
                            }
                        }
                    }
                }
                scanDir(publicMainDir)
                scanDir(privateMainDir)

                val latestFile = candidates.maxByOrNull { it.lastModified() }
                if (latestFile != null) {
                    file = latestFile
                    filePath = latestFile.absolutePath
                    fileName = latestFile.name
                }
            }

            if (file == null || !file.exists()) {
                Log.e(TAG, "No backup file found to upload to cloud.")
                sharedPrefs.edit().putBoolean("pending_cloud_upload", false).apply()
                return@withContext Result.failure()
            }

            val syncHelper = GoogleDriveSyncHelper(context)
            val isLinked = !syncHelper.getStoredRefreshToken().isNullOrEmpty()
            if (!isLinked) {
                Log.d(TAG, "Google Drive not linked. Skipping cloud upload.")
                sharedPrefs.edit().putBoolean("pending_cloud_upload", false).apply()
                return@withContext Result.success()
            }

            val jsonStr = file.readText()
            val uploadName = fileName ?: file.name
            val success = syncHelper.uploadBackupToDriveWithFilename(uploadName, jsonStr)
            if (success) {
                Log.d(TAG, "Successfully uploaded backup $uploadName to Google Drive on internet connection")
                sharedPrefs.edit()
                    .putBoolean("pending_cloud_upload", false)
                    .putLong("last_successful_cloud_backup_timestamp", System.currentTimeMillis())
                    .apply()

                com.example.ui.helper.VibrationHelper.triggerSuccessVibration(context)
                sendDelayedUploadNotification(context, uploadName)
                Result.success()
            } else {
                Log.e(TAG, "Cloud upload failed, retrying on network...")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during cloud upload", e)
            Result.retry()
        }
    }

    private fun sendDelayedUploadNotification(context: Context, fileName: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                AutoBackupWorker.CHANNEL_ID,
                context.getString(R.string.autobackup_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.autobackup_channel_desc)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 100, 50, 150)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.autobackup_notification_title_cloud_delayed)
        val text = context.getString(R.string.autobackup_notification_text_cloud_delayed)

        val notification = NotificationCompat.Builder(context, AutoBackupWorker.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1003, notification)
    }
}
