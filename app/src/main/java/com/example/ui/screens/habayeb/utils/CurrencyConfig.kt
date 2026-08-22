package com.example.ui.screens.habayeb.utils

import android.content.Context
import com.example.R
import com.example.data.local.BigDecimalConverter
import com.example.data.local.entities.HabayebTransaction
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Collections
import java.util.LinkedHashMap
import java.util.Locale

data class Currency(
    val code: String,
    val symbol: String,
    val arabicName: String,
    val flagEmoji: String
)

object CurrencyConfig {
    
    private val converter = BigDecimalConverter()

    fun parseBigDecimal(value: String): BigDecimal {
        return converter.fromString(value) ?: BigDecimal.ZERO
    }

    val currencies = listOf(
        Currency("YER", "ر.ي", "ريال يمني", "🇾🇪"),
        Currency("SAR", "ر.س", "ريال سعودي", "🇸🇦"),
        Currency("USD", "$", "دولار أمريكي", "🇺🇸")
    )

    fun getCurrencies(context: Context? = null): List<Currency> {
        if (context == null) return currencies
        val yerSym = context.getString(R.string.currency_yer)
        val yerName = context.getString(R.string.currency_name_yer)
        val sarSym = context.getString(R.string.currency_sar)
        val sarName = context.getString(R.string.currency_name_sar)
        val usdSym = context.getString(R.string.currency_usd)
        val usdName = context.getString(R.string.currency_name_usd)

        return listOf(
            Currency("YER", yerSym.ifEmpty { "ر.ي" }, yerName.ifEmpty { "ريال يمني" }, "🇾🇪"),
            Currency("SAR", sarSym.ifEmpty { "ر.س" }, sarName.ifEmpty { "ريال سعودي" }, "🇸🇦"),
            Currency("USD", usdSym.ifEmpty { "$" }, usdName.ifEmpty { "دولار أمريكي" }, "🇺🇸")
        )
    }

    fun getBySymbol(symbol: String): Currency? =
        currencies.find { it.symbol == symbol || it.code == symbol }

    fun getByCode(code: String): Currency? =
        currencies.find { it.code == code }

    private const val MAX_CACHE_SIZE = 500
    private val parseCache: MutableMap<String, Pair<String, String>> = Collections.synchronizedMap(
        object : LinkedHashMap<String, Pair<String, String>>(MAX_CACHE_SIZE, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<String, String>>?): Boolean {
                return size > MAX_CACHE_SIZE
            }
        }
    )

    /**
     * Cleans transaction description by stripping hidden currency tags like [$currencyCode] or [$currencySymbol]
     * and trimming whitespace. Returns an empty string if description is empty or contained only currency tags.
     */
    fun getCleanDetails(description: String): String {
        if (description.isBlank()) return ""
        var clean = description.trim()
        for (currency in currencies) {
            val tagSym = "[${currency.symbol}]"
            val tagCode = "[${currency.code}]"
            if (clean.startsWith(tagSym)) {
                clean = clean.substring(tagSym.length).trim()
            }
            if (clean.startsWith(tagCode)) {
                clean = clean.substring(tagCode.length).trim()
            }
        }
        if (clean.startsWith("[") && clean.contains("]")) {
            val closingIdx = clean.indexOf("]")
            if (closingIdx in 1..10) {
                clean = clean.substring(closingIdx + 1).trim()
            }
        }
        return clean
    }

    /**
     * Extracts the currency symbol and clean description from a transaction's description.
     * If no currency is tagged, returns the provided defaultCurrencySymbol.
     */
    fun parseTransactionCurrency(description: String, defaultCurrencySymbol: String): Pair<String, String> {
        val cacheKey = "$description::$defaultCurrencySymbol"
        val cached = parseCache[cacheKey]
        if (cached != null) return cached

        // Look for [Symbol] pattern at the beginning
        for (currency in currencies) {
            val tag = "[${currency.symbol}]"
            if (description.startsWith(tag)) {
                val cleanDesc = description.substring(tag.length).trim()
                val res = Pair(currency.symbol, cleanDesc)
                parseCache[cacheKey] = res
                return res
            }
        }
        val res = Pair(defaultCurrencySymbol, description)
        parseCache[cacheKey] = res
        return res
    }

    fun getCurrencyRank(symbol: String): Int {
        val sym = symbol.uppercase(Locale.ENGLISH).trim()
        return when {
            sym == "ر.ي" || sym == "YER" || sym.contains("يمن") -> 1
            sym == "ر.س" || sym == "SAR" || sym.contains("سعود") -> 2
            sym == "$" || sym == "USD" || sym.contains("دولار") -> 3
            else -> 2 // default to medium strength
        }
    }

    // دالة لاستخراج المبلغ الأصلي الفعلي للمعاملة بالعملة التي سجلت بها
    fun getOriginalAmount(tx: HabayebTransaction): BigDecimal {
        return tx.foreignAmount
    }
    
    // دالة التحويل الآمنة بين العملات بناءً على أسعار الصرف الحالية
    fun convert(amount: BigDecimal, rate: BigDecimal, toWeaker: Boolean): BigDecimal {
        if (rate <= BigDecimal.ZERO) return amount.setScale(4, RoundingMode.HALF_EVEN)
        return if (toWeaker) {
            amount.multiply(rate, MathContext.DECIMAL128).setScale(4, RoundingMode.HALF_EVEN)
        } else {
            amount.divide(rate, 4, RoundingMode.HALF_EVEN)
        }
    }

    fun convertWithCurrencyPair(
        amount: BigDecimal,
        currencyPair: com.example.domain.model.CurrencyPair
    ): BigDecimal {
        return convertAmountBigDecimal(
            amount = amount,
            baseCurrencySymbol = currencyPair.baseCurrency,
            foreignCurrencySymbol = currencyPair.targetCurrency,
            rate = currencyPair.safeRate
        )
    }

    fun convertAmountBigDecimal(
        amount: BigDecimal,
        baseCurrencySymbol: String,
        foreignCurrencySymbol: String,
        rate: BigDecimal
    ): BigDecimal {
        val baseNorm = getBySymbol(baseCurrencySymbol)?.symbol ?: baseCurrencySymbol
        val foreignNorm = getBySymbol(foreignCurrencySymbol)?.symbol ?: foreignCurrencySymbol
        if (baseNorm == foreignNorm) {
            return amount.setScale(4, RoundingMode.HALF_EVEN)
        }
        val finalRate = if (rate <= BigDecimal.ZERO) BigDecimal.ONE else rate.setScale(4, RoundingMode.HALF_EVEN)
        val baseRank = getCurrencyRank(baseNorm)
        val foreignRank = getCurrencyRank(foreignNorm)

        return if (baseRank < foreignRank) {
            amount.multiply(finalRate, MathContext.DECIMAL128).setScale(4, RoundingMode.HALF_EVEN)
        } else {
            if (finalRate.compareTo(BigDecimal.ZERO) == 0) {
                amount.setScale(4, RoundingMode.HALF_EVEN)
            } else {
                amount.divide(finalRate, 4, RoundingMode.HALF_EVEN)
            }
        }
    }

    fun convertAmount(
        amount: Double,
        baseCurrencySymbol: String,
        foreignCurrencySymbol: String,
        rate: Double
    ): Double {
        if (baseCurrencySymbol == foreignCurrencySymbol) {
            return amount
        }
        val finalRate = if (rate <= 0.0) 1.0 else rate
        val baseRank = getCurrencyRank(baseCurrencySymbol)
        val foreignRank = getCurrencyRank(foreignCurrencySymbol)

        return try {
            val amountBD = BigDecimal.valueOf(amount)
            val rateBD = BigDecimal.valueOf(finalRate)
            if (baseRank < foreignRank) {
                amountBD.multiply(rateBD, MathContext.DECIMAL128).setScale(4, RoundingMode.HALF_EVEN).toDouble()
            } else {
                if (rateBD.compareTo(BigDecimal.ZERO) == 0) amount
                else amountBD.divide(rateBD, 4, RoundingMode.HALF_EVEN).toDouble()
            }
        } catch (e: Exception) {
            if (baseRank < foreignRank) amount * finalRate
            else if (finalRate != 0.0) amount / finalRate else amount
        }
    }

    /**
     * Resolves the true currency code and transaction amount for a given transaction as BigDecimal with scale 4,
     * handling both modern schema fields and legacy description tags,
     * fully taking into account base currency shifts dynamically without relying on is_foreign.
     */
    fun getTransactionCurrencyAndAmountBigDecimal(
        tx: HabayebTransaction,
        defaultCurrencySymbol: String,
        exchangeRatesJson: String = "{}"
    ): Pair<String, BigDecimal> {
        val parsedCurrencyInfo = parseTransactionCurrency(tx.description, defaultCurrencySymbol)
        val rawTxCurrency = if (tx.currencyCode != "DEFAULT" && tx.currencyCode.isNotBlank()) {
            tx.currencyCode
        } else if (parsedCurrencyInfo.first != defaultCurrencySymbol) {
            parsedCurrencyInfo.first
        } else if (tx.baseCurrencyCode != "DEFAULT" && tx.baseCurrencyCode.isNotBlank()) {
            tx.baseCurrencyCode
        } else {
            defaultCurrencySymbol
        }
        val txCurrency = getBySymbol(rawTxCurrency)?.symbol ?: rawTxCurrency

        if (tx.isRateCalculated) {
            val baseCurrencyRaw = if (tx.baseCurrencyCode != "DEFAULT" && tx.baseCurrencyCode.isNotBlank()) {
                tx.baseCurrencyCode
            } else {
                defaultCurrencySymbol
            }
            val baseCurrency = getBySymbol(baseCurrencyRaw)?.symbol ?: baseCurrencyRaw
            return Pair(baseCurrency, tx.equivalentAmount.setScale(4, RoundingMode.HALF_EVEN))
        }

        val normDefaultSymbol = getBySymbol(defaultCurrencySymbol)?.symbol ?: defaultCurrencySymbol
        val actualAmount = if (tx.foreignAmount.compareTo(BigDecimal.ZERO) > 0) tx.foreignAmount else tx.amount

        return if (txCurrency != normDefaultSymbol) {
            Pair(txCurrency, actualAmount.setScale(4, RoundingMode.HALF_EVEN))
        } else {
            Pair(normDefaultSymbol, tx.amount.setScale(4, RoundingMode.HALF_EVEN))
        }
    }

    /**
     * Resolves the true currency code and transaction amount for a given transaction,
     * handling both modern schema fields and legacy description tags,
     * fully taking into account base currency shifts dynamically without relying on is_foreign.
     */
    fun getTransactionCurrencyAndAmount(
        tx: HabayebTransaction,
        defaultCurrencySymbol: String,
        exchangeRatesJson: String = "{}"
    ): Pair<String, Double> {
        val (curr, bd) = getTransactionCurrencyAndAmountBigDecimal(tx, defaultCurrencySymbol, exchangeRatesJson)
        return Pair(curr, bd.toDouble())
    }

    /**
     * Helper to wrap a transaction description with a currency tag.
     */
    fun formatDescriptionWithCurrency(description: String, symbol: String): String {
        return "[$symbol] $description"
    }

    /**
     * Normalizes Arabic and Farsi digits to Western Arabic (English) digits, and replaces commas with dots.
     * Centralized via StringUtils.normalizeDigits.
     */
    fun normalizeDigits(input: String): String = com.example.domain.StringUtils.normalizeDigits(input)
}

