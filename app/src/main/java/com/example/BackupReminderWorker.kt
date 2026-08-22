package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import java.util.*
import java.util.concurrent.TimeUnit

class BackupReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "BackupReminderWorker"
        private const val UNIQUE_WORK_NAME = "MizanBackupReminder"

        // Schedule the next reminder
        fun scheduleReminder(context: Context) {
            val workManager = WorkManager.getInstance(context)

            // Calculate delay for tomorrow at a random hour between 2 PM (14:00) and 8 PM (20:00)
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1) // Tomorrow
                val randomHour = (14..20).random()
                val randomMinute = (0..59).random()
                set(Calendar.HOUR_OF_DAY, randomHour)
                set(Calendar.MINUTE, randomMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val delayMs = target.timeInMillis - now.timeInMillis

            val reminderRequest = OneTimeWorkRequestBuilder<BackupReminderWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .build()

            // Keep existing so we don't reschedule on every app launch
            workManager.enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                reminderRequest
            )
            Log.d(TAG, "Scheduled next backup reminder in ${delayMs / 1000 / 60} minutes at ${target.time}")
        }
    }

    override suspend fun doWork(): Result {
        val context = applicationContext
        try {
            // Check if backup reminder is appropriate
            val sharedPrefs = context.getSharedPreferences("mizan_backup_prefs", Context.MODE_PRIVATE)
            val lastBackupTimestamp = sharedPrefs.getLong("last_successful_backup_timestamp", 0L)
            val now = System.currentTimeMillis()

            // Condition: Do not annoy if they backed up in the last 36 hours
            val thirtySixHoursInMillis = 36 * 60 * 60 * 1000L
            if (now - lastBackupTimestamp < thirtySixHoursInMillis && lastBackupTimestamp > 0L) {
                Log.d(TAG, "No reminder needed: User backed up recently (last backup: $lastBackupTimestamp)")
                // Reschedule for tomorrow
                scheduleReminder(context)
                return Result.success()
            }

            // Beautiful, polite and warm randomized messages in Arabic (loaded from resources)
            val reminderMessages = listOf(
                context.getString(R.string.backup_reminder_msg_1),
                context.getString(R.string.backup_reminder_msg_2),
                context.getString(R.string.backup_reminder_msg_3),
                context.getString(R.string.backup_reminder_msg_4),
                context.getString(R.string.backup_reminder_msg_5)
            )
            
            val randomMessage = reminderMessages.random()

            // Send notification
            sendReminderNotification(context, randomMessage)

        } catch (e: Exception) {
            Log.e(TAG, "Error running backup reminder", e)
        } finally {
            // Always schedule the next one for tomorrow
            scheduleReminder(context)
        }
        return Result.success()
    }

    private fun sendReminderNotification(context: Context, message: String) {
        val channelId = "mizan_backup_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                context.getString(com.example.R.string.autobackup_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(com.example.R.string.autobackup_channel_desc)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Clicking the notification opens settings/backup
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // We can pass an extra if we want to navigate directly to Backup Section
            putExtra("navigate_to", "backup_settings")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            2001,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.backup_reminder_title))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1003, notification)
    }
}
