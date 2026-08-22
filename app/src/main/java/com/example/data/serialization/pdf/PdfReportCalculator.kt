package com.example.data.serialization.pdf

import com.example.data.local.entities.HabayebTransaction
import com.example.domain.model.TransactionType
import com.example.ui.screens.habayeb.utils.CurrencyConfig
import com.example.ui.state.CustomerUiState
import com.example.ui.viewmodel.FinanceConstants
import java.math.BigDecimal

data class ProcessedTransaction(
    val tx: HabayebTransaction,
    val resolvedCurrency: String,
    val resolvedAmount: BigDecimal,
    val isTxForeign: Boolean,
    val baseCurrencyAmount: BigDecimal,
    val pureBaseAmount: BigDecimal
)

data class SingleCustomerPdfSummary(
    val sortedProcessedTxs: List<ProcessedTransaction>,
    val totalDebts: BigDecimal,
    val totalPayments: BigDecimal,
    val totalDebtsBase: BigDecimal,
    val totalPaymentsBase: BigDecimal,
    val calculatedNetDebt: BigDecimal,
    val uncalculatedForeignSums: Map<String, BigDecimal>,
    val hasMultipleCurrencies: Boolean
)

data class ComprehensivePdfSummary(
    val totalOwedByThem: BigDecimal,
    val totalOwedToThem: BigDecimal,
    val netPrimary: BigDecimal,
    val foreignTotalsMap: Map<String, BigDecimal>
)

object PdfReportCalculator {

    fun calculateSingleCustomerReport(
        transactions: List<HabayebTransaction>,
        currencySymbol: String
    ): SingleCustomerPdfSummary {
        val calcResult = com.example.ui.screens.habayeb.utils.CustomerHistoryCalculator.calculate(
            transactions,
            currencySymbol,
            exchangeRatesJson = null
        )

        val normDefaultSymbol = CurrencyConfig.getBySymbol(currencySymbol)?.symbol ?: currencySymbol
        val sortedTxs = transactions.sortedWith(compareBy<HabayebTransaction> { it.timestamp }.thenBy { it.id })

        val processedList = ArrayList<ProcessedTransaction>(sortedTxs.size)
        var totalDebtsBase = BigDecimal.ZERO
        var totalPaymentsBase = BigDecimal.ZERO

        for (tx in sortedTxs) {
            val (resolvedCurrency, resolvedAmount) = CurrencyConfig.getTransactionCurrencyAndAmountBigDecimal(tx, currencySymbol)
            val affectsReportPrimaryCurrency = (resolvedCurrency == normDefaultSymbol)

            // Strictly assign baseCurrencyAmount only if this transaction targets the report's primary currency!
            val baseCurrencyAmount = if (affectsReportPrimaryCurrency) resolvedAmount else BigDecimal.ZERO

            val isTxForeign = (tx.currencyCode != FinanceConstants.DEFAULT_CURRENCY_CODE && 
                               tx.currencyCode.isNotBlank() && 
                               tx.currencyCode != normDefaultSymbol) || 
                              tx.isRateCalculated

            val isPureBase = (tx.currencyCode == FinanceConstants.DEFAULT_CURRENCY_CODE || tx.currencyCode.isBlank() || tx.currencyCode == normDefaultSymbol) && !tx.isRateCalculated
            val pureBaseAmount = if (isPureBase) tx.foreignAmount else BigDecimal.ZERO

            val txType = TransactionType.fromValue(tx.type)
            if (affectsReportPrimaryCurrency) {
                if (txType == TransactionType.OWED_BY_THEM || txType == TransactionType.PAYMENT_TO_THEM) {
                    totalDebtsBase = totalDebtsBase.add(pureBaseAmount)
                } else if (txType == TransactionType.PAYMENT_BY_THEM || txType == TransactionType.OWED_TO_THEM) {
                    totalPaymentsBase = totalPaymentsBase.add(pureBaseAmount)
                }
            }

            processedList.add(
                ProcessedTransaction(
                    tx = tx,
                    resolvedCurrency = resolvedCurrency,
                    resolvedAmount = resolvedAmount,
                    isTxForeign = isTxForeign,
                    baseCurrencyAmount = baseCurrencyAmount,
                    pureBaseAmount = pureBaseAmount
                )
            )
        }

        val owedBy = calcResult.owedByThemBDMap[normDefaultSymbol] ?: BigDecimal.ZERO
        val payTo = calcResult.paymentToThemBDMap[normDefaultSymbol] ?: BigDecimal.ZERO
        val totalDebts = owedBy.add(payTo)

        val payBy = calcResult.paymentByThemBDMap[normDefaultSymbol] ?: BigDecimal.ZERO
        val owedTo = calcResult.owedToThemBDMap[normDefaultSymbol] ?: BigDecimal.ZERO
        val totalPayments = payBy.add(owedTo)

        val calculatedNetDebt = calcResult.netDebtBigDecimalMap[normDefaultSymbol] ?: BigDecimal.ZERO
        val uncalculatedForeignSums = calcResult.netDebtBigDecimalMap.filterKeys { key ->
            key != normDefaultSymbol && ((calcResult.netDebtBigDecimalMap[key] ?: BigDecimal.ZERO).compareTo(BigDecimal.ZERO) != 0)
        }

        val hasMultipleCurrencies = uncalculatedForeignSums.isNotEmpty() || processedList.any { pt -> pt.isTxForeign }

        return SingleCustomerPdfSummary(
            sortedProcessedTxs = processedList,
            totalDebts = totalDebts,
            totalPayments = totalPayments,
            totalDebtsBase = totalDebtsBase,
            totalPaymentsBase = totalPaymentsBase,
            calculatedNetDebt = calculatedNetDebt,
            uncalculatedForeignSums = uncalculatedForeignSums,
            hasMultipleCurrencies = hasMultipleCurrencies
        )
    }

    fun calculateComprehensiveReport(
        customers: List<CustomerUiState>
    ): ComprehensivePdfSummary {
        var totalOwedByThem = BigDecimal.ZERO
        var totalOwedToThem = BigDecimal.ZERO
        val foreignTotalsMap = mutableMapOf<String, BigDecimal>()

        for (c in customers) {
            val bdVal = c.defaultCurrencyTotal
            if (bdVal.compareTo(BigDecimal.ZERO) > 0) {
                totalOwedByThem = totalOwedByThem.add(bdVal)
            } else if (bdVal.compareTo(BigDecimal.ZERO) < 0) {
                totalOwedToThem = totalOwedToThem.add(bdVal.abs())
            }
            for ((curr, valBd) in c.foreignDebts) {
                if (valBd.compareTo(BigDecimal.ZERO) != 0) {
                    foreignTotalsMap[curr] = (foreignTotalsMap[curr] ?: BigDecimal.ZERO).add(valBd)
                }
            }
        }

        val netPrimary = totalOwedByThem.subtract(totalOwedToThem)

        return ComprehensivePdfSummary(
            totalOwedByThem = totalOwedByThem,
            totalOwedToThem = totalOwedToThem,
            netPrimary = netPrimary,
            foreignTotalsMap = foreignTotalsMap
        )
    }
}
