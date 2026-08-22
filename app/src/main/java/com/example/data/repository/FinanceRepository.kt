package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.paging.PagingSource
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.entities.AppSettings
import com.example.data.local.entities.CustomCategory
import com.example.data.local.entities.DeletedItemEntity
import com.example.data.local.entities.FixedCommitment
import com.example.data.local.entities.HabayebCustomer
import com.example.data.local.entities.HabayebTransaction
import com.example.data.local.entities.TransactionDb
import com.example.data.serialization.MzdBackupSerializer
import com.example.domain.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.math.BigDecimal
import java.util.UUID

/**
 * المستودع المالي المركزي (FinanceRepository)
 * يمثل نقطة الوصول المركزية لإدارة البيانات المالية وعمليات قاعدة البيانات Room
 * وعمليات الحذف المؤقت (Trash) والاستعادة، مع الحفاظ على التوافق التام مع المعاملات والنسخ الاحتياطية.
 */
class FinanceRepository(internal val database: AppDatabase, private val context: Context) {

    companion object {
        private const val PREFS_MIZAN_SEC = "mizan_sec_prefs"
        private const val PREFS_MIZAN_FINANCE = "mizan_finance_prefs"
        private const val PREF_CAT_LINK_PREFIX = "CAT_LINK_"
        private const val PREF_KEY_PINNED_PREFIX = "KEY_PINNED_IN_"
        private const val PREF_CATEGORY_ORDER_LIST_KEY = "CATEGORY_ORDER_LIST_KEY"
        private const val PREF_CLOSED_CUSTOM_NAME_KEY = "CLOSED_CUSTOM_NAME_KEY"

        private const val JSON_PINNED_CUSTOMERS = "pinned_customer_ids_by_category"
        private const val JSON_CATEGORY_ORDER_LIST = "category_order_list"
        private const val JSON_CLOSED_CUSTOM_NAME = "closed_custom_name"
        private const val JSON_MIZAN_AL_DAR_DB = "mizan_al_dar_db"
        private const val JSON_HABAYEB_DEBTS_DB = "habayeb_debts_db"

        private const val TABLE_TRANSACTIONS = "transactions"
        private const val TABLE_FIXED_COMMITMENTS = "fixed_commitments"
        private const val TABLE_HABAYEB_CUSTOMERS = "habayeb_customers"
        private const val TABLE_HABAYEB_TRANSACTIONS = "habayeb_transactions"
        private const val BUNDLE_HABAYEB = "habayeb_bundle"
        private const val BUNDLE_DAR = "dar_bundle"
    }

    private val settingsDao = database.settingsDao()
    private val commitmentDao = database.commitmentDao()
    private val transactionDao = database.transactionDao()
    private val customCategoryDao = database.customCategoryDao()
    private val trashDao = database.trashDao()
    private val habayebDao = database.habayebDao()

    private val backupDirectoryManager = BackupDirectoryManager(context)
    private val licenseAndTrialManager = LicenseAndTrialManager(context)

    private val sourceDar: String by lazy { context.getString(com.example.R.string.source_system_dar) }
    private val sourceHabayeb: String by lazy { context.getString(com.example.R.string.source_system_habayeb) }

    private fun getSecurityPreferences(): SharedPreferences =
        context.getSharedPreferences(PREFS_MIZAN_SEC, Context.MODE_PRIVATE)

    private fun writeDualPreference(action: (SharedPreferences.Editor, SharedPreferences.Editor) -> Unit) {
        val sharedPrefs = getSecurityPreferences()
        val financePrefs = context.getSharedPreferences(PREFS_MIZAN_FINANCE, Context.MODE_PRIVATE)
        val sharedEdit = sharedPrefs.edit()
        val financeEdit = financePrefs.edit()
        action(sharedEdit, financeEdit)
        sharedEdit.apply()
        financeEdit.apply()
    }

    // --- Flow Exposures ---
    val settingsFlow: Flow<AppSettings?> = settingsDao.getSettingsFlow()
    val commitmentsFlow: Flow<List<FixedCommitment>> = commitmentDao.getAllCommitmentsFlow()
    val transactionsFlow: Flow<List<TransactionDb>> = transactionDao.getAllTransactionsFlow()
    val customCategoriesFlow: Flow<List<CustomCategory>> = customCategoryDao.getAllCustomCategoriesFlow()
    val deletedItemsFlow: Flow<List<DeletedItemEntity>> = trashDao.getAllDeletedItemsFlow()
    val habayebCustomersFlow: Flow<List<HabayebCustomer>> = habayebDao.getAllCustomersFlow()
    val habayebTransactionsFlow: Flow<List<HabayebTransaction>> = habayebDao.getAllTransactionsFlow()

    fun getTransactionsForCustomerFlow(customerId: String): Flow<List<HabayebTransaction>> = 
        habayebDao.getTransactionsForCustomerFlow(customerId)

    fun getTransactionsPagingSourceForCustomer(customerId: String): PagingSource<Int, HabayebTransaction> =
        habayebDao.getTransactionsPagingSourceForCustomer(customerId)

    fun getForeignTransactionsFlow(): Flow<List<HabayebTransaction>> = habayebDao.getForeignTransactionsFlow()
    fun getTransactionsForCustomerWithLimitFlow(customerId: String, limit: Int): Flow<List<HabayebTransaction>> = habayebDao.getTransactionsForCustomerWithLimitFlow(customerId, limit)
    fun getHabayebTransactionsCountFlow(): Flow<Int> = habayebDao.getHabayebTransactionsCountFlow()
    fun getTotalCashFlow(): Flow<BigDecimal> = transactionDao.getTotalCashFlow()
    fun getTransactionsCountFlow(): Flow<Int> = transactionDao.getTransactionsCountFlow()

    // --- Settings & Commitments Operations ---
    suspend fun getSettingsDirect(): AppSettings? = settingsDao.getSettingsDirect()
    suspend fun saveSettings(settings: AppSettings) = settingsDao.insertOrUpdateSettings(settings)

    suspend fun saveCommitment(commitment: FixedCommitment) = commitmentDao.insertCommitment(commitment)
    suspend fun updateCommitments(commitments: List<FixedCommitment>) = commitmentDao.updateCommitments(commitments)
    suspend fun deleteCommitment(name: String) = commitmentDao.deleteCommitment(name)
    suspend fun clearCommitments() = commitmentDao.clearAllCommitments()

    suspend fun getTransactionById(id: String): TransactionDb? = transactionDao.getTransactionById(id)
    suspend fun saveTransaction(transaction: TransactionDb) = transactionDao.insertTransaction(transaction)
    suspend fun deleteTransaction(transaction: TransactionDb) = transactionDao.deleteTransaction(transaction)
    suspend fun deleteTransactionById(id: String) = transactionDao.deleteTransactionById(id)
    suspend fun clearTransactions() = transactionDao.clearAllTransactions()

    suspend fun saveCustomCategory(category: CustomCategory) = customCategoryDao.insertCategory(category)
    suspend fun deleteCustomCategory(category: CustomCategory) = customCategoryDao.deleteCategory(category)
    suspend fun clearCustomCategories() = customCategoryDao.clearAllCustomCategories()

    suspend fun updateCustomCategoriesOrder(orderedNames: List<String>) = withContext(Dispatchers.IO) {
        val currentCategories = customCategoryDao.getAllCustomCategoriesFlow().first()
        val orderMap = orderedNames.withIndex().associate { it.value to it.index }
        val updatedCategories = currentCategories.map { category ->
            val key = if (category.isSystemClosed) "CLOSED" else category.name
            val index = orderMap[key] ?: 999
            category.copy(displayOrder = index)
        }
        customCategoryDao.updateCategories(updatedCategories)
    }

    suspend fun getPagedTransactionsDirect(limit: Int, offset: Int): List<TransactionDb> = transactionDao.getPagedTransactionsDirect(limit, offset)
    suspend fun getExpensesSumForPeriod(startTimestamp: Long, endTimestamp: Long): BigDecimal = transactionDao.getExpensesSumForPeriod(startTimestamp, endTimestamp)
    suspend fun getTransactionsCountDirect(): Int = transactionDao.getTransactionsCountDirect()

    // --- Habayeb & Ledger Operations ---
    suspend fun insertCustomer(customer: HabayebCustomer) = habayebDao.insertCustomer(customer)
    suspend fun updateCustomer(customer: HabayebCustomer) = database.withTransaction {
        val oldCustomer = habayebDao.getCustomerByIdDirect(customer.id)
        habayebDao.updateCustomer(customer)
        if (oldCustomer != null && oldCustomer.initialType != customer.initialType) {
            when (customer.initialType) {
                TransactionType.OWED_BY_THEM.value -> habayebDao.adaptTransactionsToOwedByThem(customer.id)
                TransactionType.OWED_TO_THEM.value -> habayebDao.adaptTransactionsToOwedToThem(customer.id)
            }
        }
    }
    suspend fun insertCustomerWithOpeningTransaction(customer: HabayebCustomer, transaction: HabayebTransaction?) = 
        habayebDao.insertCustomerWithOpeningTransaction(customer, transaction)
    suspend fun deleteCustomerAndTransactions(customerId: String) = habayebDao.deleteCustomerAndTransactions(customerId)
    suspend fun updateCustomerName(id: String, newName: String) = habayebDao.updateCustomerName(id, newName)
    suspend fun insertHabayebTransaction(transaction: HabayebTransaction) = habayebDao.insertTransaction(transaction)
    suspend fun deleteHabayebTransaction(transaction: HabayebTransaction) = habayebDao.deleteTransaction(transaction)
    suspend fun deleteHabayebTransactionById(id: String) = habayebDao.deleteTransactionById(id)
    suspend fun getHabayebTransactionById(id: String): HabayebTransaction? = habayebDao.getTransactionById(id)
    suspend fun getCustomerByIdDirect(id: String): HabayebCustomer? = habayebDao.getCustomerByIdDirect(id)
    suspend fun getAllCustomersDirect(): List<HabayebCustomer> = habayebDao.getAllCustomersDirect()
    suspend fun getAllTransactionsDirect(): List<HabayebTransaction> = habayebDao.getAllTransactionsDirect()
    suspend fun getTransactionsForCustomerDirect(customerId: String): List<HabayebTransaction> = habayebDao.getTransactionsForCustomerDirect(customerId)
    suspend fun clearAllCustomers() = habayebDao.clearAllCustomers()
    suspend fun clearAllTransactions() = habayebDao.clearAllTransactions()
    suspend fun getTransactionsForCustomerPaged(customerId: String, limit: Int, offset: Int): List<HabayebTransaction> = habayebDao.getTransactionsForCustomerPaged(customerId, limit, offset)
    suspend fun getHabayebTransactionsCountDirect(): Int = habayebDao.getHabayebTransactionsCountDirect()

    suspend fun getRealTotalTransactionsCount(): Int = withContext(Dispatchers.IO) {
        getTransactionsCountDirect() + getHabayebTransactionsCountDirect()
    }

    // --- Trash & Soft Delete Delegation ---
    suspend fun getAllDeletedItemsDirect(): List<DeletedItemEntity> = trashDao.getAllDeletedItemsDirect()
    suspend fun saveDeletedItem(item: DeletedItemEntity) = trashDao.insertDeletedItem(item)
    suspend fun removeDeletedItem(item: DeletedItemEntity) = trashDao.deleteItem(item)
    suspend fun removeDeletedItemById(id: String) = trashDao.deleteItemById(id)
    suspend fun clearDeletedItems() = trashDao.clearAllDeletedItems()

    suspend fun softDeleteCommitmentToTrash(fc: FixedCommitment) = withContext(Dispatchers.IO) {
        val jsonData = TrashJsonSerializer.serializeCommitment(fc)
        val trashItem = DeletedItemEntity(
            id = "fc_${fc.name}",
            sourceSystem = sourceDar,
            originalTableName = TABLE_FIXED_COMMITMENTS,
            jsonData = jsonData
        )
        saveDeletedItem(trashItem)
    }

    suspend fun softDeleteHabayebBundleToTrash(customer: HabayebCustomer, transactions: List<HabayebTransaction>) = withContext(Dispatchers.IO) {
        val sharedPrefs = getSecurityPreferences()
        val jsonData = TrashJsonSerializer.serializeHabayebBundle(customer, transactions, sharedPrefs)
        val trashItem = DeletedItemEntity(
            id = "bundle_${customer.id}",
            sourceSystem = sourceHabayeb,
            originalTableName = BUNDLE_HABAYEB,
            jsonData = jsonData
        )
        saveDeletedItem(trashItem)
    }

    suspend fun softDeleteHabayebCustomerToTrash(customer: HabayebCustomer) = withContext(Dispatchers.IO) {
        val jsonData = TrashJsonSerializer.serializeHabayebCustomer(customer)
        val trashItem = DeletedItemEntity(
            id = "cust_${customer.id}",
            sourceSystem = sourceHabayeb,
            originalTableName = TABLE_HABAYEB_CUSTOMERS,
            jsonData = jsonData
        )
        saveDeletedItem(trashItem)
    }

    suspend fun softDeleteTransactionToTrash(tx: TransactionDb) = withContext(Dispatchers.IO) {
        val jsonData = TrashJsonSerializer.serializeTransaction(tx)
        val trashItem = DeletedItemEntity(
            id = tx.id,
            sourceSystem = sourceDar,
            originalTableName = TABLE_TRANSACTIONS,
            jsonData = jsonData
        )
        saveDeletedItem(trashItem)
    }

    suspend fun softDeleteTransactionBundleToTrash(transactions: List<TransactionDb>, title: String) = withContext(Dispatchers.IO) {
        val jsonData = TrashJsonSerializer.serializeTransactionBundle(transactions, title)
        val id = "dar_bundle_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
        val trashItem = DeletedItemEntity(
            id = id,
            sourceSystem = sourceDar,
            originalTableName = BUNDLE_DAR,
            jsonData = jsonData
        )
        saveDeletedItem(trashItem)
    }

    suspend fun softDeleteHabayebTransactionToTrash(tx: HabayebTransaction) = withContext(Dispatchers.IO) {
        val jsonData = TrashJsonSerializer.serializeHabayebTransaction(tx)
        val trashItem = DeletedItemEntity(
            id = tx.id,
            sourceSystem = sourceHabayeb,
            originalTableName = TABLE_HABAYEB_TRANSACTIONS,
            jsonData = jsonData
        )
        saveDeletedItem(trashItem)
    }

    // --- Licensing & File Management Delegation ---
    fun isAppActivated(): Boolean = licenseAndTrialManager.isAppActivated()

    suspend fun isTrialExpiredDirect(): Boolean = withContext(Dispatchers.IO) {
        val totalCount = getRealTotalTransactionsCount()
        licenseAndTrialManager.isTrialExpiredDirect(totalCount)
    }

    fun getBaseBackupDirectory(): File = backupDirectoryManager.getBaseBackupDirectory()

    fun getBackupDirectory(): File = backupDirectoryManager.getBackupDirectory()

    fun getAllMzdFilesRecursively(rootDir: File): List<File> = backupDirectoryManager.getAllMzdFilesRecursively(rootDir)

    // --- Transactional Master Restore ---
    suspend fun deleteAllData(): Unit = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
                transactionDao.clearAllTransactions()
                commitmentDao.clearAllCommitments()
                customCategoryDao.clearAllCustomCategories()
                trashDao.clearAllDeletedItems()
                habayebDao.clearAllCustomers()
                habayebDao.clearAllTransactions()
                settingsDao.insertOrUpdateSettings(AppSettings(isFirstLaunch = false))
            }
        } catch (e: Exception) {
            Log.e("FinanceRepository", "Error inside deleteAllData: ${e.message}", e)
            throw e
        }
    }

    /**
     * تنفيذ الاستعادة الشاملة للنسخة الاحتياطية (Transactional Master Restore)
     *
     * التسلسل المعماري الصارم:
     * 1. التحليل والتحقق في الذاكرة أولاً (Parse & Validate in-memory).
     * 2. التحقق من سلامة العلاقات (مثل روابط المعاملات linkedMainTxId وعلاقات العملاء).
     * 3. بدء المعاملة الذرية لقاعدة البيانات (database.withTransaction).
     * 4. تصفية جميع الجداول معاً (Clear all tables together within the atomic transaction).
     * 5. إدراج جميع السجلات المستعادة وتحديث التفضيلات المزدوجة.
     * 6. في حال حدوث أي استثناء، تتراجع المعاملة تلقائياً (Rollback) لمنع ترك قاعدة البيانات في حالة تالفة أو فارغة.
     */
    suspend fun executeMasterRestore(rawJsonString: String): RestoreResult = withContext(Dispatchers.IO) {
        val root = JSONObject(rawJsonString)
        val currentLocalSettings = getSettingsDirect() ?: AppSettings()
        
        // 1. مرحلة التحليل النحوي في الذاكرة
        val data = MzdBackupSerializer.importBackupFromJson(rawJsonString, context)
        val restoredSettingsUnmerged = data.first
        val restoredSettings = restoredSettingsUnmerged.copy(
            themeMode = currentLocalSettings.themeMode,
            doubleCheckExit = currentLocalSettings.doubleCheckExit,
            isPasscodeEnabled = currentLocalSettings.isPasscodeEnabled,
            passcodeHash = currentLocalSettings.passcodeHash,
            recoveryPhraseHash = currentLocalSettings.recoveryPhraseHash,
            recoveryHint = currentLocalSettings.recoveryHint,
            tempPart = currentLocalSettings.tempPart,
            permPart = currentLocalSettings.permPart,
            unifiedDeviceId = currentLocalSettings.unifiedDeviceId,
            isFirstLaunch = currentLocalSettings.isFirstLaunch,
            isAutoBackupEnabled = currentLocalSettings.isAutoBackupEnabled,
            isCloudSyncEnabled = currentLocalSettings.isCloudSyncEnabled
        )
        val restoredCommitments = data.second
        val restoredTransactions = data.third
        val customCategories = MzdBackupSerializer.parseCustomCategories(root)
        val deletedItems = MzdBackupSerializer.parseDeletedItems(root)
        val restoredCustomerData = MzdBackupSerializer.parseHabayebCustomers(root)
        val habayebTransactions = MzdBackupSerializer.parseHabayebTransactions(root, restoredSettings.currencySymbol)

        // 2. مرحلة التحقق من سلامة العلاقات والهوية (Integrity & Relationship Validation)
        val customerIdSet = restoredCustomerData.map { it.customer.id }.toSet()
        val validatedHabayebTransactions = habayebTransactions.map { tx ->
            val cleanLinkedId = tx.linkedMainTxId?.takeIf { it.isNotBlank() && it != tx.id && it != "0" && !it.equals("null", ignoreCase = true) }
            val cleanCustomerId = if (customerIdSet.contains(tx.customerId)) tx.customerId else tx.customerId.trim()
            tx.copy(customerId = cleanCustomerId, linkedMainTxId = cleanLinkedId)
        }

        // 3. المعاملة الذرية الشاملة لقاعدة البيانات
        database.withTransaction {
            // أ. التصفية المتزامنة لكافة الجداول داخل المعاملة فقط
            clearTransactions()
            clearCommitments()
            clearCustomCategories()
            clearDeletedItems()
            clearAllCustomers()
            clearAllTransactions()

            // ب. إدراج السجلات المستعادة
            saveSettings(restoredSettings)
            for (fc in restoredCommitments) {
                saveCommitment(fc)
            }
            for (tx in restoredTransactions) {
                saveTransaction(tx)
            }

            // استعادة الفئات المخصصة
            for (cat in customCategories) {
                saveCustomCategory(cat)
            }

            // استعادة التفضيلات المشتركة وحالات التثبيت
            writeDualPreference { sharedEdit, financeEdit ->
                if (root.has(JSON_PINNED_CUSTOMERS) && !root.isNull(JSON_PINNED_CUSTOMERS)) {
                    val pinnedObj = root.optJSONObject(JSON_PINNED_CUSTOMERS)
                    if (pinnedObj != null) {
                        val keys = pinnedObj.keys()
                        while (keys.hasNext()) {
                            val catKey = keys.next()
                            val arr = pinnedObj.getJSONArray(catKey)
                            val set = mutableSetOf<String>()
                            for (i in 0 until arr.length()) {
                                set.add(arr.getString(i))
                            }
                            val prefKey = "$PREF_KEY_PINNED_PREFIX$catKey"
                            sharedEdit.putStringSet(prefKey, set)
                            financeEdit.putStringSet(prefKey, set)
                        }
                    }
                }

                if (root.has(JSON_CATEGORY_ORDER_LIST) && !root.isNull(JSON_CATEGORY_ORDER_LIST)) {
                    val catOrder = root.getString(JSON_CATEGORY_ORDER_LIST)
                    sharedEdit.putString(PREF_CATEGORY_ORDER_LIST_KEY, catOrder)
                    financeEdit.putString(PREF_CATEGORY_ORDER_LIST_KEY, catOrder)
                }
                if (root.has(JSON_CLOSED_CUSTOM_NAME) && !root.isNull(JSON_CLOSED_CUSTOM_NAME)) {
                    val closedCustomName = root.getString(JSON_CLOSED_CUSTOM_NAME)
                    sharedEdit.putString(PREF_CLOSED_CUSTOM_NAME_KEY, closedCustomName)
                    financeEdit.putString(PREF_CLOSED_CUSTOM_NAME_KEY, closedCustomName)
                }
            }

            // استعادة سلة المهملات
            for (item in deletedItems) {
                saveDeletedItem(item)
            }

            // استعادة عملاء الحبايب والروابط
            for (custData in restoredCustomerData) {
                insertCustomer(custData.customer)
                custData.categoryLink?.let { catLink ->
                    writeDualPreference { sharedEdit, financeEdit ->
                        sharedEdit.putString("$PREF_CAT_LINK_PREFIX${custData.customer.id}", catLink)
                        financeEdit.putString("$PREF_CAT_LINK_PREFIX${custData.customer.id}", catLink)
                    }
                }
            }

            // استعادة معاملات الحبايب المحققة
            for (tx in validatedHabayebTransactions) {
                insertHabayebTransaction(tx)
            }
        }

        val isLegacy = root.has(JSON_MIZAN_AL_DAR_DB) || root.has(JSON_HABAYEB_DEBTS_DB)
        RestoreResult(restoredSettings, isLegacy)
    }

    suspend fun restoreDeletedItem(item: DeletedItemEntity) = withContext(Dispatchers.IO) {
        trashDao.restoreDeletedItem(item)
    }

    suspend fun restoreSingleTransactionFromBundle(itemId: String, txId: String) = withContext(Dispatchers.IO) {
        trashDao.restoreSingleTransactionFromBundle(itemId, txId)
    }
}

data class RestoreResult(val settings: AppSettings, val isLegacy: Boolean)
