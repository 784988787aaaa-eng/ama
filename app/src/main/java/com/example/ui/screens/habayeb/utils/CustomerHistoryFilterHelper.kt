package com.example.ui.screens.habayeb.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import com.example.R
import com.example.data.local.entities.HabayebTransaction
import com.example.domain.StringUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberFilteredCustomerTransactions(
    context: Context,
    allCustomerTxs: List<HabayebTransaction>,
    txSearchQuery: String,
    dateFilterMode: Int,
    customStartDate: Long?,
    customEndDate: Long?,
    typeFilterMode: Int,
    selectedCurrencyFilter: String?,
    currencySymbol: String,
    exchangeRatesJson: String?
): State<List<HabayebTransaction>> {
    val dateBoundaries = remember(dateFilterMode) {
        if (dateFilterMode == 0) return@remember longArrayOf(0, 0, 0, 0)
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis / 1000
        val todayEnd = todayStart + 86400

        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        val monthStart = cal.timeInMillis / 1000
        cal.add(java.util.Calendar.MONTH, 1)
        val monthEnd = cal.timeInMillis / 1000

        longArrayOf(todayStart, todayEnd, monthStart, monthEnd)
    }

    val isSearchBlank = txSearchQuery.isBlank()
    val hasNoFilters = isSearchBlank && dateFilterMode == 0 && typeFilterMode == 0 && selectedCurrencyFilter == null

    val sortedDirect = remember(allCustomerTxs) {
        if (allCustomerTxs.isEmpty()) emptyList() else allCustomerTxs.sortedByDescending { it.timestamp }
    }

    if (hasNoFilters) {
        return androidx.compose.runtime.rememberUpdatedState(sortedDirect)
    }

    return produceState<List<HabayebTransaction>>(
        initialValue = sortedDirect,
        allCustomerTxs, txSearchQuery, dateFilterMode, customStartDate, customEndDate, typeFilterMode, selectedCurrencyFilter, currencySymbol, exchangeRatesJson, dateBoundaries
    ) {
        withContext(Dispatchers.Default) {
            val todayStart = dateBoundaries[0]
            val todayEnd = dateBoundaries[1]
            val monthStart = dateBoundaries[2]
            val monthEnd = dateBoundaries[3]

            val searchDebtStr = context.getString(R.string.customer_history_search_debt)
            val searchPaymentStr = context.getString(R.string.customer_history_search_payment)
            val normalizedQuery = if (!isSearchBlank) StringUtils.normalizeArabic(txSearchQuery) else ""
            val normalizedDebtStr = if (!isSearchBlank) StringUtils.normalizeArabic(searchDebtStr) else ""
            val normalizedPaymentStr = if (!isSearchBlank) StringUtils.normalizeArabic(searchPaymentStr) else ""

            val safeRatesJson = exchangeRatesJson ?: ""

            val baseFiltered = allCustomerTxs.filter { tx ->
                val matchesSearch = if (isSearchBlank) {
                    true
                } else {
                    val normalizedDesc = StringUtils.normalizeArabic(tx.description)
                    val typeText = if (tx.type == "OWED_BY_THEM") normalizedDebtStr else normalizedPaymentStr

                    normalizedDesc.contains(normalizedQuery, ignoreCase = true) ||
                    tx.amount.toString().contains(txSearchQuery) ||
                    tx.foreignAmount.toString().contains(txSearchQuery) ||
                    typeText.contains(normalizedQuery, ignoreCase = true)
                }

                val matchesDate = when (dateFilterMode) {
                    1 -> tx.timestamp in todayStart..todayEnd
                    2 -> tx.timestamp in monthStart..monthEnd
                    3 -> {
                        val startSec = (customStartDate ?: 0L) / 1000
                        val endSec = if (customEndDate != null) (customEndDate / 1000) + 86400 else Long.MAX_VALUE
                        tx.timestamp in startSec..endSec
                    }
                    else -> true
                }

                val matchesType = when (typeFilterMode) {
                    1 -> tx.type == "OWED_BY_THEM" || tx.type == "OWED_TO_THEM"
                    2 -> tx.type == "PAYMENT_BY_THEM" || tx.type == "PAYMENT_TO_THEM"
                    else -> true
                }

                val matchesCurrency = if (selectedCurrencyFilter != null) {
                    val (txCurrency, _) = CurrencyConfig.getTransactionCurrencyAndAmount(tx, currencySymbol, safeRatesJson)
                    txCurrency == selectedCurrencyFilter
                } else {
                    true
                }

                matchesSearch && matchesDate && matchesType && matchesCurrency
            }
            value = baseFiltered.sortedByDescending { it.timestamp }
        }
    }
}
