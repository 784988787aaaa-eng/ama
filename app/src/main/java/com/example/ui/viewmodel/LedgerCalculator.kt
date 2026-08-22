package com.example.ui.viewmodel

import com.example.data.local.entities.TransactionDb
import com.example.domain.DateUtils
import com.example.domain.model.TransactionType
import java.math.BigDecimal

object LedgerCalculator {

    fun computeMonthlyLedger(txList: List<TransactionDb>): List<MonthLedger> {
        val chronicTx = txList.sortedBy { it.timestamp }
        val groupedByMonth = chronicTx.groupBy { DateUtils.getYearMonthKey(it.timestamp) }
        val sortedMonthKeys = groupedByMonth.keys.sorted()

        var runningForwardedBalance = BigDecimal.ZERO
        val ledgerList = mutableListOf<MonthLedger>()

        for (monthKey in sortedMonthKeys) {
            val monthTx = groupedByMonth[monthKey] ?: emptyList()
            if (monthTx.isEmpty()) continue
            val monthName = DateUtils.getMonthNameArabic(monthTx.first().timestamp)

            val groupedByDay = monthTx.groupBy { DateUtils.getDayOfMonth(it.timestamp) }
            val sortedDays = groupedByDay.keys.sortedDescending()

            val dayItems = mutableListOf<DayLedger>()
            var monthIncomes = BigDecimal.ZERO
            var monthExpenses = BigDecimal.ZERO

            for (day in sortedDays) {
                val dayTx = groupedByDay[day] ?: emptyList()
                if (dayTx.isEmpty()) continue
                val dayTimestamp = dayTx.first().timestamp
                val dayDateText = DateUtils.formatDateFull(dayTimestamp)
                val dayOfWeek = DateUtils.getDayOfWeekArabic(dayTimestamp)

                var dayIncome = BigDecimal.ZERO
                var dayExpense = BigDecimal.ZERO
                for (tx in dayTx) {
                    val txType = TransactionType.fromValue(tx.type)
                    if (txType == TransactionType.INCOME) {
                        dayIncome = dayIncome.add(tx.amount)
                    } else {
                        dayExpense = dayExpense.add(tx.amount)
                    }
                }
                val netDay = dayIncome.subtract(dayExpense)

                dayItems.add(
                    DayLedger(
                        dayNumber = day,
                        dayOfWeek = dayOfWeek,
                        fullDate = dayDateText,
                        netAmount = netDay,
                        transactions = dayTx.sortedByDescending { it.timestamp }
                    )
                )

                monthIncomes = monthIncomes.add(dayIncome)
                monthExpenses = monthExpenses.add(dayExpense)
            }

            val currentMonthNet = monthIncomes.subtract(monthExpenses)
            val totalForwarded = runningForwardedBalance
            val monthFinalBalance = totalForwarded.add(currentMonthNet)

            ledgerList.add(
                MonthLedger(
                    monthKey = monthKey,
                    monthName = monthName,
                    forwardedBalance = totalForwarded,
                    netAmount = currentMonthNet,
                    finalBalance = monthFinalBalance,
                    days = dayItems
                )
            )

            runningForwardedBalance = monthFinalBalance
        }

        return ledgerList.sortedByDescending { it.monthKey }
    }
}

