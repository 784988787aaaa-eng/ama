package com.example.ui.viewmodel.ledger

import androidx.compose.runtime.Immutable
import com.example.data.local.entities.TransactionDb
import java.math.BigDecimal

@Immutable
data class MonthLedger(
    val monthKey: String,
    val monthName: String,
    val forwardedBalance: BigDecimal,
    val netAmount: BigDecimal,
    val finalBalance: BigDecimal,
    val days: List<DayLedger>
)

@Immutable
data class DayLedger(
    val dayNumber: Int,
    val dayOfWeek: String,
    val fullDate: String,
    val netAmount: BigDecimal,
    val transactions: List<TransactionDb>
)
