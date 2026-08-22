package com.example.data.local

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.local.entities.AppSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseInstrumentationTest {
    private var database: AppDatabase? = null

    @After
    fun closeDatabase() {
        database?.close()
    }

    @Test
    fun currentSchemaOpensAndPersistsSettings() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.deleteDatabase(AppDatabase.DATABASE_NAME)
        database = AppDatabase.getDatabase(context)

        runBlocking { database!!.settingsDao().insertOrUpdateSettings(AppSettings(currencySymbol = "ر.ي")) }
        val settings = runBlocking { database!!.settingsDao().getSettingsDirect() }

        assertEquals("ر.ي", settings?.currencySymbol)
    }

    @Test
    fun currentSchemaContainsExpectedFinancialTables() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.deleteDatabase(AppDatabase.DATABASE_NAME)
        database = AppDatabase.getDatabase(context)

        database!!.openHelper.readableDatabase.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name IN ('transactions','habayeb_customers','habayeb_transactions','custom_categories','deleted_items')"
        ).use { cursor ->
            val names = mutableSetOf<String>()
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) names += cursor.getString(nameIndex)
            assertTrue(names.containsAll(setOf("transactions", "habayeb_customers", "habayeb_transactions", "custom_categories", "deleted_items")))
        }
    }
}
