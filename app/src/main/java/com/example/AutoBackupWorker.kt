package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Constraints
import androidx.work.NetworkType
import com.example.data.local.AppDatabase
import com.example.data.local.entities.AppSettings
import com.example.data.GoogleDriveSyncHelper
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AutoBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "AutoBackupWorker"
        const val WORK_NAME = "MizanDailyBackup"
        const val PREFS_NAME = "mizan_backup_prefs"
        const val CHANNEL_ID = "mizan_backup_channel"

        fun scheduleDailyBackupWorker(context: Context) {
            val workManager = WorkManager.getInstance(context)
            
            // Calculate initial delay to align with 23:59:00 PM (11:59 PM)
            val currentDate = Calendar.getInstance()
            val dueDate = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (dueDate.before(currentDate)) {
                dueDate.add(Calendar.DAY_OF_YEAR, 1)
            }
            val initialDelay = dueDate.timeInMillis - currentDate.timeInMillis

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(true)                // Save device battery health
                .build()

            val dailyWorkRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(
                1, TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.MINUTES
                )
                .build()

            // Use ExistingPeriodicWorkPolicy.KEEP so we don't reset the initial delay on every app open
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                dailyWorkRequest
            )
        }
        
        fun cancelDailyBackupWorker(context: Context) {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork(WORK_NAME)
        }

        suspend fun checkAndTriggerBackupIfMissed(context: Context) {
            try {
                val db = AppDatabase.getDatabase(context)
                val settings = db.settingsDao().getSettingsDirect() ?: AppSettings()
                if (!settings.isAutoBackupEnabled) {
                    Log.d(TAG, "Auto-backup check on startup: Disabled in settings.")
                    return
                }

                val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                
                // Initialize the timestamp on first run so we don't think we missed a backup on very first install
                val isFirstLaunch = sharedPrefs.getBoolean("is_first_backup_initialized", true)
                if (isFirstLaunch) {
                    sharedPrefs.edit()
                        .putLong("last_successful_auto_backup_timestamp", System.currentTimeMillis())
                        .putLong("last_successful_backup_timestamp", System.currentTimeMillis())
                        .putBoolean("is_first_backup_initialized", false)
                        .apply()
                    Log.d(TAG, "First backup initialized on start. Skipping startup backup.")
                    return
                }

                val now = Calendar.getInstance()
                val lastDueBackup = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (now.before(lastDueBackup)) {
                    lastDueBackup.add(Calendar.DAY_OF_YEAR, -1)
                }

                val lastBackupTimestamp = sharedPrefs.getLong("last_successful_backup_timestamp", 0L)
                val isPendingCloud = sharedPrefs.getBoolean("pending_cloud_upload", false)

                val syncHelper = GoogleDriveSyncHelper(context)
                val isCloudLinked = !syncHelper.getStoredRefreshToken().isNullOrEmpty()

                if (lastBackupTimestamp < lastDueBackup.timeInMillis) {
                    Log.d(TAG, "Last backup was missed! Last success: $lastBackupTimestamp, Last due: ${lastDueBackup.timeInMillis}. Triggering immediate backup.")
                    // Trigger immediate background backup using OneTimeWorkRequest
                    val immediateWorkRequest = androidx.work.OneTimeWorkRequestBuilder<AutoBackupWorker>()
                        .setBackoffCriteria(
                            androidx.work.BackoffPolicy.EXPONENTIAL,
                            10,
                            TimeUnit.MINUTES
                        )
                        .build()
                    WorkManager.getInstance(context).enqueue(immediateWorkRequest)

                    // Also schedule cloud upload on first internet connection if Google Drive is linked
                    if (isCloudLinked) {
                        CloudUploadWorker.enqueueUploadLatest(context)
                    }
                } else if (isCloudLinked && isPendingCloud) {
                    Log.d(TAG, "Local backup is up-to-date, but cloud upload is pending. Enqueueing CloudUploadWorker with CONNECTED constraint.")
                    CloudUploadWorker.enqueueUploadLatest(context)
                } else {
                    Log.d(TAG, "Backup is up to date. Last success: $lastBackupTimestamp, Last due: ${lastDueBackup.timeInMillis}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in checkAndTriggerBackupIfMissed", e)
            }
        }
    }

    override suspend fun doWork(): Result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val context = applicationContext
        try {
            val db = AppDatabase.getDatabase(context)
            val settings = db.settingsDao().getSettingsDirect() ?: AppSettings()
            
            if (!settings.isAutoBackupEnabled) {
                Log.d(TAG, "Auto-backup feature is disabled in user settings.")
                return@withContext Result.success()
            }
            
            // Send in-progress notification
            sendBackupInProgressNotification(context)
            
            val commitments = db.commitmentDao().getAllCommitmentsFlow().first()
            val transactions = db.transactionDao().getAllTransactionsFlow().first()
            val deletedItems = db.trashDao().getAllDeletedItemsDirect()

            val habayebCustomers = db.habayebDao().getAllCustomersDirect()
            val habayebTransactions = db.habayebDao().getAllTransactionsDirect()

            val jsonStr = com.example.data.serialization.BackupPayloadSerializer.exportBackupToJson(
                settings = settings,
                commitments = commitments,
                transactions = transactions,
                habayebCustomers = habayebCustomers,
                habayebTransactions = habayebTransactions,
                deletedItems = deletedItems,
                context = applicationContext
            )

            val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.US)
            val monthStr = sdfMonth.format(Date())
            val appName = applicationContext.getString(com.example.R.string.app_name)
            
            val sdfName = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US)
            val dateStr = sdfName.format(Date())
            val fileName = "Mzd_$dateStr.mzd"

            // 1. Try public Documents directory
            val documentsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS)
            val publicMainDir = File(documentsDir, appName)
            var publicFile: File? = null
            var publicWritten = false
            try {
                if (!publicMainDir.exists()) publicMainDir.mkdirs()
                val targetDir = File(publicMainDir, monthStr)
                if (!targetDir.exists()) targetDir.mkdirs()
                
                val file = File(targetDir, fileName)
                file.bufferedWriter().use { writer ->
                    writer.write(jsonStr)
                }
                if (file.exists() && file.length() > 0) {
                    publicFile = file
                    publicWritten = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write to public Documents folder", e)
            }

            // 2. Also try app's private files directory for guaranteed execution
            val privateMainDir = File(context.getExternalFilesDir(null) ?: context.filesDir, appName)
            var privateFile: File? = null
            var privateWritten = false
            try {
                if (!privateMainDir.exists()) privateMainDir.mkdirs()
                val targetDir = File(privateMainDir, monthStr)
                if (!targetDir.exists()) targetDir.mkdirs()
                
                val file = File(targetDir, fileName)
                file.bufferedWriter().use { writer ->
                    writer.write(jsonStr)
                }
                if (file.exists() && file.length() > 0) {
                    privateFile = file
                    privateWritten = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write to private files folder", e)
            }

            val localWrittenSuccessfully = publicWritten || privateWritten
            val activeBackupFile = publicFile ?: privateFile

            if (localWrittenSuccessfully && activeBackupFile != null) {
                // Save successful auto-backup timestamp to SharedPreferences
                val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                sharedPrefs.edit()
                    .putLong("last_successful_auto_backup_timestamp", System.currentTimeMillis())
                    .putLong("last_successful_backup_timestamp", System.currentTimeMillis())
                    .apply()

                // Dual Cloud Sync
                val syncHelper = GoogleDriveSyncHelper(context)
                val isLinked = !syncHelper.getStoredRefreshToken().isNullOrEmpty()
                var cloudSynced = false
                
                if (isLinked) {
                    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                    val hasNetwork = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val nw = connectivityManager.activeNetwork
                        val actNw = connectivityManager.getNetworkCapabilities(nw)
                        actNw != null && (actNw.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
                                actNw.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED))
                    } else {
                        @Suppress("DEPRECATION")
                        val nwInfo = connectivityManager.activeNetworkInfo
                        @Suppress("DEPRECATION")
                        nwInfo != null && nwInfo.isConnected
                    }

                    if (hasNetwork) {
                        cloudSynced = syncHelper.uploadBackupToDriveWithFilename(fileName, jsonStr)
                    }
                    
                    if (!cloudSynced) {
                        Log.w(TAG, "Google Cloud synchronization failed or offline. Enqueueing background CloudUploadWorker.")
                        sharedPrefs.edit().putBoolean("pending_cloud_upload", true).apply()
                        CloudUploadWorker.enqueueUpload(context, activeBackupFile.absolutePath, fileName)
                    } else {
                        sharedPrefs.edit().putBoolean("pending_cloud_upload", false).apply()
                    }
                }
                
                // Formulate beautiful readable path
                val displayedPath = if (publicWritten) {
                    "Documents/$appName/$monthStr/$fileName"
                } else {
                    "Android/data/${context.packageName}/files/$appName/$monthStr/$fileName"
                }
                
                // Trigger success vibration
                com.example.ui.helper.VibrationHelper.triggerSuccessVibration(context)

                // Absolutely last step: send success notification with exact folder path!
                sendBackupNotification(context, displayedPath, cloudSynced)
                Result.success()
            } else {
                Log.e(TAG, "Local file write verification failed.")
                sendBackupFailureNotification(context, false)
                Result.retry()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Defensive rescue: unexpected background execution error in AutoBackupWorker", e)
            sendBackupFailureNotification(context, false)
            
            // Check if failure is related to network or file system IO
            if (e is IOException) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private fun sendBackupInProgressNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(com.example.R.string.autobackup_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(com.example.R.string.autobackup_channel_desc)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val title = context.getString(com.example.R.string.autobackup_notification_title_inprogress)
        val text = context.getString(com.example.R.string.autobackup_notification_text_inprogress)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        notificationManager.notify(1001, notification)
    }

    private fun sendBackupNotification(context: Context, folderPath: String, cloudSynced: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(com.example.R.string.autobackup_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(com.example.R.string.autobackup_channel_desc)
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

        val title = context.getString(com.example.R.string.autobackup_notification_title_success)
        val text = context.getString(com.example.R.string.autobackup_notification_text_success)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }

    private fun sendBackupFailureNotification(context: Context, isPermissionIssue: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(com.example.R.string.autobackup_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(com.example.R.string.autobackup_channel_desc)
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

        val title = context.getString(com.example.R.string.autobackup_notification_title_failure)
        val text = if (isPermissionIssue) {
            context.getString(com.example.R.string.autobackup_notification_text_permission)
        } else {
            context.getString(com.example.R.string.autobackup_notification_text_failure)
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}
