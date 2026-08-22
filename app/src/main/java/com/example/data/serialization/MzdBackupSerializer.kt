package com.example.data.serialization

import android.content.Context
import com.example.data.local.entities.AppSettings
import com.example.data.local.entities.CustomCategory
import com.example.data.local.entities.DeletedItemEntity
import com.example.data.local.entities.FixedCommitment
import com.example.data.local.entities.HabayebCustomer
import com.example.data.local.entities.HabayebTransaction
import com.example.data.local.entities.TransactionDb
import com.example.domain.model.TransactionType
import com.example.ui.navigation.Screen
import com.example.ui.viewmodel.FinanceConstants
import org.json.JSONObject
import java.math.BigDecimal

/**
 * محول النسخ الاحتياطية ودعم التوافق مع الإصدارات السابقة (Backward Compatibility & Serialization Adapter)
 *
 * القواعد والحدود المعمارية الحاكمة:
 * 1. قدسية خيارات المستخدم الصريحة (Explicit User Intent): إذا احتوت النسخة الاحتياطية على قيمة صريحة لحقل
 *    (مثل initial_type للعميل)، يُمنع منعاً باتاً إعادة اشتقاقها أو استنتاجها من المعاملات؛ لأن ذلك يؤدي إلى
 *    تغيير تصنيف العميل وتشويه تفضيل المستخدم بعد الاستعادة.
 * 2. التراجع الآمن للإصدارات القديمة (Graceful Legacy Fallback): يُسمح بالاشتقاق المنطقي فقط عند غياب الحقل
 *    تماماً من النسخ الاحتياطية القديمة جداً (Legacy Backups v1).
 * 3. الفصل التام بين التحليل النحوي (Parsing) والتحقق المنطقي (Validation) وقواعد البيانات (Transactions).
 * 4. الحفاظ على توافق الحقول القديمة (مثل habayeb_debts_db و customerId) لمنع كسر استعادة ملفات النسخ السابقة.
 */
object MzdBackupSerializer {

    suspend fun exportBackupToJson(
        settings: AppSettings,
        commitments: List<FixedCommitment>,
        transactions: List<TransactionDb>,
        habayebCustomers: List<HabayebCustomer> = emptyList(),
        habayebTransactions: List<HabayebTransaction> = emptyList(),
        deletedItems: List<DeletedItemEntity> = emptyList(),
        context: Context? = null
    ): String = BackupPayloadSerializer.exportBackupToJson(
        settings, commitments, transactions, habayebCustomers, habayebTransactions, deletedItems, context
    )

    suspend fun exportBackupToFile(
        settings: AppSettings,
        commitments: List<FixedCommitment>,
        transactions: List<TransactionDb>,
        habayebCustomers: List<HabayebCustomer> = emptyList(),
        habayebTransactions: List<HabayebTransaction> = emptyList(),
        deletedItems: List<DeletedItemEntity> = emptyList(),
        context: Context,
        targetFile: java.io.File
    ) {
        val jsonStr = BackupPayloadSerializer.exportBackupToJson(
            settings, commitments, transactions, habayebCustomers, habayebTransactions, deletedItems, context
        )
        targetFile.writeText(jsonStr)
    }

    fun getBigDecimal(obj: JSONObject, key: String, fallback: String = "0"): BigDecimal =
        BackupPayloadSerializer.getBigDecimal(obj, key, fallback)

    suspend fun importBackupFromJson(
        jsonString: String,
        context: Context? = null
    ): Triple<AppSettings, List<FixedCommitment>, List<TransactionDb>> =
        BackupPayloadSerializer.importBackupFromJson(jsonString, context)

    fun parseCustomCategories(root: JSONObject): List<CustomCategory> {
        val list = mutableListOf<CustomCategory>()
        if (root.has("custom_categories") && !root.isNull("custom_categories")) {
            val catsArr = root.optJSONArray("custom_categories")
            if (catsArr != null) {
                for (i in 0 until catsArr.length()) {
                    val obj = catsArr.getJSONObject(i)
                    list.add(
                        CustomCategory(
                            name = obj.getString("name"),
                            tabType = obj.optString("tab_type", Screen.HABAYEB.name),
                            iconEmoji = obj.optString("icon_emoji", ""),
                            displayOrder = obj.optInt("display_order", i),
                            isSystemClosed = obj.optBoolean("is_system_closed", false)
                        )
                    )
                }
            }
        }
        return list
    }

    fun parseDeletedItems(root: JSONObject): List<DeletedItemEntity> {
        val list = mutableListOf<DeletedItemEntity>()
        if (root.has("deleted_items") && !root.isNull("deleted_items")) {
            val deletedItemsArr = root.optJSONArray("deleted_items")
            if (deletedItemsArr != null) {
                for (i in 0 until deletedItemsArr.length()) {
                    val obj = deletedItemsArr.getJSONObject(i)
                    list.add(
                        DeletedItemEntity(
                            id = obj.getString("id"),
                            sourceSystem = obj.getString("sourceSystem"),
                            originalTableName = obj.getString("originalTableName"),
                            jsonData = obj.getString("jsonData"),
                            deletedAt = obj.getLong("deletedAt")
                        )
                    )
                }
            }
        }
        return list
    }

    data class RestoredHabayebCustomerData(
        val customer: HabayebCustomer,
        val categoryLink: String?
    )

    fun parseHabayebCustomers(root: JSONObject): List<RestoredHabayebCustomerData> {
        val jsonHabayebObj = root.optJSONObject("habayeb_debts")
            ?: root.optJSONObject("habayeb_debts_db")

        val txArr = jsonHabayebObj?.optJSONArray("debt_transactions")
            ?: jsonHabayebObj?.optJSONArray("habayeb_transactions")

        val customerIdToTxTypes = mutableMapOf<String, MutableSet<String>>()
        if (txArr != null) {
            for (i in 0 until txArr.length()) {
                val obj = txArr.getJSONObject(i)
                val cId = obj.optString("customer_id", obj.optString("customerId", "")).trim()
                val tType = obj.optString("type", "").trim()
                if (cId.isNotEmpty() && tType.isNotEmpty()) {
                    customerIdToTxTypes.getOrPut(cId) { mutableSetOf() }.add(tType)
                }
            }
        }

        val custArr = jsonHabayebObj?.optJSONArray("customers")
            ?: jsonHabayebObj?.optJSONArray("habayeb_customers")

        val result = mutableListOf<RestoredHabayebCustomerData>()
        if (custArr != null) {
            for (i in 0 until custArr.length()) {
                val obj = custArr.getJSONObject(i)
                val cId = obj.optString("id", obj.optString("customer_id", "")).trim()

                // التحقق من وجود قيمة صريحة لـ initial_type في ملف النسخ الاحتياطي
                val explicitInitialType = when {
                    obj.has("initial_type") && !obj.isNull("initial_type") -> obj.optString("initial_type").trim()
                    obj.has("initialType") && !obj.isNull("initialType") -> obj.optString("initialType").trim()
                    else -> ""
                }

                val determinedInitialType = if (explicitInitialType.isNotBlank()) {
                    // الالتزام التام بالقيمة الصريحة المحفوظة من المستخدم وعدم إعادة اشتقاقها
                    explicitInitialType
                } else {
                    // التراجع للاشتقاق فقط للنسخ القديمة التي لا تحتوي على الحقل
                    val txTypesForCust = customerIdToTxTypes[cId]
                    if (txTypesForCust != null && txTypesForCust.isNotEmpty()) {
                        if (txTypesForCust.contains(TransactionType.OWED_TO_THEM.value) || txTypesForCust.contains(TransactionType.PAYMENT_TO_THEM.value)) {
                            TransactionType.OWED_TO_THEM.value
                        } else if (txTypesForCust.contains(TransactionType.OWED_BY_THEM.value) || txTypesForCust.contains(TransactionType.PAYMENT_BY_THEM.value)) {
                            TransactionType.OWED_BY_THEM.value
                        } else {
                            TransactionType.OWED_BY_THEM.value
                        }
                    } else {
                        TransactionType.OWED_BY_THEM.value
                    }
                }

                val customer = HabayebCustomer(
                    id = cId,
                    name = obj.getString("name"),
                    phone = obj.optString("phone", ""),
                    notes = obj.optString("notes", ""),
                    createdAt = obj.optLong("created_at", obj.optLong("createdAt", System.currentTimeMillis() / 1000)),
                    initialType = determinedInitialType
                )

                val catLink = if (obj.has("category_link")) obj.getString("category_link") else null
                result.add(RestoredHabayebCustomerData(customer, catLink))
            }
        }
        return result
    }

    fun parseHabayebTransactions(root: JSONObject, defaultCurrencySymbol: String): List<HabayebTransaction> {
        val jsonHabayebObj = root.optJSONObject("habayeb_debts")
            ?: root.optJSONObject("habayeb_debts_db")

        val txArr = jsonHabayebObj?.optJSONArray("debt_transactions")
            ?: jsonHabayebObj?.optJSONArray("habayeb_transactions")

        val result = mutableListOf<HabayebTransaction>()
        if (txArr != null) {
            for (i in 0 until txArr.length()) {
                val obj = txArr.getJSONObject(i)
                val amtVal = getBigDecimal(obj, "amount")
                val isForeign = obj.optBoolean("is_foreign", false)
                var currencyCode = obj.optString("currency_code", obj.optString("currencyCode", FinanceConstants.DEFAULT_CURRENCY_CODE))
                if (currencyCode == FinanceConstants.DEFAULT_CURRENCY_CODE || currencyCode.isBlank()) {
                    currencyCode = defaultCurrencySymbol
                }

                val foreignAmountVal = if (obj.has("foreign_amount")) {
                    getBigDecimal(obj, "foreign_amount")
                } else {
                    amtVal
                }

                val exchangeRateVal = if (obj.has("exchange_rate")) {
                    getBigDecimal(obj, "exchange_rate", "1")
                } else {
                    BigDecimal.ONE
                }
                val isRateCalculated = obj.optBoolean("is_rate_calculated", false)

                val equivalentAmountVal = if (obj.has("equivalent_amount")) {
                    getBigDecimal(obj, "equivalent_amount")
                } else {
                    amtVal
                }

                val rawLinkedId = when {
                    obj.has("linked_main_tx_id") && !obj.isNull("linked_main_tx_id") -> obj.optString("linked_main_tx_id", "").trim()
                    obj.has("linkedMainTxId") && !obj.isNull("linkedMainTxId") -> obj.optString("linkedMainTxId", "").trim()
                    else -> null
                }
                val txId = obj.getString("id").trim()
                val cleanLinkedId = if (rawLinkedId.isNullOrBlank() || rawLinkedId.equals("null", ignoreCase = true) || rawLinkedId == "0" || rawLinkedId == txId) null else rawLinkedId

                result.add(
                    HabayebTransaction(
                        id = txId,
                        customerId = obj.optString("customer_id", obj.optString("customerId", "")).trim(),
                        type = obj.getString("type"),
                        amount = amtVal,
                        timestamp = obj.getLong("timestamp"),
                        description = obj.optString("description", ""),
                        linkedMainTxId = cleanLinkedId,
                        isForeign = isForeign,
                        currencyCode = currencyCode,
                        foreignAmount = foreignAmountVal,
                        exchangeRate = exchangeRateVal,
                        isRateCalculated = isRateCalculated,
                        equivalentAmount = equivalentAmountVal,
                        baseCurrencyCode = obj.optString("base_currency_code", FinanceConstants.DEFAULT_CURRENCY_CODE)
                    )
                )
            }
        }
        return result
    }
}


