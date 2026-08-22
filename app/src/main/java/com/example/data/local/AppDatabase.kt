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

/**
 * قاعدة البيانات الرئيسية لميزان الدار (Mizan Al-Dar Room AppDatabase)
 *
 * المبادئ والمعايير المعمارية الحاكمة:
 * 1. تاريخ المستخدمين غير قابل للمس: مخطط قاعدة البيانات (Schema) والهجرات المنشورة (Migrations 1 to 31) تمثل تاريخاً حقيقياً وموثوقاً لبيانات المستخدمين على أجهزتهم؛ لذا يُمنع منعاً باتاً تعديل أي Migration سابقة أو حذف أي جدول/عمود تاريخي.
 * 2. وضع الكتابة المسبقة للتدوين (WAL Mode): يتم تفعيل JournalMode.WRITE_AHEAD_LOGGING لتسريع عمليات القراءة المتزامنة دون حظر خيوط الكتابة.
 * 3. صيانة العلاقات والروابط المعلقة (Consistency Callbacks): عند فتح قاعدة البيانات يتم تنظيف روابط المعاملات غير الصالحة لضمان اتساق دفتر اليومية مع الحبايب دون التأثير على السجلات الأصلية.
 */
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
