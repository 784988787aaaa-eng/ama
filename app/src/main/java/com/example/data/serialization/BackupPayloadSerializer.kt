package com.example.data.serialization

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.BigDecimalConverter
import com.example.data.local.entities.AppSettings
import com.example.data.local.entities.CustomCategory
import com.example.data.local.entities.DatabaseDefaults
import com.example.data.local.entities.DeletedItemEntity
import com.example.data.local.entities.FixedCommitment
import com.example.data.local.entities.HabayebCustomer
import com.example.data.local.entities.HabayebTransaction
import com.example.data.local.entities.TransactionDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.math.BigDecimal
import java.security.MessageDigest

/**
 * وعاء بيانات النسخ الاحتياطي الكامل (Backup Payload Container)
 */
data class BackupPayloadData(
    val settings: AppSettings,
    val commitments: List<FixedCommitment>,
    val transactions: List<TransactionDb>,
    val habayebCustomers: List<HabayebCustomer> = emptyList(),
    val habayebTransactions: List<HabayebTransaction> = emptyList(),
    val deletedItems: List<DeletedItemEntity> = emptyList(),
    val customCategories: List<CustomCategory> = emptyList(),
    val categoryLinks: Map<String, String> = emptyMap(),
    val pinnedCustomerIdsByCategory: Map<String, Set<String>> = emptyMap(),
    val categoryOrderList: String? = null,
    val closedCustomName: String? = null
)

/**
 * محرك تسلسل وتشفير وفحص سلامة النسخ الاحتياطية (Unified Backup Serializer & Integrity Engine)
 *
 * التوثيق المعماري للمبادئ الصارمة:
 * 1. وجود البصمة الأمنية (Integrity Hash): لضمان سلامة النسخة الاحتياطية من التلف العفوي أثناء النقل
 *    أو التعديل غير المصرح به، ومقارنة التغييرات بدقة قبل التخزين السحابي.
 * 2. الترتيب الحتمي للبيانات (Deterministic Ordering): خرائط الذاكرة (Maps/HashSets) وترتيب استرجاع قواعد
 *    البيانات قد يختلف بين الأجهزة وإصدارات أندرويد؛ لذا يتم ترتيب جميع القوائم والمجموعات منطقياً قبل التشفير
 *    أو بناء البصمة لضمان تطابق البصمة لنفس البيانات بنسبة 100%.
 * 3. استبعاد المعرفات الداخلية التلقائية (Room Internal IDs): الاعتماد يكون حصرياً على الهوية المنطقية للكيانات
 *    (مثل UUID أو اسم البند أو الطابع الزمني) وليس على Auto-increment rowid لقاعدة البيانات المحلية.
 * 4. الحفاظ التام على الدقة المالية لـ BigDecimal ومنع أي تحويل وسيط عبر optDouble أو Double.toString.
 */
object BackupPayloadSerializer {
    private const val TAG = "BackupPayloadSerializer"
    private val HEX_CHARS = "0123456789abcdef".toCharArray()

    // Cryptographic & Preferences Constants
    private const val ALGORITHM_SHA_256 = "SHA-256"
    private const val PREF_MIZAN_SEC = "mizan_sec_prefs"
    private const val PREF_MIZAN_FINANCE = "mizan_finance_prefs"
    private const val PREFIX_CAT_LINK = "CAT_LINK_"
    private const val PREFIX_KEY_PINNED_IN = "KEY_PINNED_IN_"
    private const val KEY_CATEGORY_ORDER_LIST_PREF = "CATEGORY_ORDER_LIST_KEY"
    private const val KEY_CLOSED_CUSTOM_NAME_PREF = "CLOSED_CUSTOM_NAME_KEY"

    // JSON Schema Structural Keys
    private const val KEY_MIZAN_AL_DAR_DB = "mizan_al_dar_db"
    private const val KEY_METADATA = "metadata"
    private const val KEY_APP_NAME = "app_name"
    private const val KEY_APP_VERSION = "app_version"
    private const val KEY_BACKUP_TIMESTAMP = "backup_timestamp"
    private const val KEY_SECURITY_HASH = "security_hash"

    private const val KEY_SETTINGS = "settings"
    private const val KEY_CURRENCY_SYMBOL = "currency_symbol"
    private const val KEY_SCHOOL_EXPENSES_ENABLED = "school_expenses_enabled"
    private const val KEY_EXCHANGE_RATES_JSON = "exchange_rates_json"

    private const val KEY_FIXED_COMMITMENTS = "fixed_commitments"
    private const val KEY_NAME = "name"
    private const val KEY_TARGET_AMOUNT = "target_amount"
    private const val KEY_CURRENT_PROGRESS = "current_progress"
    private const val KEY_ORDER_INDEX = "order_index"

    private const val KEY_TRANSACTIONS = "transactions"
    private const val KEY_ID = "id"
    private const val KEY_TIMESTAMP = "timestamp"
    private const val KEY_TYPE = "type"
    private const val KEY_CATEGORY = "category"
    private const val KEY_AMOUNT = "amount"
    private const val KEY_DESCRIPTION = "description"

    private const val KEY_HABAYEB_DEBTS = "habayeb_debts"
    private const val KEY_CUSTOMERS = "customers"
    private const val KEY_PHONE = "phone"
    private const val KEY_NOTES = "notes"
    private const val KEY_CREATED_AT = "created_at"
    private const val KEY_INITIAL_TYPE = "initial_type"
    private const val KEY_CATEGORY_LINK = "category_link"

    private const val KEY_DEBT_TRANSACTIONS = "debt_transactions"
    private const val KEY_CUSTOMER_ID = "customer_id"
    private const val KEY_LINKED_MAIN_TX_ID = "linked_main_tx_id"
    private const val KEY_IS_FOREIGN = "is_foreign"
    private const val KEY_CURRENCY_CODE = "currency_code"
    private const val KEY_FOREIGN_AMOUNT = "foreign_amount"
    private const val KEY_EXCHANGE_RATE = "exchange_rate"
    private const val KEY_IS_RATE_CALCULATED = "is_rate_calculated"
    private const val KEY_EQUIVALENT_AMOUNT = "equivalent_amount"
    private const val KEY_BASE_CURRENCY_CODE = "base_currency_code"

    private const val KEY_DELETED_ITEMS = "deleted_items"
    private const val KEY_SOURCE_SYSTEM = "sourceSystem"
    private const val KEY_ORIGINAL_TABLE_NAME = "originalTableName"
    private const val KEY_JSON_DATA = "jsonData"
    private const val KEY_DELETED_AT = "deletedAt"

    private const val KEY_PINNED_CUSTOMER_IDS_BY_CATEGORY = "pinned_customer_ids_by_category"
    private const val KEY_CATEGORY_ORDER_LIST = "category_order_list"
    private const val KEY_CLOSED_CUSTOM_NAME = "closed_custom_name"

    private const val KEY_CUSTOM_CATEGORIES = "custom_categories"
    private const val KEY_TAB_TYPE = "tab_type"
    private const val KEY_ICON_EMOJI = "icon_emoji"
    private const val KEY_DISPLAY_ORDER = "display_order"
    private const val KEY_IS_SYSTEM_CLOSED = "is_system_closed"

    /**
     * Calculates cryptographic SHA-256 hash of any string payload for Zero-Diff audits.
     */
    fun calculateSha256Hash(input: String): String {
        val digest = MessageDigest.getInstance(ALGORITHM_SHA_256)
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        val hexChars = CharArray(hashBytes.size * 2)
        for (i in hashBytes.indices) {
            val v = hashBytes[i].toInt() and 0xFF
            hexChars[i * 2] = HEX_CHARS[v ushr 4]
            hexChars[i * 2 + 1] = HEX_CHARS[v and 0x0F]
        }
        return String(hexChars)
    }

    /**
     * حساب بصمة تكامل منطقية حتمية (Deterministic Logical Integrity Hash)
     * ترتب الكيانات منطقياً لضمان استقرار البصمة بغض النظر عن ترتيب المعالجة في الذاكرة.
     */
    fun calculateIntegrityHash(data: BackupPayloadData): String {
        val sb = StringBuilder()
        sb.append("settings:").append(data.settings.currencySymbol).append("|")
            .append(data.settings.schoolExpensesEnabled).append("|")
            .append(data.settings.exchangeRatesJson).append(";")

        sb.append("commitments:")
        data.commitments.sortedWith(compareBy({ it.orderIndex }, { it.name })).forEach {
            sb.append(it.name).append(",").append(it.targetAmount.toPlainString()).append(",")
                .append(it.currentProgress.toPlainString()).append(",")
                .append(it.orderIndex).append("|")
        }
        sb.append(";")

        sb.append("transactions:")
        data.transactions.sortedWith(compareBy({ it.timestamp }, { it.id })).forEach {
            sb.append(it.id).append(",").append(it.timestamp).append(",")
                .append(it.type).append(",").append(it.category).append(",")
                .append(it.amount.toPlainString()).append(",")
                .append(it.description).append("|")
        }
        sb.append(";")

        sb.append("customers:")
        data.habayebCustomers.sortedBy { it.id }.forEach {
            sb.append(it.id).append(",").append(it.name).append(",")
                .append(it.phone).append(",").append(it.initialType).append("|")
        }
        sb.append(";")

        sb.append("habayebTx:")
        data.habayebTransactions.sortedWith(compareBy({ it.timestamp }, { it.id })).forEach {
            sb.append(it.id).append(",").append(it.customerId).append(",")
                .append(it.type).append(",").append(it.amount.toPlainString()).append(",")
                .append(it.timestamp).append(",").append(it.currencyCode).append(",")
                .append(it.foreignAmount.toPlainString()).append(",")
                .append(it.linkedMainTxId ?: "").append("|")
        }
        sb.append(";")

        sb.append("deletedItems:")
        data.deletedItems.sortedBy { it.id }.forEach {
            sb.append(it.id).append(",").append(it.sourceSystem).append(",")
                .append(it.originalTableName).append(",").append(it.deletedAt).append("|")
        }
        sb.append(";")

        sb.append("customCategories:")
        data.customCategories.sortedWith(compareBy({ it.displayOrder }, { it.name })).forEach {
            sb.append(it.name).append(",").append(it.tabType).append(",")
                .append(it.displayOrder).append(",").append(it.isSystemClosed).append("|")
        }
        sb.append(";")

        data.categoryLinks.toSortedMap().forEach { (k, v) ->
            sb.append("catLink:").append(k).append("=").append(v).append(";")
        }

        data.pinnedCustomerIdsByCategory.toSortedMap().forEach { (k, set) ->
            sb.append("pinned:").append(k).append("=")
                .append(set.sorted().joinToString(",")).append(";")
        }

        return calculateSha256Hash(sb.toString())
    }

    /**
     * Streams full application payload data directly into a Writer using [android.util.JsonWriter]
     * to ensure constant memory footprint and prevent OutOfMemoryError.
     */
    fun exportBackupToWriter(data: BackupPayloadData, writer: java.io.Writer) {
        val jsonWriter = android.util.JsonWriter(writer)
        jsonWriter.beginObject()

        // Metadata
        jsonWriter.name(KEY_METADATA)
        jsonWriter.beginObject()
        jsonWriter.name(KEY_APP_NAME).value("Mizan Al-Dar")
        jsonWriter.name(KEY_APP_VERSION).value("1.1.0")
        jsonWriter.name(KEY_BACKUP_TIMESTAMP).value(System.currentTimeMillis() / 1000)
        jsonWriter.name(KEY_SECURITY_HASH).value(calculateIntegrityHash(data))
        jsonWriter.endObject()

        // Settings
        jsonWriter.name(KEY_SETTINGS)
        jsonWriter.beginObject()
        jsonWriter.name(KEY_CURRENCY_SYMBOL).value(data.settings.currencySymbol)
        jsonWriter.name(KEY_SCHOOL_EXPENSES_ENABLED).value(data.settings.schoolExpensesEnabled)
        jsonWriter.name(KEY_EXCHANGE_RATES_JSON).value(data.settings.exchangeRatesJson)
        jsonWriter.endObject()

        // Fixed Commitments
        jsonWriter.name(KEY_FIXED_COMMITMENTS)
        jsonWriter.beginArray()
        for (fc in data.commitments) {
            jsonWriter.beginObject()
            jsonWriter.name(KEY_NAME).value(fc.name)
            jsonWriter.name(KEY_TARGET_AMOUNT).value(fc.targetAmount.toPlainString())
            jsonWriter.name(KEY_CURRENT_PROGRESS).value(fc.currentProgress.toPlainString())
            jsonWriter.name(KEY_ORDER_INDEX).value(fc.orderIndex)
            jsonWriter.endObject()
        }
        jsonWriter.endArray()

        // Transactions
        jsonWriter.name(KEY_TRANSACTIONS)
        jsonWriter.beginArray()
        for (tx in data.transactions) {
            jsonWriter.beginObject()
            jsonWriter.name(KEY_ID).value(tx.id)
            jsonWriter.name(KEY_TIMESTAMP).value(tx.timestamp)
            jsonWriter.name(KEY_TYPE).value(tx.type)
            jsonWriter.name(KEY_CATEGORY).value(tx.category)
            jsonWriter.name(KEY_AMOUNT).value(tx.amount.toPlainString())
            jsonWriter.name(KEY_DESCRIPTION).value(tx.description)
            jsonWriter.endObject()
        }
        jsonWriter.endArray()

        // Habayeb Debts
        jsonWriter.name(KEY_HABAYEB_DEBTS)
        jsonWriter.beginObject()
        jsonWriter.name(KEY_CUSTOMERS)
        jsonWriter.beginArray()
        for (c in data.habayebCustomers) {
            jsonWriter.beginObject()
            jsonWriter.name(KEY_ID).value(c.id)
            jsonWriter.name(KEY_NAME).value(c.name)
            jsonWriter.name(KEY_PHONE).value(c.phone)
            jsonWriter.name(KEY_NOTES).value(c.notes)
            jsonWriter.name(KEY_CREATED_AT).value(c.createdAt)
            jsonWriter.name(KEY_INITIAL_TYPE).value(c.initialType)
            val catLink = data.categoryLinks[c.id]
            if (catLink != null) {
                jsonWriter.name(KEY_CATEGORY_LINK).value(catLink)
            }
            jsonWriter.endObject()
        }
        jsonWriter.endArray()

        jsonWriter.name(KEY_DEBT_TRANSACTIONS)
        jsonWriter.beginArray()
        for (t in data.habayebTransactions) {
            jsonWriter.beginObject()
            jsonWriter.name(KEY_ID).value(t.id)
            jsonWriter.name(KEY_CUSTOMER_ID).value(t.customerId)
            jsonWriter.name(KEY_TYPE).value(t.type)
            jsonWriter.name(KEY_AMOUNT).value(t.amount.toPlainString())
            jsonWriter.name(KEY_TIMESTAMP).value(t.timestamp)
            jsonWriter.name(KEY_DESCRIPTION).value(t.description)
            val cleanLinkedId = t.linkedMainTxId?.trim()?.takeIf { 
                it.isNotBlank() && !it.equals("null", ignoreCase = true) && it != "0" && it != t.id 
            }
            if (cleanLinkedId != null) {
                jsonWriter.name(KEY_LINKED_MAIN_TX_ID).value(cleanLinkedId)
            } else {
                jsonWriter.name(KEY_LINKED_MAIN_TX_ID).nullValue()
            }
            jsonWriter.name(KEY_IS_FOREIGN).value(t.isForeign)
            jsonWriter.name(KEY_CURRENCY_CODE).value(t.currencyCode)
            jsonWriter.name(KEY_FOREIGN_AMOUNT).value(t.foreignAmount.toPlainString())
            jsonWriter.name(KEY_EXCHANGE_RATE).value(t.exchangeRate.toPlainString())
            jsonWriter.name(KEY_IS_RATE_CALCULATED).value(t.isRateCalculated)
            jsonWriter.name(KEY_EQUIVALENT_AMOUNT).value(t.equivalentAmount.toPlainString())
            jsonWriter.name(KEY_BASE_CURRENCY_CODE).value(t.baseCurrencyCode)
            jsonWriter.endObject()
        }
        jsonWriter.endArray()
        jsonWriter.endObject()

        // Deleted Items
        jsonWriter.name(KEY_DELETED_ITEMS)
        jsonWriter.beginArray()
        for (di in data.deletedItems) {
            jsonWriter.beginObject()
            jsonWriter.name(KEY_ID).value(di.id)
            jsonWriter.name(KEY_SOURCE_SYSTEM).value(di.sourceSystem)
            jsonWriter.name(KEY_ORIGINAL_TABLE_NAME).value(di.originalTableName)
            jsonWriter.name(KEY_JSON_DATA).value(di.jsonData)
            jsonWriter.name(KEY_DELETED_AT).value(di.deletedAt)
            jsonWriter.endObject()
        }
        jsonWriter.endArray()

        // Pinned Customers
        if (data.pinnedCustomerIdsByCategory.isNotEmpty()) {
            jsonWriter.name(KEY_PINNED_CUSTOMER_IDS_BY_CATEGORY)
            jsonWriter.beginObject()
            for ((catKey, set) in data.pinnedCustomerIdsByCategory.toSortedMap()) {
                jsonWriter.name(catKey)
                jsonWriter.beginArray()
                set.sorted().forEach { jsonWriter.value(it) }
                jsonWriter.endArray()
            }
            jsonWriter.endObject()
        }

        if (data.categoryOrderList != null) {
            jsonWriter.name(KEY_CATEGORY_ORDER_LIST).value(data.categoryOrderList)
        }
        if (data.closedCustomName != null) {
            jsonWriter.name(KEY_CLOSED_CUSTOM_NAME).value(data.closedCustomName)
        }

        // Custom Categories
        if (data.customCategories.isNotEmpty()) {
            jsonWriter.name(KEY_CUSTOM_CATEGORIES)
            jsonWriter.beginArray()
            for (cc in data.customCategories) {
                jsonWriter.beginObject()
                jsonWriter.name(KEY_NAME).value(cc.name)
                jsonWriter.name(KEY_TAB_TYPE).value(cc.tabType)
                jsonWriter.name(KEY_ICON_EMOJI).value(cc.iconEmoji)
                jsonWriter.name(KEY_DISPLAY_ORDER).value(cc.displayOrder)
                jsonWriter.name(KEY_IS_SYSTEM_CLOSED).value(cc.isSystemClosed)
                jsonWriter.endObject()
            }
            jsonWriter.endArray()
        }

        jsonWriter.endObject()
        jsonWriter.flush()
    }

    /**
     * Streams full application payload directly to an OutputStream.
     */
    suspend fun exportBackupToStream(
        data: BackupPayloadData,
        outputStream: java.io.OutputStream
    ) = withContext(Dispatchers.IO) {
        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
            exportBackupToWriter(data, writer)
        }
    }

    /**
     * Streams full application payload directly to a local target File.
     */
    suspend fun exportBackupToFile(
        data: BackupPayloadData,
        targetFile: java.io.File
    ) = withContext(Dispatchers.IO) {
        java.io.FileOutputStream(targetFile).use { fos ->
            exportBackupToStream(data, fos)
        }
    }

    /**
     * Exports full application payload data container to a unified JSON string payload.
     */
    suspend fun exportBackupToJson(
        data: BackupPayloadData
    ): String = withContext(Dispatchers.IO) {
        val stringWriter = java.io.StringWriter()
        stringWriter.use { sw ->
            exportBackupToWriter(data, sw)
        }
        stringWriter.toString()
    }

    /**
     * Exports full application database state to a unified JSON string payload.
     * Backwards compatibility convenience method.
     */
    suspend fun exportBackupToJson(
        settings: AppSettings,
        commitments: List<FixedCommitment>,
        transactions: List<TransactionDb>,
        habayebCustomers: List<HabayebCustomer> = emptyList(),
        habayebTransactions: List<HabayebTransaction> = emptyList(),
        deletedItems: List<DeletedItemEntity> = emptyList(),
        context: Context? = null
    ): String = withContext(Dispatchers.IO) {
        val extraData = context?.let { fetchExtraBackupData(it, habayebCustomers) } ?: ExtraBackupData()
        val payloadData = BackupPayloadData(
            settings = settings,
            commitments = commitments,
            transactions = transactions,
            habayebCustomers = habayebCustomers,
            habayebTransactions = habayebTransactions,
            deletedItems = deletedItems,
            customCategories = extraData.customCategories,
            categoryLinks = extraData.categoryLinks,
            pinnedCustomerIdsByCategory = extraData.pinnedMap,
            categoryOrderList = extraData.categoryOrderList,
            closedCustomName = extraData.closedCustomName
        )
        exportBackupToJson(payloadData)
    }

    private suspend fun fetchExtraBackupData(
        context: Context,
        habayebCustomers: List<HabayebCustomer>
    ): ExtraBackupData = withContext(Dispatchers.IO) {
        val sharedPrefs = context.getSharedPreferences(PREF_MIZAN_SEC, Context.MODE_PRIVATE)
        val financePrefs = context.getSharedPreferences(PREF_MIZAN_FINANCE, Context.MODE_PRIVATE)

        val categoryLinks = mutableMapOf<String, String>()
        for (c in habayebCustomers) {
            val catLink = financePrefs?.getString("$PREFIX_CAT_LINK${c.id}", null)
                ?: sharedPrefs?.getString("$PREFIX_CAT_LINK${c.id}", null)
            if (catLink != null) {
                categoryLinks[c.id] = catLink
            }
        }

        val pinnedMap = mutableMapOf<String, Set<String>>()
        val combinedPrefs = mutableMapOf<String, Any?>()
        sharedPrefs?.all?.let { combinedPrefs.putAll(it) }
        financePrefs?.all?.let { combinedPrefs.putAll(it) }

        for ((key, value) in combinedPrefs) {
            if (key.startsWith(PREFIX_KEY_PINNED_IN)) {
                val catKey = key.removePrefix(PREFIX_KEY_PINNED_IN)
                if (value is Set<*>) {
                    @Suppress("UNCHECKED_CAST")
                    pinnedMap[catKey] = value as Set<String>
                }
            }
        }

        val catOrder = financePrefs?.getString(KEY_CATEGORY_ORDER_LIST_PREF, null)
            ?: sharedPrefs?.getString(KEY_CATEGORY_ORDER_LIST_PREF, null)
        val closedCustomName = financePrefs?.getString(KEY_CLOSED_CUSTOM_NAME_PREF, null)
            ?: sharedPrefs?.getString(KEY_CLOSED_CUSTOM_NAME_PREF, null)

        val customCategories = try {
            val db = AppDatabase.getDatabase(context)
            db.customCategoryDao().getAllCustomCategoriesFlow().first()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to include custom categories in backup payload", e)
            emptyList()
        }

        ExtraBackupData(
            categoryLinks = categoryLinks,
            pinnedMap = pinnedMap,
            categoryOrderList = catOrder,
            closedCustomName = closedCustomName,
            customCategories = customCategories
        )
    }

    private data class ExtraBackupData(
        val categoryLinks: Map<String, String> = emptyMap(),
        val pinnedMap: Map<String, Set<String>> = emptyMap(),
        val categoryOrderList: String? = null,
        val closedCustomName: String? = null,
        val customCategories: List<CustomCategory> = emptyList()
    )

    /**
     * استخراج قيمة BigDecimal من JSON بأمان ودقة تامة مع رفض التحويلات العائمة الفاقدة للدقة
     */
    fun getBigDecimal(obj: JSONObject, key: String, fallback: String = "0"): BigDecimal {
        if (!obj.has(key)) return BigDecimal(fallback)
        val raw = obj.opt(key) ?: return BigDecimal(fallback)
        if (raw is BigDecimal) return raw
        val valueStr = raw.toString().trim()
        if (valueStr.isEmpty() || valueStr.equals("null", ignoreCase = true)) {
            return BigDecimal(fallback)
        }
        val cleaned = BigDecimalConverter.cleanNumberString(valueStr)
        if (cleaned.isEmpty()) return BigDecimal(fallback)
        return try {
            BigDecimal(cleaned)
        } catch (_: Exception) {
            BigDecimal(fallback)
        }
    }

    /**
     * Imports and parses backup payload from JSON string into structured domain data models.
     */
    suspend fun importBackupFromJson(
        jsonString: String,
        context: Context? = null
    ): Triple<AppSettings, List<FixedCommitment>, List<TransactionDb>> = withContext(Dispatchers.IO) {
        val root = JSONObject(jsonString)
        val sourceObj = if (root.has(KEY_MIZAN_AL_DAR_DB)) root.getJSONObject(KEY_MIZAN_AL_DAR_DB) else root

        val settingsObj = sourceObj.optJSONObject(KEY_SETTINGS)
        val fallbackCurrency = context?.getString(com.example.R.string.currency_yer) ?: DatabaseDefaults.DEFAULT_CURRENCY_SYMBOL
        val settings = if (settingsObj != null) {
            AppSettings(
                currencySymbol = settingsObj.optString(KEY_CURRENCY_SYMBOL, fallbackCurrency),
                schoolExpensesEnabled = settingsObj.optBoolean(KEY_SCHOOL_EXPENSES_ENABLED, true),
                themeMode = 0,
                exchangeRatesJson = settingsObj.optString(KEY_EXCHANGE_RATES_JSON, "{}")
            )
        } else {
            AppSettings()
        }

        val commitmentsList = mutableListOf<FixedCommitment>()
        val commitmentsArr = sourceObj.optJSONArray(KEY_FIXED_COMMITMENTS)
        if (commitmentsArr != null) {
            for (i in 0 until commitmentsArr.length()) {
                val obj = commitmentsArr.getJSONObject(i)
                commitmentsList.add(
                    FixedCommitment(
                        name = obj.getString(KEY_NAME),
                        targetAmount = getBigDecimal(obj, KEY_TARGET_AMOUNT),
                        currentProgress = getBigDecimal(obj, KEY_CURRENT_PROGRESS),
                        orderIndex = obj.optInt(KEY_ORDER_INDEX, i)
                    )
                )
            }
        }

        val transactionsList = mutableListOf<TransactionDb>()
        val transactionsArr = sourceObj.optJSONArray(KEY_TRANSACTIONS)
        if (transactionsArr != null) {
            for (i in 0 until transactionsArr.length()) {
                val obj = transactionsArr.getJSONObject(i)
                transactionsList.add(
                    TransactionDb(
                        id = obj.getString(KEY_ID),
                        timestamp = obj.getLong(KEY_TIMESTAMP),
                        type = obj.getString(KEY_TYPE),
                        category = obj.getString(KEY_CATEGORY),
                        amount = getBigDecimal(obj, KEY_AMOUNT),
                        description = obj.optString(KEY_DESCRIPTION, "")
                    )
                )
            }
        }

        Triple(settings, commitmentsList, transactionsList)
    }
}


