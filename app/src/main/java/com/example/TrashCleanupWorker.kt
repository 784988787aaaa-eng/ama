package com.example

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Constraints
import com.example.data.local.AppDatabase
import java.util.concurrent.TimeUnit

class TrashCleanupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting trash auto-cleanup in background...")
        try {
            val sharedPrefs = applicationContext.getSharedPreferences("trash_prefs", Context.MODE_PRIVATE)
            val period = sharedPrefs.getString("trash_auto_cleanup_period", "never") ?: "never"
            if (period == "never") {
                Log.d(TAG, "Auto-cleanup is disabled (never).")
                return Result.success()
            }

            val ageInMillis = getPeriodDurationMillis(period)
            if (ageInMillis <= 0L) {
                return Result.success()
            }

            val thresholdTime = System.currentTimeMillis() - ageInMillis
            val db = AppDatabase.getDatabase(applicationContext)
            val trashDao = db.trashDao()

            val items = trashDao.getAllDeletedItemsDirect()
            val expiredItems = items.filter { it.deletedAt < thresholdTime }

            Log.d(TAG, "Found ${expiredItems.size} expired items out of ${items.size} total items (threshold: $thresholdTime).")

            expiredItems.forEach { item ->
                trashDao.deleteItem(item)
                Log.d(TAG, "Deleted expired item: ${item.id}")
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error running trash cleanup worker", e)
            return Result.retry()
        }
    }

    companion object {
        private const val TAG = "TrashCleanupWorker"
        const val WORK_NAME = "MizanTrashCleanup"

        fun getPeriodDurationMillis(period: String): Long {
            return when (period) {
                "week" -> 7 * 24 * 60 * 60 * 1000L
                "month" -> 30 * 24 * 60 * 60 * 1000L
                "3months" -> 90L * 24 * 60 * 60 * 1000L
                "6months" -> 180L * 24 * 60 * 60 * 1000L
                "year" -> 365L * 24 * 60 * 60 * 1000L
                else -> 0L
            }
        }

        fun schedulePeriodicCleanup(context: Context, period: String) {
            val workManager = WorkManager.getInstance(context)
            if (period == "never") {
                workManager.cancelUniqueWork(WORK_NAME)
                Log.d(TAG, "Cancelled periodic trash cleanup worker.")
                return
            }

            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<TrashCleanupWorker>(
                1, TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                periodicRequest
            )
            Log.d(TAG, "Scheduled daily trash cleanup worker for period: $period")
        }
    }
}
