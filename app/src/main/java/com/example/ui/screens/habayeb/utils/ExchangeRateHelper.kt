package com.example.ui.screens.habayeb.utils

import com.example.domain.model.CurrencyPair
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode

object ExchangeRateHelper {
    
    fun getCurrencyPair(jsonStr: String, baseCurrencySymbol: String, foreignCurrencySymbol: String): CurrencyPair {
        val rate = getRateBigDecimal(jsonStr, baseCurrencySymbol, foreignCurrencySymbol)
        return CurrencyPair(
            baseCurrency = baseCurrencySymbol,
            targetCurrency = foreignCurrencySymbol,
            rate = rate
        )
    }

    fun setCurrencyPair(jsonStr: String, pair: CurrencyPair): String {
        return setRate(jsonStr, pair.baseCurrency, pair.targetCurrency, pair.safeRate)
    }

    fun getRateBigDecimal(jsonStr: String, baseCurrencySymbol: String, foreignCurrencySymbol: String): BigDecimal {
        val baseNorm = CurrencyConfig.getBySymbol(baseCurrencySymbol)?.symbol ?: baseCurrencySymbol
        val foreignNorm = CurrencyConfig.getBySymbol(foreignCurrencySymbol)?.symbol ?: foreignCurrencySymbol
        if (baseNorm == foreignNorm) return BigDecimal.ONE.setScale(4, RoundingMode.HALF_EVEN)
        return try {
            val root = JSONObject(if (jsonStr.isBlank()) "{}" else jsonStr)
            if (root.has(baseNorm) && root.get(baseNorm) is JSONObject) {
                val baseObj = root.getJSONObject(baseNorm)
                if (baseObj.has(foreignNorm)) {
                    val rawVal = baseObj.opt(foreignNorm)
                    val r = when (rawVal) {
                        is Number -> BigDecimal(rawVal.toString())
                        is String -> if (rawVal.isNotBlank()) BigDecimal(rawVal) else BigDecimal.ZERO
                        else -> BigDecimal.ZERO
                    }
                    if (r.compareTo(BigDecimal.ZERO) > 0) return r.setScale(4, RoundingMode.HALF_EVEN)
                }
            }
            if (root.has(foreignNorm) && root.get(foreignNorm) is JSONObject) {
                val foreignObj = root.getJSONObject(foreignNorm)
                if (foreignObj.has(baseNorm)) {
                    val rawVal = foreignObj.opt(baseNorm)
                    val invR = when (rawVal) {
                        is Number -> BigDecimal(rawVal.toString())
                        is String -> if (rawVal.isNotBlank()) BigDecimal(rawVal) else BigDecimal.ZERO
                        else -> BigDecimal.ZERO
                    }
                    if (invR.compareTo(BigDecimal.ZERO) > 0) return invR.setScale(4, RoundingMode.HALF_EVEN)
                }
            }
            BigDecimal.ONE.setScale(4, RoundingMode.HALF_EVEN)
        } catch (e: Exception) {
            BigDecimal.ONE.setScale(4, RoundingMode.HALF_EVEN)
        }
    }

    fun getRate(jsonStr: String, baseCurrencySymbol: String, foreignCurrencySymbol: String): Double {
        return getRateBigDecimal(jsonStr, baseCurrencySymbol, foreignCurrencySymbol).toDouble()
    }

    fun hasRate(jsonStr: String, baseCurrencySymbol: String, foreignCurrencySymbol: String): Boolean {
        val baseNorm = CurrencyConfig.getBySymbol(baseCurrencySymbol)?.symbol ?: baseCurrencySymbol
        val foreignNorm = CurrencyConfig.getBySymbol(foreignCurrencySymbol)?.symbol ?: foreignCurrencySymbol
        if (baseNorm == foreignNorm) return true
        return try {
            val root = JSONObject(if (jsonStr.isBlank()) "{}" else jsonStr)
            if (root.has(baseNorm) && root.get(baseNorm) is JSONObject) {
                val baseObj = root.getJSONObject(baseNorm)
                if (baseObj.has(foreignNorm)) {
                    val rate = getRateBigDecimal(jsonStr, baseCurrencySymbol, foreignCurrencySymbol)
                    if (rate.compareTo(BigDecimal.ZERO) > 0) return true
                }
            }
            if (root.has(foreignNorm) && root.get(foreignNorm) is JSONObject) {
                val foreignObj = root.getJSONObject(foreignNorm)
                if (foreignObj.has(baseNorm)) {
                    val rate = getRateBigDecimal(jsonStr, baseCurrencySymbol, foreignCurrencySymbol)
                    if (rate.compareTo(BigDecimal.ZERO) > 0) return true
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    fun setRate(jsonStr: String, baseCurrencySymbol: String, foreignCurrencySymbol: String, rate: BigDecimal): String {
        val baseNorm = CurrencyConfig.getBySymbol(baseCurrencySymbol)?.symbol ?: baseCurrencySymbol
        val foreignNorm = CurrencyConfig.getBySymbol(foreignCurrencySymbol)?.symbol ?: foreignCurrencySymbol
        if (baseNorm == foreignNorm) return jsonStr
        val updatedJson = try {
            val root = JSONObject(if (jsonStr.isBlank()) "{}" else jsonStr)
            
            // 1. Set direct rate for the base currency object
            val baseObj = if (root.has(baseNorm) && root.get(baseNorm) is JSONObject) {
                root.getJSONObject(baseNorm)
            } else {
                JSONObject()
            }
            val rateBD = rate.setScale(4, RoundingMode.HALF_EVEN)
            baseObj.put(foreignNorm, rateBD.toDouble())
            root.put(baseNorm, baseObj)
            
            // 2. Set identical rate for the foreign currency object to maintain bidirectional pair linkage
            if (rateBD.compareTo(BigDecimal.ZERO) > 0) {
                val foreignObj = if (root.has(foreignNorm) && root.get(foreignNorm) is JSONObject) {
                    root.getJSONObject(foreignNorm)
                } else {
                    JSONObject()
                }
                foreignObj.put(baseNorm, rateBD.toDouble())
                root.put(foreignNorm, foreignObj)
            }
            
            root.toString()
        } catch (e: Exception) {
            jsonStr
        }
        return completeMatrix(updatedJson)
    }

    fun setRate(jsonStr: String, baseCurrencySymbol: String, foreignCurrencySymbol: String, rate: Double): String {
        return setRate(jsonStr, baseCurrencySymbol, foreignCurrencySymbol, BigDecimal.valueOf(rate))
    }

    fun completeMatrix(jsonStr: String): String {
        try {
            val root = JSONObject(if (jsonStr.isBlank()) "{}" else jsonStr)
            
            val symbolsSet = mutableSetOf("ر.ي", "ر.س", "$")
            val keys = root.keys()
            while (keys.hasNext()) {
                val key = keys.next() as String
                symbolsSet.add(key)
                if (root.get(key) is JSONObject) {
                    val inner = root.getJSONObject(key)
                    val innerKeys = inner.keys()
                    while (innerKeys.hasNext()) {
                        symbolsSet.add(innerKeys.next() as String)
                    }
                }
            }
            val symbols = symbolsSet.toList()
            
            val rates = mutableMapOf<String, MutableMap<String, BigDecimal>>()
            for (src in symbols) {
                rates[src] = mutableMapOf()
                rates[src]!![src] = BigDecimal.ONE
            }
            
            for (src in symbols) {
                if (root.has(src) && root.get(src) is JSONObject) {
                    val obj = root.getJSONObject(src)
                    for (dst in symbols) {
                        if (obj.has(dst)) {
                            val rawVal = obj.opt(dst)
                            val r = when (rawVal) {
                                is Number -> BigDecimal(rawVal.toString())
                                is String -> if (rawVal.isNotBlank()) BigDecimal(rawVal) else BigDecimal.ZERO
                                else -> BigDecimal.ZERO
                            }
                            if (r.compareTo(BigDecimal.ZERO) > 0) {
                                rates[src]!![dst] = r.setScale(4, RoundingMode.HALF_EVEN)
                            }
                        }
                    }
                }
            }
            
            // Synchronize direct pair symmetry ONLY for explicit user-entered pairs
            for (src in symbols) {
                for (dst in symbols) {
                    if (src != dst) {
                        val direct = rates[src]?.get(dst)
                        val inverse = rates[dst]?.get(src)
                        if (direct != null && direct.compareTo(BigDecimal.ZERO) > 0 && (inverse == null || inverse.compareTo(BigDecimal.ZERO) <= 0)) {
                            rates[dst]!![src] = direct
                        } else if (inverse != null && inverse.compareTo(BigDecimal.ZERO) > 0 && (direct == null || direct.compareTo(BigDecimal.ZERO) <= 0)) {
                            rates[src]!![dst] = inverse
                        }
                    }
                }
            }

            for (src in symbols) {
                val obj = if (root.has(src) && root.get(src) is JSONObject) root.getJSONObject(src) else JSONObject()
                val srcRates = rates[src] ?: continue
                for ((dst, r) in srcRates) {
                    if (src != dst && r.compareTo(BigDecimal.ZERO) > 0) {
                        obj.put(dst, r.toDouble())
                    }
                }
                root.put(src, obj)
            }
            return root.toString()
        } catch (e: Exception) {
            return jsonStr
        }
    }

    fun migrateRates(jsonStr: String, oldBase: String, newBase: String): String {
        return jsonStr
    }
}


