package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.navigation.Screen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

private val Context.navigationDataStore: DataStore<Preferences> by preferencesDataStore(name = "navigation_prefs")

/**
 * تفضيلات التنقل والشاشة الافتراضية للتطبيق.
 * تم تثبيت الشاشة الافتراضية حتمياً على شاشة "حبايب" (HABAYEB) مع تأمين نطاق DataStore.
 */
class NavigationPreferences(private val context: Context) {
    companion object {
        private val KEY_DEFAULT_START = stringPreferencesKey("default_start")
        
        val DEFAULT_START = Screen.HABAYEB.name
        val DEFAULT_ORDER = "${Screen.HABAYEB.name},${Screen.LEDGER.name}"
    }

    val tabOrderFlow: Flow<String> = flowOf(DEFAULT_ORDER)

    val defaultStartFlow: Flow<String> = flowOf(DEFAULT_START)

    @Deprecated("لم تعد التفضيلات متغيرة، التطبيق يفتح حتمياً على شاشة حبايب")
    @Suppress("UNUSED_PARAMETER")
    suspend fun saveDefaultStart(start: String) {
        context.navigationDataStore.edit { preferences ->
            preferences[KEY_DEFAULT_START] = DEFAULT_START
        }
    }

    @Deprecated("لم تعد التفضيلات متغيرة، التطبيق يفتح حتمياً على شاشة حبايب")
    @Suppress("UNUSED_PARAMETER")
    suspend fun saveTabOrder(order: String) {}
}


