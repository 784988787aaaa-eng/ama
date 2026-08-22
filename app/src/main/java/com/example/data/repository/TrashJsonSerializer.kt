package com.example.data.repository

import android.content.SharedPreferences
import com.example.data.local.entities.FixedCommitment
import com.example.data.local.entities.HabayebCustomer
import com.example.data.local.entities.HabayebTransaction
import com.example.data.local.entities.TransactionDb
import com.example.domain.model.TransactionType
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal

object TrashJsonSerializer {

    private const val PREF_CAT_LINK_PREFIX = "CAT_LINK_"
    private const val PREF_KEY_PINNED_PREFIX = "KEY_PINNED_IN_"

    fun serializeCommitment(fc: FixedCommitment): String {
        return JSONObject().apply {
            put("name", fc.name)
            put("targetAmount", fc.targetAmount)
            put("currentProgress", fc.currentProgress)
            put("orderIndex", fc.orderIndex)
        }.toString()
    }

    fun serializeHabayebBundle(
        customer: HabayebCustomer,
        transactions: List<HabayebTransaction>,
        sharedPrefs: SharedPreferences
    ): String {
        val categoryLink = sharedPrefs.getString("$PREF_CAT_LINK_PREFIX${customer.id}", null)
        val pinnedCats = JSONArray()
        for ((key, value) in sharedPrefs.all) {
            if (key.startsWith(PREF_KEY_PINNED_PREFIX) && value is Set<*> && value.contains(customer.id)) {
                pinnedCats.put(key.removePrefix(PREF_KEY_PINNED_PREFIX))
            }
        }

        return JSONObject().apply {
            put("customer", JSONObject().apply {
                put("id", customer.id)
                put("name", customer.name)
                put("phone", customer.phone)
                put("notes", customer.notes)
                put("createdAt", customer.createdAt)
                if (categoryLink != null) {
                    put("categoryLink", categoryLink)
                }
                if (pinnedCats.length() > 0) {
                    put("pinnedCategories", pinnedCats)
                }
            })
            val txsArray = JSONArray()
            transactions.forEach { tx ->
                txsArray.put(serializeHabayebTransactionJsonObject(tx))
            }
            put("transactions", txsArray)
            put("totalTransactions", transactions.size)
            put("name", customer.name)
        }.toString()
    }

    fun serializeHabayebCustomer(customer: HabayebCustomer): String {
        return JSONObject().apply {
            put("id", customer.id)
            put("name", customer.name)
            put("phone", customer.phone)
            put("notes", customer.notes)
            put("createdAt", customer.createdAt)
        }.toString()
    }

    fun serializeTransaction(tx: TransactionDb): String {
        return serializeTransactionJsonObject(tx).toString()
    }

    fun serializeTransactionBundle(transactions: List<TransactionDb>, title: String): String {
        return JSONObject().apply {
            val txsArray = JSONArray()
            transactions.forEach { tx ->
                txsArray.put(serializeTransactionJsonObject(tx))
            }
            put("transactions", txsArray)
            put("totalTransactions", transactions.size)
            val totalNet = transactions.fold(BigDecimal.ZERO) { acc, tx ->
                if (tx.type == TransactionType.INCOME.value) acc.add(tx.amount) else acc.subtract(tx.amount)
            }
            put("totalNet", totalNet)
            put("name", title)
        }.toString()
    }

    fun serializeHabayebTransaction(tx: HabayebTransaction): String {
        return serializeHabayebTransactionJsonObject(tx).toString()
    }

    private fun serializeTransactionJsonObject(tx: TransactionDb): JSONObject {
        return JSONObject().apply {
            put("id", tx.id)
            put("timestamp", tx.timestamp)
            put("type", tx.type)
            put("category", tx.category)
            put("amount", tx.amount)
            put("description", tx.description)
        }
    }

    private fun serializeHabayebTransactionJsonObject(tx: HabayebTransaction): JSONObject {
        return JSONObject().apply {
            put("id", tx.id)
            put("customerId", tx.customerId)
            put("type", tx.type)
            put("amount", tx.amount)
            put("timestamp", tx.timestamp)
            put("description", tx.description)
            put("linkedMainTxId", tx.linkedMainTxId ?: JSONObject.NULL)
            put("is_foreign", tx.isForeign)
            put("currency_code", tx.currencyCode)
            put("foreign_amount", tx.foreignAmount)
            put("exchange_rate", tx.exchangeRate)
            put("is_rate_calculated", tx.isRateCalculated)
            put("equivalent_amount", tx.equivalentAmount)
            put("base_currency_code", tx.baseCurrencyCode)
        }
    }
}
