package com.example.ui.screens.ledger.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entities.TransactionDb
import com.example.domain.model.TransactionType
import com.example.ui.theme.SelectionGreen
import com.example.ui.theme.financialCreditColor
import com.example.ui.theme.financialDebtColor
import com.example.ui.viewmodel.DayLedger
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun String.toWesternDigits(): String {
    var result = this
    val eastern = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    val western = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    for (i in 0..9) {
        result = result.replace(eastern[i], western[i])
    }
    return result
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DayCard(
    dayLedger: DayLedger,
    dayKey: String,
    isDaySelected: Boolean,
    isDaySelectionMode: Boolean,
    isExpanded: Boolean,
    haptic: HapticFeedback,
    currencySymbol: String,
    formatCurrency: (BigDecimal, String) -> String,
    onDeleteTransaction: (String) -> Unit,
    onEditTransaction: (TransactionDb) -> Unit,
    onDayClick: (String) -> Unit,
    onDayLongClick: (String) -> Unit,
    isSelectionMode: Boolean = false,
    selectedTxIds: List<String> = emptyList(),
    onTransactionSelectToggle: (String) -> Unit = {}
) {
    var txIdToDelete by remember { mutableStateOf<String?>(null) }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Format: [اسم اليوم] [اليوم/الشهر] (مثال: الأربعاء 19/08)
    val formattedDateHeader = remember(dayLedger) {
        val tx = dayLedger.transactions.firstOrNull()
        if (tx != null) {
            val ms = if (tx.timestamp in 1..999999999999L) tx.timestamp * 1000L else tx.timestamp
            try {
                val sdf = SimpleDateFormat("EEEE dd/MM", Locale.forLanguageTag("ar"))
                sdf.format(Date(ms)).toWesternDigits()
            } catch (e: Exception) {
                "${dayLedger.dayOfWeek} ${String.format(Locale.ENGLISH, "%02d", dayLedger.dayNumber)}"
            }
        } else {
            "${dayLedger.dayOfWeek} ${String.format(Locale.ENGLISH, "%02d", dayLedger.dayNumber)}"
        }
    }

    val formattedNetAmount = remember(dayLedger.netAmount, currencySymbol) {
        val sign = if (dayLedger.netAmount.compareTo(BigDecimal.ZERO) > 0) "▲ +"
        else if (dayLedger.netAmount.compareTo(BigDecimal.ZERO) < 0) "▼ "
        else ""
        sign + formatCurrency(dayLedger.netAmount, currencySymbol).toWesternDigits()
    }

    val netHeaderColor = remember(dayLedger.netAmount, isDark) {
        if (dayLedger.netAmount.compareTo(BigDecimal.ZERO) > 0) financialCreditColor(isDark)
        else if (dayLedger.netAmount.compareTo(BigDecimal.ZERO) < 0) financialDebtColor(isDark)
        else if (isDark) Color.LightGray else Color.Gray
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDaySelected) {
                if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else Color(0xFFF3F0FF)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isDaySelected) {
            BorderStroke(1.5.dp, SelectionGreen)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 2.5.dp)
            .combinedClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDayClick(dayKey)
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDayLongClick(dayKey)
                }
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Sleek Day Card Header
            DayCardHeader(
                formattedDateHeader = formattedDateHeader,
                formattedNetAmount = formattedNetAmount,
                netHeaderColor = netHeaderColor,
                isExpanded = isExpanded,
                isDaySelected = isDaySelected,
                isDaySelectionMode = isDaySelectionMode
            )

            // 2. Expandable Body: Dense Transactions List, 3-Metric Summary Bar, and WhatsApp Share Button
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(1.5.dp)
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(bottom = 1.dp)
                    )

                    val sortedTxs = remember(dayLedger.transactions) {
                        dayLedger.transactions.sortedByDescending { it.timestamp }
                    }

                    if (sortedTxs.isEmpty()) {
                        Text(
                            text = stringResource(id = R.string.ledger_no_txs_today),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        // High-Density Clean Transaction Rows
                        sortedTxs.forEachIndexed { index, tx ->
                            val isSelected = selectedTxIds.contains(tx.id)

                            if (index > 0) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f),
                                    thickness = 0.4.dp,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                            }

                            DayCardTransactionRow(
                                tx = tx,
                                isSelected = isSelected,
                                isSelectionMode = isSelectionMode,
                                isDark = isDark,
                                currencySymbol = currencySymbol,
                                formatCurrency = formatCurrency,
                                haptic = haptic,
                                onEditTransaction = onEditTransaction,
                                onDeleteRequest = { id -> txIdToDelete = id },
                                onTransactionSelectToggle = onTransactionSelectToggle
                            )
                        }

                        // Calculations: Daily Totals
                        val (dailyIncome, dailyExpense) = remember(dayLedger.transactions) {
                            var inc = BigDecimal.ZERO
                            var exp = BigDecimal.ZERO
                            for (t in dayLedger.transactions) {
                                if (t.type == TransactionType.INCOME.value) {
                                    inc = inc.add(t.amount)
                                } else if (t.type == TransactionType.EXPENSE.value) {
                                    exp = exp.add(t.amount)
                                }
                            }
                            Pair(inc, exp)
                        }
                        val dailyNet = remember(dailyIncome, dailyExpense) {
                            dailyIncome.subtract(dailyExpense)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            // 3. Compact 3-Metric Summary Bar
                            DayCardSummaryBar(
                                dailyIncome = dailyIncome,
                                dailyExpense = dailyExpense,
                                dailyNet = dailyNet,
                                isDark = isDark,
                                currencySymbol = currencySymbol,
                                formatCurrency = formatCurrency
                            )

                            // 4. WhatsApp Share Action Button
                            DayCardWhatsAppShareButton(
                                dayLedger = dayLedger,
                                dailyIncome = dailyIncome,
                                dailyExpense = dailyExpense,
                                currencySymbol = currencySymbol,
                                formatCurrency = formatCurrency
                            )
                        }
                    }
                }
            }
        }
    }

    if (txIdToDelete != null) {
        DayCardDeleteDialog(
            txId = txIdToDelete!!,
            onConfirm = { id ->
                onDeleteTransaction(id)
                txIdToDelete = null
            },
            onDismiss = { txIdToDelete = null }
        )
    }
}
