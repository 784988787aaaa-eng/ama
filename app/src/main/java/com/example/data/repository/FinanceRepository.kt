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
import com.example.data.serialization.BackupPayloadData
import com.example.data.serialization.BackupPayloadSerializer
import com.example.domain.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import android.util.Base64
import java.math.BigDecimal
import java.util.UUID

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

    suspend fun executeMasterRestore(rawJsonString: String): RestoreResult = withContext(Dispatchers.IO) {
        val root = JSONObject(rawJsonString)
        val currentLocalSettings = getSettingsDirect() ?: AppSettings()
        
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

        // Integrity verification MUST happen before the first destructive DB operation.
        // Legacy backups without the versioned hash remain restorable for compatibility,
        // but new backups are rejected if their canonical logical content was modified.
        // Parse the profile once and reuse the immutable snapshot for integrity verification.
        // Re-parsing the same JSON four times is unnecessary and made it easier for future
        // changes to accidentally use slightly different interpretations of the profile.
        val restoredBusinessProfile = BackupPayloadSerializer.parseBusinessProfile(root)

        verifyBackupIntegrityIfVersioned(
            root = root,
            data = BackupPayloadData(
                settings = restoredSettingsUnmerged,
                commitments = restoredCommitments,
                transactions = restoredTransactions,
                habayebCustomers = restoredCustomerData.map { it.customer },
                habayebTransactions = habayebTransactions,
                deletedItems = deletedItems,
                customCategories = customCategories,
                categoryLinks = restoredCustomerData.mapNotNull { data ->
                    data.categoryLink?.let { data.customer.id to it }
                }.toMap(),
                pinnedCustomerIdsByCategory = parsePinnedCustomerIds(root),
                categoryOrderList = root.optStringOrNull(JSON_CATEGORY_ORDER_LIST),
                closedCustomName = root.optStringOrNull(JSON_CLOSED_CUSTOM_NAME),
                businessName = restoredBusinessProfile.name,
                businessDescription = restoredBusinessProfile.description,
                businessPhones = restoredBusinessProfile.phones,
                businessLogoBase64 = restoredBusinessProfile.logoBase64
            )
        )

        validateRestoreIntegrity(
            restoredCustomerData.map { it.customer },
            habayebTransactions,
            restoredTransactions
        )

        database.withTransaction {
            clearTransactions()
            clearCommitments()
            clearCustomCategories()
            clearDeletedItems()

            saveSettings(restoredSettings)
            for (fc in restoredCommitments) {
                saveCommitment(fc)
            }
            for (tx in restoredTransactions) {
                saveTransaction(tx)
            }

            // Restore Custom Categories
            for (cat in customCategories) {
                saveCustomCategory(cat)
            }

            // Restore Deleted Items
            for (item in deletedItems) {
                saveDeletedItem(item)
            }

            clearAllCustomers()
            clearAllTransactions()

            // Restore Habayeb Customers. Category-link preferences are applied after the
            // database transaction so a DB rollback cannot leave a partially restored
            // preference set behind.
            for (custData in restoredCustomerData) {
                insertCustomer(custData.customer)
            }

            // Restore Habayeb Transactions
            for (tx in habayebTransactions) {
                insertHabayebTransaction(tx)
            }
        }

        // SharedPreferences are not part of Room's transaction. Rebuild the restore-owned
        // preference namespace only after the DB transaction succeeds, and clear stale keys
        // that are absent from the backup. Without this replacement step, restoring a backup
        // with fewer pins/category-links could silently retain values from the previous device state.
        restoreOwnedPreferences(root, restoredCustomerData)
        restoreBusinessProfile(restoredBusinessProfile, root.has("business_profile"))

        val isLegacy = root.has(JSON_MIZAN_AL_DAR_DB) || root.has(JSON_HABAYEB_DEBTS_DB)
        RestoreResult(restoredSettings, isLegacy)
    }

    private fun restoreOwnedPreferences(
        root: JSONObject,
        restoredCustomerData: List<MzdBackupSerializer.RestoredHabayebCustomerData>
    ) {
        val pinnedByCategory = parsePinnedCustomerIds(root)
        val categoryOrder = root.optStringOrNull(JSON_CATEGORY_ORDER_LIST)
        val closedCustomName = root.optStringOrNull(JSON_CLOSED_CUSTOM_NAME)

        writeDualPreference { sharedEdit, financeEdit ->
            fun clearRestoreOwnedKeys(prefs: SharedPreferences.Editor, current: Map<String, *>) {
                current.keys
                    .filter { it.startsWith(PREF_CAT_LINK_PREFIX) || it.startsWith(PREF_KEY_PINNED_PREFIX) }
                    .forEach(prefs::remove)
                prefs.remove(PREF_CATEGORY_ORDER_LIST_KEY)
                prefs.remove(PREF_CLOSED_CUSTOM_NAME_KEY)
            }

            val sharedCurrent = context.getSharedPreferences(PREFS_MIZAN_SEC, Context.MODE_PRIVATE).all
            val financeCurrent = context.getSharedPreferences(PREFS_MIZAN_FINANCE, Context.MODE_PRIVATE).all
            clearRestoreOwnedKeys(sharedEdit, sharedCurrent)
            clearRestoreOwnedKeys(financeEdit, financeCurrent)

            pinnedByCategory.forEach { (category, ids) ->
                val prefKey = "$PREF_KEY_PINNED_PREFIX$category"
                sharedEdit.putStringSet(prefKey, ids)
                financeEdit.putStringSet(prefKey, ids)
            }

            if (categoryOrder != null) {
                sharedEdit.putString(PREF_CATEGORY_ORDER_LIST_KEY, categoryOrder)
                financeEdit.putString(PREF_CATEGORY_ORDER_LIST_KEY, categoryOrder)
            }
            if (closedCustomName != null) {
                sharedEdit.putString(PREF_CLOSED_CUSTOM_NAME_KEY, closedCustomName)
                financeEdit.putString(PREF_CLOSED_CUSTOM_NAME_KEY, closedCustomName)
            }

            restoredCustomerData.forEach { custData ->
                custData.categoryLink?.let { link ->
                    val prefKey = "$PREF_CAT_LINK_PREFIX${custData.customer.id}"
                    sharedEdit.putString(prefKey, link)
                    financeEdit.putString(prefKey, link)
                }
            }
        }
    }

    private fun restoreBusinessProfile(
        profile: BackupPayloadSerializer.BusinessProfileBackupData,
        presentInBackup: Boolean
    ) {
        // Old backups do not contain Business Profile; preserve the current device
        // profile for backward compatibility. Current backups contain the object even
        // when the user has no profile configured.
        if (!presentInBackup) return

        val prefs = context.getSharedPreferences("business_profile", Context.MODE_PRIVATE)
        val altPrefs = context.getSharedPreferences("business_profile_prefs", Context.MODE_PRIVATE)
        val targetLogo = File(context.filesDir, "business_logo.png")
        val stagedLogo = File(context.filesDir, "business_logo.restore.tmp")
        val oldLogoBackup = File(context.filesDir, "business_logo.previous.tmp")

        stagedLogo.delete()
        oldLogoBackup.delete()
        val originalPrefs = prefs.all.toMap()
        val originalAltPrefs = altPrefs.all.toMap()

        // Stage all file changes first. Do not destroy the current logo until the
        // replacement is fully decoded and written. Keep a rollback copy until both
        // preference namespaces have been durably committed.
        if (profile.logoBase64 != null) {
            runCatching {
                val bytes = Base64.decode(profile.logoBase64, Base64.DEFAULT)
                require(bytes.isNotEmpty()) { "Business profile logo is empty" }
                stagedLogo.outputStream().use { it.write(bytes) }
                require(stagedLogo.length() > 0) { "Business profile logo is empty" }
            }.getOrElse { failure ->
                stagedLogo.delete()
                throw IllegalArgumentException(
                    "Business profile logo could not be prepared",
                    failure
                )
            }
        }

        if (targetLogo.exists()) {
            require(targetLogo.renameTo(oldLogoBackup)) {
                "Could not stage the existing business logo"
            }
        }

        fun restorePreferenceSnapshot(target: SharedPreferences, snapshot: Map<String, *>) {
            val editor = target.edit().clear()
            snapshot.forEach { (key, value) ->
                when (value) {
                    is String -> editor.putString(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
                }
            }
            check(editor.commit()) { "Failed to roll back Business Profile preferences" }
        }

        fun rollbackLogoAndPreferences() {
            stagedLogo.delete()
            targetLogo.delete()
            if (oldLogoBackup.exists()) oldLogoBackup.renameTo(targetLogo)
            restorePreferenceSnapshot(prefs, originalPrefs)
            restorePreferenceSnapshot(altPrefs, originalAltPrefs)
        }

        try {
            if (profile.logoBase64 != null) {
                require(stagedLogo.renameTo(targetLogo)) {
                    "Could not install restored business logo"
                }
            }

            // commit() is intentional here: Business Profile is user-owned restore state
            // and must be durably written before this restore step is considered complete.
            check(
                prefs.edit().clear()
                    .putString("biz_name", profile.name.orEmpty())
                    .putString("biz_desc", profile.description.orEmpty())
                    .putString("biz_phones", org.json.JSONArray(profile.phones).toString())
                    .apply {
                        if (profile.logoBase64 != null) putString("biz_logo_path", targetLogo.absolutePath)
                        else remove("biz_logo_path")
                    }
                    .commit()
            ) { "Failed to persist restored Business Profile" }

            // Keep the legacy preference namespace synchronized so older readers and the
            // PDF loader's fallback path see the same restored user-owned profile.
            check(
                altPrefs.edit().clear()
                    .putString("business_name", profile.name.orEmpty())
                    .putString("business_slogan", profile.description.orEmpty())
                    .putString("business_phone", profile.phones.firstOrNull().orEmpty())
                    .apply {
                        if (profile.logoBase64 != null) putString("logo_path", targetLogo.absolutePath)
                        else remove("logo_path")
                    }
                    .commit()
            ) { "Failed to persist legacy Business Profile preferences" }

            // Only now is it safe to discard the previous logo.
            oldLogoBackup.delete()
        } catch (failure: Throwable) {
            // Restore both the file and preference namespaces to their pre-restore values.
            // This makes the Business Profile step itself failure-safe even though it is
            // outside Room's transaction.
            rollbackLogoAndPreferences()
            throw IllegalArgumentException(
                "Business Profile could not be restored completely",
                failure
            )
        }
    }

    private fun verifyBackupIntegrityIfVersioned(root: JSONObject, data: BackupPayloadData) {
        val metadata = root.optJSONObject("metadata") ?: return
        val version = metadata.optInt("security_hash_version", 0)
        if (version == 0) return // Legacy/unversioned backup: preserve backward compatibility.
        require(version == 2 || version == BackupPayloadSerializer.CURRENT_SECURITY_HASH_VERSION) {
            "Unsupported backup integrity hash version: $version"
        }
        require(BackupPayloadSerializer.verifyIntegrityHash(root, data)) {
            "Backup integrity verification failed: the payload was modified or is incomplete"
        }
    }

    private fun parsePinnedCustomerIds(root: JSONObject): Map<String, Set<String>> {
        val result = mutableMapOf<String, Set<String>>()
        val pinnedObj = root.optJSONObject(JSON_PINNED_CUSTOMERS) ?: return result
        val keys = pinnedObj.keys()
        while (keys.hasNext()) {
            val category = keys.next()
            val arr = pinnedObj.optJSONArray(category) ?: continue
            val ids = linkedSetOf<String>()
            for (i in 0 until arr.length()) ids.add(arr.getString(i))
            result[category] = ids
        }
        return result
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key, null)

    /**
     * Rejects structurally inconsistent restore payloads before any destructive
     * database operation begins. Foreign-key constraints protect customerId,
     * but linkedMainTxId is intentionally a soft relationship and therefore
     * needs explicit validation.
     */
    private fun validateRestoreIntegrity(
        customers: List<HabayebCustomer>,
        habayebTransactions: List<HabayebTransaction>,
        transactions: List<TransactionDb>
    ) {
        fun requireUniqueIds(ids: List<String>, label: String) {
            val duplicates = ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
            require(duplicates.isEmpty()) {
                "Backup contains duplicate $label IDs: ${duplicates.take(10)}"
            }
        }

        requireUniqueIds(customers.map { it.id }, "customer")
        requireUniqueIds(habayebTransactions.map { it.id }, "Habayeb transaction")
        requireUniqueIds(transactions.map { it.id }, "transaction")

        val customerIds = customers.map { it.id }.toHashSet()
        val txIds = habayebTransactions.map { it.id }.toHashSet()

        val missingCustomers = habayebTransactions
            .asSequence()
            .filter { it.customerId !in customerIds }
            .map { it.id to it.customerId }
            .take(10)
            .toList()
        require(missingCustomers.isEmpty()) {
            "Backup contains transactions referencing missing customers: $missingCustomers"
        }

        val badLinks = habayebTransactions
            .asSequence()
            .mapNotNull { tx ->
                val link = tx.linkedMainTxId?.trim()?.takeIf { it.isNotEmpty() }
                when {
                    link == null -> null
                    link == tx.id -> tx.id to "self-link"
                    link !in txIds -> tx.id to link
                    else -> null
                }
            }
            .take(10)
            .toList()
        require(badLinks.isEmpty()) {
            "Backup contains dangling/invalid linkedMainTxId references: $badLinks"
        }
    }

    suspend fun restoreDeletedItem(item: DeletedItemEntity) = withContext(Dispatchers.IO) {
        trashDao.restoreDeletedItem(item)
    }

    suspend fun restoreSingleTransactionFromBundle(itemId: String, txId: String) = withContext(Dispatchers.IO) {
        trashDao.restoreSingleTransactionFromBundle(itemId, txId)
    }
}

data class RestoreResult(val settings: AppSettings, val isLegacy: Boolean)
