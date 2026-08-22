package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.local.entities.AppSettings
import com.example.data.local.entities.CustomCategory
import com.example.data.local.entities.DeletedItemEntity
import com.example.data.local.entities.FixedCommitment
import com.example.data.local.entities.HabayebCustomer
import com.example.data.local.entities.HabayebTransaction
import com.example.data.local.entities.TransactionDb

import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AppSettings::class,
        FixedCommitment::class,
        TransactionDb::class,
        CustomCategory::class,
        DeletedItemEntity::class,
        HabayebCustomer::class,
        HabayebTransaction::class
    ],
    version = 31,
    exportSchema = false
)
@TypeConverters(BigDecimalConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao
    abstract fun commitmentDao(): CommitmentDao
    abstract fun transactionDao(): TransactionDao
    abstract fun customCategoryDao(): CustomCategoryDao
    abstract fun trashDao(): TrashDao
    abstract fun habayebDao(): HabayebDao

    companion object {
        const val DATABASE_NAME = "mizan_al_dar_db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .addMigrations(*DatabaseMigrations.ALL_MIGRATIONS)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        try {
                            db.execSQL("""
                                UPDATE habayeb_transactions 
                                SET linkedMainTxId = NULL 
                                WHERE linkedMainTxId IS NOT NULL 
                                  AND (
                                      TRIM(linkedMainTxId) = '' 
                                      OR LOWER(TRIM(linkedMainTxId)) = 'null' 
                                      OR TRIM(linkedMainTxId) = '0' 
                                      OR linkedMainTxId = id
                                      OR NOT EXISTS (
                                          SELECT 1 FROM habayeb_transactions parent
                                          WHERE parent.id = TRIM(linkedMainTxId)
                                      )
                                  )
                            """)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                })
                .build().also { INSTANCE = it }
            }
        }
    }
}
