package com.example

import android.app.Application
import androidx.work.Configuration
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FinanceApplication : Application(), Configuration.Provider {
    override fun onCreate() {
        super.onCreate()
        // Perform all background initializations asynchronously to keep the main thread 100% unblocked (<400ms Cold Startup)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Initialize unified Google Auth session manager asynchronously
                com.example.domain.GoogleAuthSessionManager.initialize(this@FinanceApplication)
                
                // Pre-warm the database early in background to ensure migrations and indexes are primed
                val db = AppDatabase.getDatabase(applicationContext)
                db.settingsDao().getSettingsDirect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
