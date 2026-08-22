package com.example.ui.screens.trash.utils

import androidx.compose.ui.graphics.Color
import com.example.data.local.entities.DeletedItemEntity
import com.example.data.local.entities.FixedCommitment
import com.example.data.local.entities.HabayebCustomer
import com.example.data.local.entities.HabayebTransaction
import com.example.data.local.entities.TransactionDb
import com.example.domain.model.TransactionType
import com.example.ui.helper.HabayebMathHelper
import com.example.ui.screens.habayeb.utils.HabayebDateFormatter
import com.example.ui.viewmodel.FinanceConstants
import java.math.BigDecimal
import java.util.Locale
import org.json.JSONObject

@androidx.compose.runtime.Immutable
data class ParsedBundleTransaction(
    val id: String,
    val type: String,
    val description: String,
    val hasNotes: Boolean,
    val amountDec: BigDecimal,
    val isNegative: Boolean,
    val dateText: String,
    val displayAmountText: String,
    val equivalentAmountText: String,
    val exchangeRateText: String
)

@androidx.compose.runtime.Immutable
data class TrashStrings(
    val systemHabayeb: String,
    val unknownText: String,
    val noPhoneText: String,
    val noNotesText: String,
    val debtTxText: String,
    val owedByThemText: String,
    val paymentByThemText: String,
    val owedToThemText: String,
    val paymentToThemText: String,
    val categoryLabelText: String,
    val customerLabelText: String,
    val equivalentInfoTemplate: String,
    val progressTextTemplate: String,
    val customerBundleDescTemplate: String,
    val ledgerBundleDescTemplate: String,
    val exchangeRateLabelText: String
)

@androidx.compose.runtime.Immutable
data class ParsedTrashData(
    val name: String,
    val amount: BigDecimal,
    val searchableText: String,
    val titleText: String,
    val amountText: String,
    val isExpense: Boolean,
    val subText: String,
    val exchangeInfoText: String,
    val indicatorColor: Color,
    val bundleTransactions: List<ParsedBundleTransaction>,
    val parsedDate: String,
    val phone: String = "",
    val isForeign: Boolean = false,
    val isRateCalculated: Boolean = false,
    val exchangeRateVal: String = "",
    val currencyBreakdown: Map<String, BigDecimal> = emptyMap(),
    val customerName: String = "",
    val customerPhone: String = "",
    val txOriginalDate: String = "",
    val txTypeDisplay: String = "",
    val rawDescription: String = "",
    val equivalentAmountText: String = "",
    val baseCurrencyCode: String = ""
)

object TrashItemParser {

    fun stripCurrencyTag(text: String): String {
        if (text.isBlank()) return ""
        return text.replace(Regex("""^\s*(\[[^\]]*\]\s*)+"""), "").trim()
    }

    fun parseBigDecimal(obj: JSONObject, key: String, fallback: String = "0"): BigDecimal {
        if (!obj.has(key)) return BigDecimal(fallback)
        val valueStr = obj.optString(key, null)
        if (!valueStr.isNullOrBlank() && valueStr != "null") {
            try {
                return BigDecimal(valueStr.trim())
            } catch (_: Exception) {}
        }
        val doubleVal = obj.optDouble(key, 0.0)
        return try {
            BigDecimal.valueOf(doubleVal)
        } catch (_: Exception) {
            BigDecimal(fallback)
        }
    }

    fun parseHabayebCustomer(custData: JSONObject): HabayebCustomer {
        return HabayebCustomer(
            id = custData.getString("id"),
            name = custData.getString("name"),
            phone = custData.optString("phone", ""),
            notes = custData.optString("notes", ""),
            createdAt = custData.optLong("createdAt", System.currentTimeMillis()),
            initialType = custData.optString("initialType", custData.optString("initial_type", TransactionType.OWED_BY_THEM.value))
        )
    }

    fun parseHabayebTransaction(txObj: JSONObject): HabayebTransaction {
        val linkedId = if (txObj.has("linkedMainTxId") && !txObj.isNull("linkedMainTxId")) {
            txObj.getString("linkedMainTxId")
        } else null
        return HabayebTransaction(
            id = txObj.getString("id"),
            customerId = txObj.getString("customerId"),
            type = txObj.getString("type"),
            amount = parseBigDecimal(txObj, "amount"),
            timestamp = txObj.optLong("timestamp", System.currentTimeMillis()),
            description = txObj.optString("description", ""),
            linkedMainTxId = linkedId,
            isForeign = txObj.optBoolean("is_foreign", false),
            currencyCode = txObj.optString("currency_code", FinanceConstants.DEFAULT_CURRENCY_CODE),
            foreignAmount = parseBigDecimal(txObj, "foreign_amount"),
            exchangeRate = parseBigDecimal(txObj, "exchange_rate", "1"),
            isRateCalculated = txObj.optBoolean("is_rate_calculated", false),
            equivalentAmount = parseBigDecimal(txObj, "equivalent_amount"),
            baseCurrencyCode = txObj.optString("base_currency_code", FinanceConstants.DEFAULT_CURRENCY_CODE)
        )
    }

    fun parseFixedCommitment(fcObj: JSONObject): FixedCommitment {
        return FixedCommitment(
            name = fcObj.getString("name"),
            targetAmount = parseBigDecimal(fcObj, "targetAmount"),
            currentProgress = parseBigDecimal(fcObj, "currentProgress"),
            orderIndex = fcObj.optInt("orderIndex", 0)
        )
    }

    fun parseTransactionDb(txObj: JSONObject): TransactionDb {
        return TransactionDb(
            id = txObj.getString("id"),
            timestamp = txObj.optLong("timestamp", System.currentTimeMillis() / 1000),
            type = txObj.getString("type"),
            category = txObj.optString("category", ""),
            amount = parseBigDecimal(txObj, "amount"),
            description = txObj.optString("description", "")
        )
    }

    fun parse(
        item: DeletedItemEntity,
        customersList: List<HabayebCustomer>,
        currencySymbol: String,
        strings: TrashStrings,
        primaryColor: Color,
        secondaryColor: Color,
        errorColor: Color,
        outlineColor: Color
    ): ParsedTrashData {
        val parsedDate = HabayebDateFormatter.formatFullDateTime(item.deletedAt)

        val systemColor = if (item.sourceSystem == strings.systemHabayeb) secondaryColor else primaryColor

        var name = strings.unknownText
        var amountDec = BigDecimal.ZERO
        var titleText = strings.unknownText
        var amountText = ""
        var isExpense = false
        var subText = ""
        var exchangeInfoText = ""
        var indicatorColor = systemColor
        val bundleTxs = mutableListOf<ParsedBundleTransaction>()
        val searchTokens = mutableListOf<String>()

        var phoneVal = ""
        var isForeignVal = false
        var isRateCalculatedVal = false
        var exchangeRateVal = ""
        val currencyBreakdownVal = mutableMapOf<String, BigDecimal>()
        var customerNameVal = ""
        var customerPhoneVal = ""
        var txOriginalDateVal = ""
        var txTypeDisplayVal = ""
        var rawDescriptionVal = ""
        var equivalentAmountTextVal = ""
        var baseCurrencyCodeVal = ""

        try {
            val jsonObj = JSONObject(item.jsonData)

            when (item.originalTableName) {
                "habayeb_transactions" -> {
                    val customerId = jsonObj.optString("customerId", "")
                    val foundCustomer = customersList.find { it.id == customerId }
                    val resolvedName = foundCustomer?.name ?: ""
                    phoneVal = foundCustomer?.phone ?: ""
                    customerNameVal = resolvedName
                    customerPhoneVal = phoneVal

                    val origTimestamp = jsonObj.optLong("timestamp", 0L)
                    if (origTimestamp > 0) {
                        val millis = if (origTimestamp < 10000000000L) origTimestamp * 1000 else origTimestamp
                        txOriginalDateVal = HabayebDateFormatter.formatFullDateTime(millis)
                    }

                    val desc = jsonObj.optString("description", "").trim()
                    val notes = jsonObj.optString("notes", "").trim()
                    val rawText = desc.ifEmpty { notes }
                    rawDescriptionVal = rawText
                    val cleanText = stripCurrencyTag(rawText)
                    titleText = cleanText.ifEmpty { strings.noNotesText }
                    name = if (resolvedName.isNotEmpty()) "$resolvedName - $titleText" else titleText

                    val amountStr = jsonObj.optString("amount", "0")
                    amountDec = HabayebMathHelper.toBigDecimal(amountStr)

                    val type = jsonObj.optString("type", "")
                    val isNegative = type == "OWED_BY_THEM" || type == "PAYMENT_TO_THEM"
                    isExpense = isNegative
                    indicatorColor = if (isExpense) errorColor else secondaryColor

                    val typeAr = when (type) {
                        "OWED_BY_THEM" -> strings.owedByThemText
                        "PAYMENT_BY_THEM" -> strings.paymentByThemText
                        "OWED_TO_THEM" -> strings.owedToThemText
                        "PAYMENT_TO_THEM" -> strings.paymentToThemText
                        else -> type
                    }
                    txTypeDisplayVal = typeAr

                    val displayName = if (resolvedName.isNotEmpty()) resolvedName else strings.unknownText
                    subText = try {
                        String.format(Locale.getDefault(), strings.customerLabelText, "$displayName ($typeAr)")
                    } catch (e: Exception) {
                        "تابع لحساب: $displayName ($typeAr)"
                    }

                    isForeignVal = jsonObj.optBoolean("is_foreign", false)
                    val rawCurrencyCode = jsonObj.optString("currency_code", "DEFAULT")
                    val effectiveCurrency = if (rawCurrencyCode == "DEFAULT" || rawCurrencyCode.isBlank()) currencySymbol else rawCurrencyCode

                    if (isForeignVal) {
                        val foreignAmountStr = jsonObj.optString("foreign_amount", "0")
                        val foreignAmountDec = HabayebMathHelper.toBigDecimal(foreignAmountStr)
                        amountText = "${HabayebMathHelper.formatSmart(foreignAmountDec)} $effectiveCurrency"

                        isRateCalculatedVal = jsonObj.optBoolean("is_rate_calculated", false)
                        if (isRateCalculatedVal) {
                            val equivalentValStr = jsonObj.optString("equivalent_amount", "0")
                            val equivalentValDec = HabayebMathHelper.toBigDecimal(equivalentValStr)
                            val baseCurrencyRaw = jsonObj.optString("base_currency_code", "DEFAULT")
                            val baseCurrencyCode = if (baseCurrencyRaw == "DEFAULT" || baseCurrencyRaw.isBlank()) currencySymbol else baseCurrencyRaw
                            baseCurrencyCodeVal = baseCurrencyCode
                            val rateStr = jsonObj.optString("exchange_rate", "1.0")
                            val rateDec = HabayebMathHelper.toBigDecimal(rateStr)
                            exchangeRateVal = HabayebMathHelper.formatSmart(rateDec)
                            equivalentAmountTextVal = "${HabayebMathHelper.formatSmart(equivalentValDec)} $baseCurrencyCode"
                            exchangeInfoText = String.format(
                                Locale.getDefault(),
                                strings.equivalentInfoTemplate,
                                equivalentAmountTextVal,
                                exchangeRateVal
                            )
                            val signedEq = if (isNegative) equivalentValDec.negate() else equivalentValDec
                            currencyBreakdownVal[baseCurrencyCode] = signedEq
                        } else {
                            val signedForeign = if (isNegative) foreignAmountDec.negate() else foreignAmountDec
                            currencyBreakdownVal[effectiveCurrency] = signedForeign
                        }
                    } else {
                        amountText = "${HabayebMathHelper.formatSmart(amountDec)} $effectiveCurrency"
                        val signedAmt = if (isNegative) amountDec.negate() else amountDec
                        currencyBreakdownVal[effectiveCurrency] = signedAmt
                    }

                    searchTokens.add(desc)
                    searchTokens.add(notes)
                    searchTokens.add(resolvedName)
                    searchTokens.add(typeAr)
                    searchTokens.add(amountStr)
                    searchTokens.add(effectiveCurrency)
                }

                "habayeb_customers" -> {
                    val rawName = jsonObj.optString("name", "").trim()
                    titleText = rawName.ifEmpty { strings.unknownText }
                    name = titleText

                    phoneVal = jsonObj.optString("phone", "").trim()
                    amountText = ""

                    val notesStr = jsonObj.optString("notes", "").trim()
                    subText = if (notesStr.isNotEmpty()) notesStr else strings.noNotesText
                    indicatorColor = secondaryColor

                    searchTokens.add(rawName)
                    searchTokens.add(phoneVal)
                    searchTokens.add(notesStr)
                }

                "habayeb_bundle" -> {
                    val custObj = jsonObj.optJSONObject("customer")
                    var custName = ""
                    var custPhone = ""
                    if (custObj != null) {
                        custName = custObj.optString("name", "").trim()
                        custPhone = custObj.optString("phone", "").trim()
                    }
                    if (custName.isEmpty()) {
                        custName = jsonObj.optString("customerName", "").trim()
                    }

                    titleText = custName.ifEmpty { strings.unknownText }
                    name = titleText
                    phoneVal = custPhone

                    val txsArray = jsonObj.optJSONArray("transactions")
                    val txCount = txsArray?.length() ?: 0
                    subText = try {
                        String.format(Locale.getDefault(), strings.customerBundleDescTemplate, txCount)
                    } catch (e: Exception) {
                        "عدد المعاملات: $txCount"
                    }
                    indicatorColor = secondaryColor

                    searchTokens.add(custName)
                    searchTokens.add(custPhone)

                    if (txsArray != null) {
                        val currencySums = mutableMapOf<String, BigDecimal>()

                        for (i in 0 until txsArray.length()) {
                            val txObj = txsArray.getJSONObject(i)
                            val txId = txObj.getString("id")
                            val txType = txObj.getString("type")
                            val rawDesc = txObj.optString("description", "").trim()
                            val cleanDesc = stripCurrencyTag(rawDesc)
                            val hasNotes = cleanDesc.isNotEmpty()
                            val txDesc = if (hasNotes) cleanDesc else strings.noNotesText
                            val txAmountStr = txObj.optString("amount", "0")
                            val txAmountDec = HabayebMathHelper.toBigDecimal(txAmountStr)
                            val txIsNegative = txType == "OWED_BY_THEM" || txType == "PAYMENT_TO_THEM"

                            val rawTimestamp = txObj.optLong("timestamp", 0L)
                            val txDateStr = HabayebDateFormatter.formatFullDateTime(rawTimestamp)

                            val txIsForeign = txObj.optBoolean("is_foreign", false)
                            val txIsRateCalculated = txObj.optBoolean("is_rate_calculated", false)

                            val effectiveCurrency: String
                            val effectiveAmountDec: BigDecimal

                            if (txIsForeign && txIsRateCalculated) {
                                val baseCurrencyRaw = txObj.optString("base_currency_code", "DEFAULT")
                                effectiveCurrency = if (baseCurrencyRaw == "DEFAULT" || baseCurrencyRaw.isBlank()) currencySymbol else baseCurrencyRaw
                                val equivStr = txObj.optString("equivalent_amount", "0")
                                val equivDec = HabayebMathHelper.toBigDecimal(equivStr)
                                effectiveAmountDec = if (equivDec.compareTo(BigDecimal.ZERO) > 0) equivDec else txAmountDec
                            } else if (txIsForeign) {
                                val currCode = txObj.optString("currency_code", "DEFAULT")
                                effectiveCurrency = if (currCode == "DEFAULT" || currCode.isBlank()) currencySymbol else currCode
                                val foreignAmountStr = txObj.optString("foreign_amount", "0")
                                val foreignAmountDec = HabayebMathHelper.toBigDecimal(foreignAmountStr)
                                effectiveAmountDec = if (foreignAmountDec.compareTo(BigDecimal.ZERO) > 0) foreignAmountDec else txAmountDec
                            } else {
                                val currCode = txObj.optString("currency_code", "DEFAULT")
                                effectiveCurrency = if (currCode == "DEFAULT" || currCode.isBlank()) currencySymbol else currCode
                                effectiveAmountDec = txAmountDec
                            }

                            val signedEffective = if (txIsNegative) effectiveAmountDec.negate() else effectiveAmountDec
                            val prevSum = currencySums.getOrDefault(effectiveCurrency, BigDecimal.ZERO)
                            currencySums[effectiveCurrency] = prevSum.add(signedEffective)

                            val displayAmountStr = if (txIsForeign && !txIsRateCalculated) {
                                "${HabayebMathHelper.formatSmart(effectiveAmountDec)} $effectiveCurrency"
                            } else if (txIsForeign && txIsRateCalculated) {
                                val foreignAmountStr = txObj.optString("foreign_amount", "0")
                                val foreignAmountDec = HabayebMathHelper.toBigDecimal(foreignAmountStr)
                                val currCode = txObj.optString("currency_code", "DEFAULT")
                                val displayCurr = if (currCode == "DEFAULT" || currCode.isBlank()) currencySymbol else currCode
                                "${HabayebMathHelper.formatSmart(foreignAmountDec)} $displayCurr"
                            } else {
                                "${HabayebMathHelper.formatSmart(txAmountDec)} $effectiveCurrency"
                            }

                            var equivStr = ""
                            var rateStr = ""
                            if (txIsForeign && txIsRateCalculated) {
                                val formattedEquiv = HabayebMathHelper.formatSmart(effectiveAmountDec)
                                val exchangeRateStr = txObj.optString("exchange_rate", "1.0")
                                val exchangeRateDec = HabayebMathHelper.toBigDecimal(exchangeRateStr)
                                val formattedRate = HabayebMathHelper.formatSmart(exchangeRateDec)
                                equivStr = "($formattedEquiv $effectiveCurrency)"
                                rateStr = formattedRate
                            }

                            bundleTxs.add(
                                ParsedBundleTransaction(
                                    id = txId,
                                    type = txType,
                                    description = txDesc,
                                    hasNotes = hasNotes,
                                    amountDec = effectiveAmountDec,
                                    isNegative = txIsNegative,
                                    dateText = txDateStr,
                                    displayAmountText = displayAmountStr,
                                    equivalentAmountText = equivStr,
                                    exchangeRateText = rateStr
                                )
                            )

                            searchTokens.add(cleanDesc)
                            searchTokens.add(txAmountStr)
                            searchTokens.add(effectiveCurrency)
                        }

                        currencyBreakdownVal.putAll(currencySums)

                        val nonZeroSums = currencySums.filter { it.value.compareTo(BigDecimal.ZERO) != 0 }
                        if (nonZeroSums.isNotEmpty()) {
                            val summaryParts = nonZeroSums.map { (curr, sum) ->
                                "${HabayebMathHelper.formatSmart(sum.abs())} $curr"
                            }
                            amountText = summaryParts.joinToString(" • ")
                            val firstVal = nonZeroSums.values.first()
                            isExpense = firstVal < BigDecimal.ZERO
                            amountDec = firstVal
                        } else {
                            amountText = ""
                        }
                    } else {
                        amountText = ""
                    }
                }

                else -> {
                    val rawName = jsonObj.optString("name", jsonObj.optString("description", "")).trim()
                    titleText = rawName.ifEmpty { strings.unknownText }
                    name = titleText
                    indicatorColor = outlineColor

                    searchTokens.add(rawName)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val cleanSearchableText = searchTokens.filter { it.isNotBlank() }.joinToString(" ")

        return ParsedTrashData(
            name = name,
            amount = amountDec,
            searchableText = cleanSearchableText,
            titleText = titleText,
            amountText = amountText,
            isExpense = isExpense,
            subText = subText,
            exchangeInfoText = exchangeInfoText,
            indicatorColor = indicatorColor,
            bundleTransactions = bundleTxs,
            parsedDate = parsedDate,
            phone = phoneVal,
            isForeign = isForeignVal,
            isRateCalculated = isRateCalculatedVal,
            exchangeRateVal = exchangeRateVal,
            currencyBreakdown = currencyBreakdownVal,
            customerName = customerNameVal,
            customerPhone = customerPhoneVal,
            txOriginalDate = txOriginalDateVal,
            txTypeDisplay = txTypeDisplayVal,
            rawDescription = rawDescriptionVal,
            equivalentAmountText = equivalentAmountTextVal,
            baseCurrencyCode = baseCurrencyCodeVal
        )
    }
}
