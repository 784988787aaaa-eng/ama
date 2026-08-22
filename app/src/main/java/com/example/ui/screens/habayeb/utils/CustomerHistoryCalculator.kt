package com.example.ui.screens.habayeb.utils

import com.example.data.local.entities.HabayebTransaction
import com.example.domain.model.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode

@androidx.compose.runtime.Immutable
data class CustomerSummaryResult(
    val netDebtBigDecimalMap: Map<String, BigDecimal>,
    val primaryDisplayCurrency: String,
    val netDebt: Double,
    val lastTimestamp: Long
)

@androidx.compose.runtime.Immutable
data class CustomerHistoryCalculationResult(
    val currencyKeys: List<String>,
    val owedByThemMap: Map<String, Double>,
    val paymentByThemMap: Map<String, Double>,
    val owedToThemMap: Map<String, Double>,
    val paymentToThemMap: Map<String, Double>,
    val netDebtMap: Map<String, Double>,
    val runningBalances: Map<String, BigDecimal>,
    val txSequenceNumbers: Map<String, Int>,
    val primaryDisplayCurrency: String,
    val netDebt: Double,
    val netDebtBigDecimalMap: Map<String, BigDecimal> = emptyMap(),
    val owedByThemBDMap: Map<String, BigDecimal> = emptyMap(),
    val paymentByThemBDMap: Map<String, BigDecimal> = emptyMap(),
    val owedToThemBDMap: Map<String, BigDecimal> = emptyMap(),
    val paymentToThemBDMap: Map<String, BigDecimal> = emptyMap()
) {
    val runningBalancesDouble: Map<String, Double> by lazy {
        runningBalances.mapValues { it.value.toDouble() }
    }
}

object CustomerHistoryCalculator {
    fun calculate(
        allCustomerTxs: List<HabayebTransaction>,
        currencySymbol: String,
        exchangeRatesJson: String?
    ): CustomerHistoryCalculationResult {
        val safeRatesJson = exchangeRatesJson ?: ""
        val totalCount = allCustomerTxs.size

        // Sort chronologically once (timestamp, then id for deterministic stability)
        val chronological = if (totalCount <= 1) allCustomerTxs else allCustomerTxs.sortedWith(
            compareBy<HabayebTransaction> { it.timestamp }.thenBy { it.id }
        )

        val owedByThemBD = HashMap<String, BigDecimal>(4)
        val paymentByThemBD = HashMap<String, BigDecimal>(4)
        val owedToThemBD = HashMap<String, BigDecimal>(4)
        val paymentToThemBD = HashMap<String, BigDecimal>(4)

        val currencySet = LinkedHashSet<String>(4)
        currencySet.add(currencySymbol)

        val balancesMap = HashMap<String, BigDecimal>(totalCount)
        val currentBalBDMap = HashMap<String, BigDecimal>(4)
        val txSequenceNumbers = HashMap<String, Int>(totalCount)

        var seq = 1
        for (tx in chronological) {
            val (txCurrency, bdAmount) = CurrencyConfig.getTransactionCurrencyAndAmountBigDecimal(tx, currencySymbol, safeRatesJson)
            currencySet.add(txCurrency)

            val safeBd = bdAmount.setScale(4, RoundingMode.HALF_EVEN)
            val txType = TransactionType.fromValue(tx.type)

            // Accumulate by type using exact BigDecimal math
            when (txType) {
                TransactionType.OWED_BY_THEM -> owedByThemBD[txCurrency] = (owedByThemBD[txCurrency] ?: BigDecimal.ZERO).add(safeBd)
                TransactionType.PAYMENT_BY_THEM -> paymentByThemBD[txCurrency] = (paymentByThemBD[txCurrency] ?: BigDecimal.ZERO).add(safeBd)
                TransactionType.OWED_TO_THEM -> owedToThemBD[txCurrency] = (owedToThemBD[txCurrency] ?: BigDecimal.ZERO).add(safeBd)
                TransactionType.PAYMENT_TO_THEM -> paymentToThemBD[txCurrency] = (paymentToThemBD[txCurrency] ?: BigDecimal.ZERO).add(safeBd)
                else -> {}
            }

            // Calculate running balance using exact BigDecimal math
            var currentBalBD = currentBalBDMap[txCurrency] ?: BigDecimal.ZERO
            currentBalBD = when (txType) {
                TransactionType.OWED_BY_THEM, TransactionType.PAYMENT_TO_THEM -> currentBalBD.add(safeBd)
                TransactionType.PAYMENT_BY_THEM, TransactionType.OWED_TO_THEM -> currentBalBD.subtract(safeBd)
                else -> currentBalBD
            }.setScale(4, RoundingMode.HALF_EVEN)

            currentBalBDMap[txCurrency] = currentBalBD
            balancesMap[tx.id] = currentBalBD
            txSequenceNumbers[tx.id] = seq++
        }

        val currencyKeys = currencySet.toList()
        val numCurrencies = currencyKeys.size

        val owedByThemMap = HashMap<String, Double>(numCurrencies)
        val paymentByThemMap = HashMap<String, Double>(numCurrencies)
        val owedToThemMap = HashMap<String, Double>(numCurrencies)
        val paymentToThemMap = HashMap<String, Double>(numCurrencies)
        val netDebtMap = HashMap<String, Double>(numCurrencies)
        val netDebtBDMap = HashMap<String, BigDecimal>(numCurrencies)

        for (curr in currencyKeys) {
            val owedBy = (owedByThemBD[curr] ?: BigDecimal.ZERO).setScale(4, RoundingMode.HALF_EVEN)
            val payBy = (paymentByThemBD[curr] ?: BigDecimal.ZERO).setScale(4, RoundingMode.HALF_EVEN)
            val owedTo = (owedToThemBD[curr] ?: BigDecimal.ZERO).setScale(4, RoundingMode.HALF_EVEN)
            val payTo = (paymentToThemBD[curr] ?: BigDecimal.ZERO).setScale(4, RoundingMode.HALF_EVEN)

            owedByThemMap[curr] = owedBy.toDouble()
            paymentByThemMap[curr] = payBy.toDouble()
            owedToThemMap[curr] = owedTo.toDouble()
            paymentToThemMap[curr] = payTo.toDouble()

            // netDebt = owedBy - payBy - owedTo + payTo
            val netDebtBD = owedBy.subtract(payBy).subtract(owedTo).add(payTo).setScale(4, RoundingMode.HALF_EVEN)
            netDebtMap[curr] = netDebtBD.toDouble()
            netDebtBDMap[curr] = netDebtBD
        }

        val primaryDisplayCurrency: String
        val netDebt: Double

        val baseNetBd = netDebtBDMap[currencySymbol] ?: BigDecimal.ZERO
        if (baseNetBd.setScale(4, RoundingMode.HALF_EVEN).compareTo(BigDecimal.ZERO) != 0) {
            primaryDisplayCurrency = currencySymbol
            netDebt = baseNetBd.toDouble()
        } else {
            val nonZeroForeignEntry = netDebtBDMap.entries.firstOrNull { (k, v) ->
                k != currencySymbol && v.setScale(4, RoundingMode.HALF_EVEN).compareTo(BigDecimal.ZERO) != 0
            }
            if (nonZeroForeignEntry != null) {
                primaryDisplayCurrency = nonZeroForeignEntry.key
                netDebt = nonZeroForeignEntry.value.toDouble()
            } else {
                primaryDisplayCurrency = currencySymbol
                netDebt = 0.0
            }
        }

        return CustomerHistoryCalculationResult(
            currencyKeys = currencyKeys,
            owedByThemMap = owedByThemMap,
            paymentByThemMap = paymentByThemMap,
            owedToThemMap = owedToThemMap,
            paymentToThemMap = paymentToThemMap,
            netDebtMap = netDebtMap,
            runningBalances = balancesMap,
            txSequenceNumbers = txSequenceNumbers,
            primaryDisplayCurrency = primaryDisplayCurrency,
            netDebt = netDebt,
            netDebtBigDecimalMap = netDebtBDMap,
            owedByThemBDMap = owedByThemBD,
            paymentByThemBDMap = paymentByThemBD,
            owedToThemBDMap = owedToThemBD,
            paymentToThemBDMap = paymentToThemBD
        )
    }

    fun calculateSummary(
        allCustomerTxs: List<HabayebTransaction>,
        currencySymbol: String,
        exchangeRatesJson: String?,
        fallbackCreatedAt: Long
    ): CustomerSummaryResult {
        if (allCustomerTxs.isEmpty()) {
            return CustomerSummaryResult(
                netDebtBigDecimalMap = emptyMap(),
                primaryDisplayCurrency = currencySymbol,
                netDebt = 0.0,
                lastTimestamp = fallbackCreatedAt
            )
        }

        val safeRatesJson = exchangeRatesJson ?: ""
        val owedByThemBD = HashMap<String, BigDecimal>(2)
        val paymentByThemBD = HashMap<String, BigDecimal>(2)
        val owedToThemBD = HashMap<String, BigDecimal>(2)
        val paymentToThemBD = HashMap<String, BigDecimal>(2)
        val currencySet = LinkedHashSet<String>(2)
        currencySet.add(currencySymbol)

        var maxTime = fallbackCreatedAt

        for (tx in allCustomerTxs) {
            if (tx.timestamp > maxTime) {
                maxTime = tx.timestamp
            }
            val (txCurrency, bdAmount) = CurrencyConfig.getTransactionCurrencyAndAmountBigDecimal(tx, currencySymbol, safeRatesJson)
            currencySet.add(txCurrency)

            val safeBd = bdAmount.setScale(4, RoundingMode.HALF_EVEN)
            when (TransactionType.fromValue(tx.type)) {
                TransactionType.OWED_BY_THEM -> owedByThemBD[txCurrency] = (owedByThemBD[txCurrency] ?: BigDecimal.ZERO).add(safeBd)
                TransactionType.PAYMENT_BY_THEM -> paymentByThemBD[txCurrency] = (paymentByThemBD[txCurrency] ?: BigDecimal.ZERO).add(safeBd)
                TransactionType.OWED_TO_THEM -> owedToThemBD[txCurrency] = (owedToThemBD[txCurrency] ?: BigDecimal.ZERO).add(safeBd)
                TransactionType.PAYMENT_TO_THEM -> paymentToThemBD[txCurrency] = (paymentToThemBD[txCurrency] ?: BigDecimal.ZERO).add(safeBd)
                else -> {}
            }
        }

        val netDebtBDMap = HashMap<String, BigDecimal>(currencySet.size)
        for (curr in currencySet) {
            val owedBy = owedByThemBD[curr] ?: BigDecimal.ZERO
            val payBy = paymentByThemBD[curr] ?: BigDecimal.ZERO
            val owedTo = owedToThemBD[curr] ?: BigDecimal.ZERO
            val payTo = paymentToThemBD[curr] ?: BigDecimal.ZERO
            netDebtBDMap[curr] = owedBy.subtract(payBy).subtract(owedTo).add(payTo).setScale(4, RoundingMode.HALF_EVEN)
        }

        val primaryDisplayCurrency: String
        val netDebt: Double
        val baseNetBd = netDebtBDMap[currencySymbol] ?: BigDecimal.ZERO
        if (baseNetBd.setScale(4, RoundingMode.HALF_EVEN).compareTo(BigDecimal.ZERO) != 0) {
            primaryDisplayCurrency = currencySymbol
            netDebt = baseNetBd.toDouble()
        } else {
            val nonZeroForeignEntry = netDebtBDMap.entries.firstOrNull { (k, v) ->
                k != currencySymbol && v.setScale(4, RoundingMode.HALF_EVEN).compareTo(BigDecimal.ZERO) != 0
            }
            if (nonZeroForeignEntry != null) {
                primaryDisplayCurrency = nonZeroForeignEntry.key
                netDebt = nonZeroForeignEntry.value.toDouble()
            } else {
                primaryDisplayCurrency = currencySymbol
                netDebt = 0.0
            }
        }

        return CustomerSummaryResult(
            netDebtBigDecimalMap = netDebtBDMap,
            primaryDisplayCurrency = primaryDisplayCurrency,
            netDebt = netDebt,
            lastTimestamp = maxTime
        )
    }
}


