package com.example.ui.screens.habayeb.utils

import com.example.domain.model.CurrencyPair
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Exchange-rate persistence and lookup.
 *
 * Schema v1 (legacy): a pair was stored symmetrically (A->B = R, B->A = R)
 * and CurrencyRank decided whether conversion multiplied or divided.
 *
 * Schema v2 (current): rates are directed quotes. For a stored pair A->B,
 * the value means "units of A (the output/base currency) per one unit of B
 * (the input/foreign currency)". Therefore B->A is exactly 1 / (A->B).
 * Conversion code multiplies the input/foreign amount by the requested quote.
 */
object ExchangeRateHelper {
    private const val SCHEMA_KEY = "__schemaVersion"
    private const val BASE_KEY = "__baseCurrency"
    private const val WARNINGS_KEY = "__migrationWarnings"
    private const val CURRENT_SCHEMA = 2
    private const val SCALE = 8
    private val ZERO = BigDecimal.ZERO
    private val ONE = BigDecimal.ONE

    private fun normalize(symbol: String): String =
        CurrencyConfig.getBySymbol(symbol)?.symbol ?: symbol.trim()

    private fun readRate(value: Any?): BigDecimal? {
        return try {
            val rate = when (value) {
                is Number -> BigDecimal(value.toString())
                is String -> if (value.isBlank()) null else BigDecimal(value)
                else -> null
            } ?: return null
            if (rate > ZERO) rate else null
        } catch (_: Exception) {
            null
        }
    }

    private fun rounded(rate: BigDecimal): BigDecimal =
        rate.setScale(SCALE, RoundingMode.HALF_EVEN)

    private fun schemaVersion(root: JSONObject): Int = root.optInt(SCHEMA_KEY, 1)

    private fun symbolsFrom(root: JSONObject): Set<String> {
        val symbols = linkedSetOf<String>()
        CurrencyConfig.currencies.forEach { symbols.add(it.symbol) }
        val keys = root.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (!key.startsWith("__") && root.opt(key) is JSONObject) {
                symbols.add(key)
                val inner = root.optJSONObject(key)
                if (inner != null) {
                    val innerKeys = inner.keys()
                    while (innerKeys.hasNext()) symbols.add(innerKeys.next())
                }
            }
        }
        return symbols
    }

    fun getCurrencyPair(jsonStr: String, baseCurrencySymbol: String, foreignCurrencySymbol: String): CurrencyPair {
        return CurrencyPair(
            baseCurrency = normalize(baseCurrencySymbol),
            targetCurrency = normalize(foreignCurrencySymbol),
            rate = getRateBigDecimal(jsonStr, baseCurrencySymbol, foreignCurrencySymbol)
        )
    }

    fun setCurrencyPair(jsonStr: String, pair: CurrencyPair): String {
        if (!pair.isValid && !pair.isSelfPair) return jsonStr
        return setRate(jsonStr, pair.baseCurrency, pair.targetCurrency, pair.rate)
    }

    /**
     * Returns a directed rate: baseCurrency -> foreignCurrency.
     * Missing/invalid rates return 0, never 1.
     */
    fun getRateBigDecimal(jsonStr: String, baseCurrencySymbol: String, foreignCurrencySymbol: String): BigDecimal {
        val base = normalize(baseCurrencySymbol)
        val foreign = normalize(foreignCurrencySymbol)
        if (base == foreign) return ONE.setScale(SCALE, RoundingMode.HALF_EVEN)

        return try {
            val root = JSONObject(if (jsonStr.isBlank()) "{}" else jsonStr)
            if (schemaVersion(root) >= CURRENT_SCHEMA) {
                readRate(root.optJSONObject(base)?.opt(foreign))?.let { return rounded(it) }
                return ZERO.setScale(SCALE, RoundingMode.HALF_EVEN)
            }

            // Legacy v1 compatibility: the persisted value is a pair magnitude.
            // Old conversion semantics multiplied when baseRank < foreignRank.
            val direct = readRate(root.optJSONObject(base)?.opt(foreign))
            val reverse = readRate(root.optJSONObject(foreign)?.opt(base))
            val pairMagnitude = direct ?: reverse ?: return ZERO.setScale(SCALE, RoundingMode.HALF_EVEN)
            val baseRank = CurrencyConfig.getCurrencyRank(base)
            val foreignRank = CurrencyConfig.getCurrencyRank(foreign)
            val directed = if (baseRank < foreignRank) pairMagnitude else ONE.divide(pairMagnitude, SCALE, RoundingMode.HALF_EVEN)
            rounded(directed)
        } catch (_: Exception) {
            ZERO.setScale(SCALE, RoundingMode.HALF_EVEN)
        }
    }

    fun getRate(jsonStr: String, baseCurrencySymbol: String, foreignCurrencySymbol: String): Double =
        getRateBigDecimal(jsonStr, baseCurrencySymbol, foreignCurrencySymbol).toDouble()

    fun hasRate(jsonStr: String, baseCurrencySymbol: String, foreignCurrencySymbol: String): Boolean =
        getRateBigDecimal(jsonStr, baseCurrencySymbol, foreignCurrencySymbol) > ZERO

    /**
     * Stores a directed rate and its mathematically correct inverse.
     */
    fun setRate(jsonStr: String, baseCurrencySymbol: String, foreignCurrencySymbol: String, rate: BigDecimal): String {
        val base = normalize(baseCurrencySymbol)
        val foreign = normalize(foreignCurrencySymbol)
        if (base == foreign || rate <= ZERO) return jsonStr

        return try {
            val migrated = migrateRates(jsonStr, base, base)
            val root = JSONObject(if (migrated.isBlank()) "{}" else migrated)
            val rateBD = rounded(rate)
            val inverse = rounded(ONE.divide(rateBD, SCALE, RoundingMode.HALF_EVEN))

            val baseObj = root.optJSONObject(base) ?: JSONObject()
            baseObj.put(foreign, rateBD.toPlainString())
            root.put(base, baseObj)

            val foreignObj = root.optJSONObject(foreign) ?: JSONObject()
            foreignObj.put(base, inverse.toPlainString())
            root.put(foreign, foreignObj)

            root.put(SCHEMA_KEY, CURRENT_SCHEMA)
            root.toString()
        } catch (_: Exception) {
            jsonStr
        }
    }

    fun setRate(jsonStr: String, baseCurrencySymbol: String, foreignCurrencySymbol: String, rate: Double): String =
        setRate(jsonStr, baseCurrencySymbol, foreignCurrencySymbol, BigDecimal.valueOf(rate))

    /**
     * Ensures every known pair in a v2 document has its inverse. Missing
     * inverse is derived mathematically; existing directed values are kept.
     */
    fun completeMatrix(jsonStr: String): String {
        return try {
            val root = migrateRates(jsonStr, "", "")
            val out = JSONObject(if (root.isBlank()) "{}" else root)
            val symbols = symbolsFrom(out).toList()

            for (a in symbols) {
                val aObj = out.optJSONObject(a) ?: JSONObject().also { out.put(a, it) }
                for (b in symbols) {
                    if (a == b) continue
                    val direct = readRate(aObj.opt(b))
                    val reverse = readRate(out.optJSONObject(b)?.opt(a))
                    when {
                        direct != null && reverse == null -> {
                            val inv = rounded(ONE.divide(direct, SCALE, RoundingMode.HALF_EVEN))
                            (out.optJSONObject(b) ?: JSONObject().also { out.put(b, it) })
                                .put(a, inv.toPlainString())
                        }
                        direct == null && reverse != null -> {
                            val inv = rounded(ONE.divide(reverse, SCALE, RoundingMode.HALF_EVEN))
                            aObj.put(b, inv.toPlainString())
                        }
                    }
                }
            }
            out.put(SCHEMA_KEY, CURRENT_SCHEMA)
            out.toString()
        } catch (_: Exception) {
            jsonStr
        }
    }

    /**
     * Migrates legacy symmetric pair storage to directed schema v2.
     * Existing legacy data is interpreted using the old CurrencyRank contract
     * only at migration time. After migration, CurrencyRank is no longer part
     * of conversion semantics.
     */
    fun migrateRates(jsonStr: String, oldBase: String, newBase: String): String {
        return try {
            val root = JSONObject(if (jsonStr.isBlank()) "{}" else jsonStr)
            if (schemaVersion(root) >= CURRENT_SCHEMA) {
                if (newBase.isNotBlank()) root.put(BASE_KEY, normalize(newBase))
                return completeMatrixV2(root)
            }

            val symbols = symbolsFrom(root).toList()
            val rates = mutableMapOf<Pair<String, String>, BigDecimal>()
            val warnings = JSONArray()

            for (i in symbols.indices) {
                for (j in i + 1 until symbols.size) {
                    val a = symbols[i]
                    val b = symbols[j]
                    val ab = readRate(root.optJSONObject(a)?.opt(b))
                    val ba = readRate(root.optJSONObject(b)?.opt(a))
                    if (ab == null && ba == null) continue

                    val chosen = when {
                        ab != null && ba != null && nearlyEqual(ab, ba) -> ab
                        ab != null && ba == null -> ab
                        ab == null && ba != null -> ba
                        else -> {
                            warnings.put("Ambiguous legacy pair $a/$b: retained the value from the weaker→stronger orientation")
                            chooseLegacyCanonical(a, b, ab!!, ba!!)
                        }
                    }
                    val (weaker, stronger) = if (CurrencyConfig.getCurrencyRank(a) < CurrencyConfig.getCurrencyRank(b)) {
                        a to b
                    } else if (CurrencyConfig.getCurrencyRank(b) < CurrencyConfig.getCurrencyRank(a)) {
                        b to a
                    } else {
                        if (a <= b) a to b else b to a
                    }
                    rates[weaker to stronger] = rounded(chosen)
                    rates[stronger to weaker] = rounded(ONE.divide(chosen, SCALE, RoundingMode.HALF_EVEN))
                }
            }

            val out = JSONObject()
            symbols.forEach { out.put(it, JSONObject()) }
            rates.forEach { (pair, rate) ->
                out.optJSONObject(pair.first)!!.put(pair.second, rate.toPlainString())
            }
            out.put(SCHEMA_KEY, CURRENT_SCHEMA)
            val base = if (newBase.isNotBlank()) normalize(newBase) else normalize(oldBase)
            if (base.isNotBlank()) out.put(BASE_KEY, base)
            if (warnings.length() > 0) out.put(WARNINGS_KEY, warnings)
            out.toString()
        } catch (_: Exception) {
            jsonStr
        }
    }

    private fun completeMatrixV2(root: JSONObject): String {
        val symbols = symbolsFrom(root).toList()
        for (a in symbols) {
            val aObj = root.optJSONObject(a) ?: JSONObject().also { root.put(a, it) }
            for (b in symbols) {
                if (a == b) continue
                val direct = readRate(aObj.opt(b))
                val reverseObj = root.optJSONObject(b)
                val reverse = readRate(reverseObj?.opt(a))
                when {
                    direct != null && reverse == null -> reverseObjOrCreate(root, b).put(a, rounded(ONE.divide(direct, SCALE, RoundingMode.HALF_EVEN)).toPlainString())
                    direct == null && reverse != null -> aObj.put(b, rounded(ONE.divide(reverse, SCALE, RoundingMode.HALF_EVEN)).toPlainString())
                }
            }
        }
        root.put(SCHEMA_KEY, CURRENT_SCHEMA)
        return root.toString()
    }

    private fun reverseObjOrCreate(root: JSONObject, symbol: String): JSONObject =
        root.optJSONObject(symbol) ?: JSONObject().also { root.put(symbol, it) }

    private fun nearlyEqual(a: BigDecimal, b: BigDecimal): Boolean =
        a.subtract(b).abs() <= BigDecimal("0.00000001")

    private fun chooseLegacyCanonical(a: String, b: String, ab: BigDecimal, ba: BigDecimal): BigDecimal {
        val ar = CurrencyConfig.getCurrencyRank(a)
        val br = CurrencyConfig.getCurrencyRank(b)
        return when {
            ar < br -> ab
            br < ar -> ba
            else -> ab
        }
    }
}
