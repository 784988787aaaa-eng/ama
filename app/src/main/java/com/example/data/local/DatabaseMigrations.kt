package com.example.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * سجل هجرات وتحديثات هيكل قاعدة البيانات ميزان الدار (Immutable Database Schema Migrations)
 * 
 * تحذير معماري حاسم:
 * هذا الملف يمثل سجلاً تاريخياً ثابتاً وغير قابل للتعديل (Immutable History) لجميع هياكل قاعدة البيانات المنشورة للمستخدمين.
 * - يُمنع منعاً باتاً حذف أو تعديل أو إعادة ترقيم أو دمج أي Migration قديمة.
 * - أي تغيير في بنية الجداول المستقبلية يجب أن يُضاف كـ Migration جديدة ذات رقم تزايدي متسلسل.
 * - جميع عمليات SQL المكتوبة هنا تحافظ على التوافق التام مع بيانات المستخدمين التاريخية دون أي فقدان.
 */
object DatabaseMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Safeguard: Empty path to support legacy version 1 installs transitioning to version 2
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE fixed_commitments ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE app_settings ADD COLUMN isPasscodeEnabled INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE app_settings ADD COLUMN passcodeHash TEXT")
            db.execSQL("ALTER TABLE app_settings ADD COLUMN recoveryPhraseHash TEXT")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE app_settings ADD COLUMN recoveryHint TEXT")
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `makhzan_products` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `name` TEXT NOT NULL, 
                    `category` TEXT NOT NULL, 
                    `purchasePrice` REAL NOT NULL, 
                    `sellingPrice` REAL NOT NULL, 
                    `quantity` INTEGER NOT NULL, 
                    `imageUrl` TEXT, 
                    `lowStockThreshold` INTEGER NOT NULL DEFAULT 5
                )
            """)
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `makhzan_products_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `name` TEXT NOT NULL, 
                    `category` TEXT NOT NULL, 
                    `purchasePrice` REAL NOT NULL, 
                    `sellingPrice` REAL NOT NULL, 
                    `quantity` REAL NOT NULL, 
                    `imageUrl` TEXT, 
                    `lowStockThreshold` REAL NOT NULL DEFAULT 5.0, 
                    `unitType` TEXT NOT NULL DEFAULT 'حبة'
                )
            """)
            db.execSQL("""
                INSERT INTO `makhzan_products_new` (id, name, category, purchasePrice, sellingPrice, quantity, imageUrl, lowStockThreshold)
                SELECT id, name, category, purchasePrice, sellingPrice, CAST(quantity AS REAL), imageUrl, CAST(lowStockThreshold AS REAL)
                FROM `makhzan_products`
            """)
            db.execSQL("DROP TABLE `makhzan_products`")
            db.execSQL("ALTER TABLE `makhzan_products_new` RENAME TO `makhzan_products`")
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `makhzan_transactions` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `productId` INTEGER NOT NULL, 
                    `productName` TEXT NOT NULL, 
                    `type` TEXT NOT NULL, 
                    `quantityChanged` REAL NOT NULL, 
                    `pricePerUnit` REAL NOT NULL, 
                    `timestamp` INTEGER NOT NULL
                )
            """)
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE makhzan_transactions ADD COLUMN note TEXT NOT NULL DEFAULT ''")
        }
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Safeguard: Empty path to support transitional version 9 installs upgrading to version 10/11
        }
    }

    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE app_settings ADD COLUMN tempPart TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE app_settings ADD COLUMN permPart TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE app_settings ADD COLUMN unifiedDeviceId TEXT NOT NULL DEFAULT ''")
        }
    }

    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE app_settings ADD COLUMN isFirstLaunch INTEGER NOT NULL DEFAULT 1")
        }
    }

    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE makhzan_products ADD COLUMN hasSubUnits INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE makhzan_products ADD COLUMN parentUnitName TEXT NOT NULL DEFAULT 'كرتون'")
            db.execSQL("ALTER TABLE makhzan_products ADD COLUMN subUnitName TEXT NOT NULL DEFAULT 'حبة'")
            db.execSQL("ALTER TABLE makhzan_products ADD COLUMN subUnitCountPerParent REAL NOT NULL DEFAULT 1.0")
        }
    }

    val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `deleted_items` (
                    `id` TEXT NOT NULL, 
                    `sourceSystem` TEXT NOT NULL, 
                    `originalTableName` TEXT NOT NULL, 
                    `jsonData` TEXT NOT NULL, 
                    `deletedAt` INTEGER NOT NULL, 
                    PRIMARY KEY(`id`)
                )
            """)
        }
    }

    val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE app_settings ADD COLUMN isAutoBackupEnabled INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE app_settings ADD COLUMN isCloudSyncEnabled INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE makhzan_products ADD COLUMN barcode TEXT")
        }
    }

    val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_makhzan_products_category` ON `makhzan_products` (`category`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_timestamp` ON `transactions` (`timestamp`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_makhzan_transactions_productId` ON `makhzan_transactions` (`productId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_makhzan_transactions_timestamp` ON `makhzan_transactions` (`timestamp`)")
        }
    }

    val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE habayeb_transactions ADD COLUMN is_foreign INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE habayeb_transactions ADD COLUMN currency_code TEXT NOT NULL DEFAULT 'DEFAULT'")
            db.execSQL("ALTER TABLE habayeb_transactions ADD COLUMN foreign_amount REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE habayeb_transactions ADD COLUMN exchange_rate REAL NOT NULL DEFAULT 1.0")
            db.execSQL("ALTER TABLE habayeb_transactions ADD COLUMN is_rate_calculated INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE habayeb_transactions ADD COLUMN equivalent_amount REAL NOT NULL DEFAULT 0.0")
        }
    }

    val MIGRATION_19_20 = object : Migration(19, 20) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE app_settings ADD COLUMN exchangeRateSar REAL NOT NULL DEFAULT 1.0")
            db.execSQL("ALTER TABLE app_settings ADD COLUMN exchangeRateUsd REAL NOT NULL DEFAULT 1.0")
            db.execSQL("ALTER TABLE app_settings ADD COLUMN exchangeRateYer REAL NOT NULL DEFAULT 1.0")
        }
    }

    val MIGRATION_20_21 = object : Migration(20, 21) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Version 21 didn't have a migration defined previously, this ensures a path exists.
        }
    }

    val MIGRATION_21_22 = object : Migration(21, 22) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE habayeb_customers ADD COLUMN initialType TEXT NOT NULL DEFAULT 'OWED_BY_THEM'")
        }
    }

    val MIGRATION_22_23 = object : Migration(22, 23) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE app_settings ADD COLUMN exchangeRatesJson TEXT NOT NULL DEFAULT '{}'")
        }
    }

    val MIGRATION_23_24 = object : Migration(23, 24) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE habayeb_transactions ADD COLUMN base_currency_code TEXT NOT NULL DEFAULT 'DEFAULT'")
        }
    }

    val MIGRATION_24_25 = object : Migration(24, 25) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_category` ON `transactions` (`category`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_type` ON `transactions` (`type`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_type_timestamp` ON `transactions` (`type`, `timestamp`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_category_timestamp` ON `transactions` (`category`, `timestamp`)")
        }
    }

    val MIGRATION_25_26 = object : Migration(25, 26) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE custom_categories ADD COLUMN displayOrder INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_26_27 = object : Migration(26, 27) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE custom_categories ADD COLUMN isSystemClosed INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_27_28 = object : Migration(27, 28) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Safeguard legacy migration to version 28
        }
    }

    val MIGRATION_28_29 = object : Migration(28, 29) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // أ) بالنسبة لجدول transactions
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `transactions_new` (
                    `id` TEXT PRIMARY KEY NOT NULL, 
                    `timestamp` INTEGER NOT NULL, 
                    `type` TEXT NOT NULL, 
                    `category` TEXT NOT NULL, 
                    `amount` TEXT NOT NULL, 
                    `description` TEXT NOT NULL
                )
            """)
            db.execSQL("""
                INSERT INTO `transactions_new` (id, timestamp, type, category, amount, description)
                SELECT id, timestamp, type, category, CAST(amount AS TEXT), description FROM `transactions`
            """)
            db.execSQL("DROP TABLE `transactions`")
            db.execSQL("ALTER TABLE `transactions_new` RENAME TO `transactions`")
            
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_timestamp` ON `transactions` (`timestamp`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_category` ON `transactions` (`category`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_type` ON `transactions` (`type`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_type_timestamp` ON `transactions` (`type`, `timestamp`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_category_timestamp` ON `transactions` (`category`, `timestamp`)")

            // ب) بالنسبة لجدول fixed_commitments
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `fixed_commitments_new` (
                    `name` TEXT PRIMARY KEY NOT NULL, 
                    `targetAmount` TEXT NOT NULL, 
                    `currentProgress` TEXT NOT NULL, 
                    `orderIndex` INTEGER NOT NULL DEFAULT 0
                )
            """)
            db.execSQL("""
                INSERT INTO `fixed_commitments_new` (name, targetAmount, currentProgress, orderIndex)
                SELECT name, CAST(targetAmount AS TEXT), CAST(currentProgress AS TEXT), orderIndex FROM `fixed_commitments`
            """)
            db.execSQL("DROP TABLE `fixed_commitments`")
            db.execSQL("ALTER TABLE `fixed_commitments_new` RENAME TO `fixed_commitments`")

            // ج) بالنسبة لجدول habayeb_transactions
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `habayeb_transactions_new` (
                    `id` TEXT PRIMARY KEY NOT NULL, 
                    `customerId` TEXT NOT NULL, 
                    `type` TEXT NOT NULL, 
                    `amount` TEXT NOT NULL, 
                    `timestamp` INTEGER NOT NULL, 
                    `description` TEXT NOT NULL, 
                    `linkedMainTxId` TEXT, 
                    `is_foreign` INTEGER NOT NULL, 
                    `currency_code` TEXT NOT NULL, 
                    `foreign_amount` TEXT NOT NULL, 
                    `exchange_rate` TEXT NOT NULL, 
                    `is_rate_calculated` INTEGER NOT NULL, 
                    `equivalent_amount` TEXT NOT NULL, 
                    `base_currency_code` TEXT NOT NULL,
                    FOREIGN KEY(`customerId`) REFERENCES `habayeb_customers`(`id`) ON UPDATE CASCADE ON DELETE CASCADE
                )
            """)
            db.execSQL("""
                INSERT INTO `habayeb_transactions_new` (
                    id, customerId, type, amount, timestamp, description, linkedMainTxId, 
                    is_foreign, currency_code, foreign_amount, exchange_rate, is_rate_calculated, 
                    equivalent_amount, base_currency_code
                )
                SELECT 
                    id, customerId, type, CAST(amount AS TEXT), timestamp, description, linkedMainTxId, 
                    is_foreign, currency_code, CAST(foreign_amount AS TEXT), CAST(exchange_rate AS TEXT), is_rate_calculated, 
                    CAST(equivalent_amount AS TEXT), base_currency_code
                FROM `habayeb_transactions`
            """)
            db.execSQL("DROP TABLE `habayeb_transactions`")
            db.execSQL("ALTER TABLE `habayeb_transactions_new` RENAME TO `habayeb_transactions`")

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_habayeb_transactions_customerId` ON `habayeb_transactions` (`customerId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_habayeb_transactions_timestamp` ON `habayeb_transactions` (`timestamp`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_habayeb_transactions_linkedMainTxId` ON `habayeb_transactions` (`linkedMainTxId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_habayeb_transactions_type` ON `habayeb_transactions` (`type`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_habayeb_transactions_customerId_timestamp` ON `habayeb_transactions` (`customerId`, `timestamp`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_habayeb_transactions_customerId_type` ON `habayeb_transactions` (`customerId`, `type`)")
        }
    }

    val MIGRATION_29_30 = object : Migration(29, 30) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_habayeb_customers_name` ON `habayeb_customers` (`name`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_habayeb_customers_phone` ON `habayeb_customers` (`phone`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_habayeb_customers_createdAt` ON `habayeb_customers` (`createdAt`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_habayeb_customers_initialType` ON `habayeb_customers` (`initialType`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_habayeb_transactions_currency_code` ON `habayeb_transactions` (`currency_code`)")
        }
    }

    val MIGRATION_30_31 = object : Migration(30, 31) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `app_settings_new` (
                    `id` INTEGER NOT NULL PRIMARY KEY,
                    `currencySymbol` TEXT NOT NULL,
                    `schoolExpensesEnabled` INTEGER NOT NULL,
                    `themeMode` INTEGER NOT NULL,
                    `doubleCheckExit` INTEGER NOT NULL,
                    `isPasscodeEnabled` INTEGER NOT NULL,
                    `passcodeHash` TEXT,
                    `recoveryPhraseHash` TEXT,
                    `recoveryHint` TEXT,
                    `tempPart` TEXT NOT NULL,
                    `permPart` TEXT NOT NULL,
                    `unifiedDeviceId` TEXT NOT NULL,
                    `isFirstLaunch` INTEGER NOT NULL,
                    `isAutoBackupEnabled` INTEGER NOT NULL,
                    `isCloudSyncEnabled` INTEGER NOT NULL,
                    `exchangeRateSar` REAL NOT NULL,
                    `exchangeRateUsd` REAL NOT NULL,
                    `exchangeRateYer` REAL NOT NULL,
                    `exchangeRatesJson` TEXT NOT NULL
                )
            """)

            val cursor = db.query("PRAGMA table_info(app_settings)")
            val existingColumns = mutableSetOf<String>()
            cursor.use {
                val nameIndex = it.getColumnIndex("name")
                if (nameIndex != -1) {
                    while (it.moveToNext()) {
                        existingColumns.add(it.getString(nameIndex))
                    }
                }
            }

            val idCol = if (existingColumns.contains("id")) "id" else "1"
            val currencyCol = if (existingColumns.contains("currencySymbol")) "currencySymbol" else "'ر.ي'"
            val schoolCol = if (existingColumns.contains("schoolExpensesEnabled")) "schoolExpensesEnabled" else "1"
            val themeCol = if (existingColumns.contains("themeMode")) "themeMode" else "0"
            val doubleExitCol = if (existingColumns.contains("doubleCheckExit")) "doubleCheckExit" else "1"
            val isPasscodeCol = if (existingColumns.contains("isPasscodeEnabled")) "isPasscodeEnabled" else "0"
            val passcodeHashCol = if (existingColumns.contains("passcodeHash")) "passcodeHash" else "NULL"
            val recoveryPhraseHashCol = if (existingColumns.contains("recoveryPhraseHash")) "recoveryPhraseHash" else "NULL"
            val recoveryHintCol = if (existingColumns.contains("recoveryHint")) "recoveryHint" else "NULL"
            val tempPartCol = if (existingColumns.contains("tempPart")) "tempPart" else "''"
            val permPartCol = if (existingColumns.contains("permPart")) "permPart" else "''"
            val unifiedDeviceIdCol = if (existingColumns.contains("unifiedDeviceId")) "unifiedDeviceId" else "''"
            val isFirstLaunchCol = if (existingColumns.contains("isFirstLaunch")) "isFirstLaunch" else "1"
            val isAutoBackupCol = if (existingColumns.contains("isAutoBackupEnabled")) "isAutoBackupEnabled" else "1"
            val isCloudSyncCol = if (existingColumns.contains("isCloudSyncEnabled")) "isCloudSyncEnabled" else "0"
            val rateSarCol = if (existingColumns.contains("exchangeRateSar")) "exchangeRateSar" else "1.0"
            val rateUsdCol = if (existingColumns.contains("exchangeRateUsd")) "exchangeRateUsd" else "1.0"
            val rateYerCol = if (existingColumns.contains("exchangeRateYer")) "exchangeRateYer" else "1.0"
            val ratesJsonCol = if (existingColumns.contains("exchangeRatesJson")) "exchangeRatesJson" else "'{}'"

            db.execSQL("""
                INSERT INTO `app_settings_new` (
                    id, currencySymbol, schoolExpensesEnabled, themeMode, doubleCheckExit,
                    isPasscodeEnabled, passcodeHash, recoveryPhraseHash, recoveryHint,
                    tempPart, permPart, unifiedDeviceId, isFirstLaunch, isAutoBackupEnabled,
                    isCloudSyncEnabled, exchangeRateSar, exchangeRateUsd, exchangeRateYer, exchangeRatesJson
                )
                SELECT 
                    $idCol, $currencyCol, $schoolCol, $themeCol, $doubleExitCol,
                    $isPasscodeCol, $passcodeHashCol, $recoveryPhraseHashCol, $recoveryHintCol,
                    $tempPartCol, $permPartCol, $unifiedDeviceIdCol, $isFirstLaunchCol, $isAutoBackupCol,
                    $isCloudSyncCol, $rateSarCol, $rateUsdCol, $rateYerCol, $ratesJsonCol
                FROM `app_settings`
            """)

            db.execSQL("DROP TABLE `app_settings`")
            db.execSQL("ALTER TABLE `app_settings_new` RENAME TO `app_settings`")
        }
    }

    val ALL_MIGRATIONS = arrayOf(
        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
        MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
        MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13,
        MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17,
        MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21,
        MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25,
        MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29,
        MIGRATION_29_30, MIGRATION_30_31
    )
}
