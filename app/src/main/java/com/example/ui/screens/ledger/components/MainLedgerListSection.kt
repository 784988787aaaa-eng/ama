package com.example.ui.screens.ledger.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entities.FixedCommitment
import com.example.data.local.entities.TransactionDb
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.MonthLedger
import java.math.BigDecimal

@Composable
fun MainLedgerListSection(
    lazyListState: LazyListState,
    bottomPadding: Dp,
    isDaySelectionMode: Boolean,
    selectedDayKeys: List<String>,
    formatCurrency: (BigDecimal, String) -> String,
    formatDoubleCurrency: (Double, String) -> String,
    currencySymbol: String,
    monthlyLedger: List<MonthLedger>,
    isScreenReady: Boolean,
    collapsedMonths: Set<String>,
    onToggleMonthCollapsed: (String) -> Unit,
    expandedDayKeys: Set<String>,
    haptic: HapticFeedback,
    context: Context,
    viewModel: FinanceViewModel,
    onEditTransaction: (TransactionDb) -> Unit,
    onDayClick: (String) -> Unit,
    onDayLongClick: (String) -> Unit,
    isSelectionMode: Boolean,
    selectedTxIds: MutableList<String>,
    onTransactionSelectToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 4.dp,
            bottom = bottomPadding + 110.dp
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Empty state placeholder
        if (monthlyLedger.isEmpty()) {
            item(key = "empty_state") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("📓", fontSize = 56.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(id = R.string.ledger_empty_state_msg),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Deferred loading skeleton
        if (!isScreenReady && monthlyLedger.isNotEmpty()) {
            item(key = "loading_skeleton") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = EmeraldPrimary,
                        strokeWidth = 3.dp
                    )
                }
            }
        } else {
            // Ledger Month-by-month list
            monthlyLedger.forEachIndexed { monthIdx, monthLedger ->
                val isCollapsed = collapsedMonths.contains(monthLedger.monthKey)

                // Month Header
                item(key = "header_${monthLedger.monthKey}") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onToggleMonthCollapsed(monthLedger.monthKey)
                            }
                            .padding(
                                start = 14.dp,
                                end = 14.dp,
                                top = if (monthIdx == 0) 2.dp else 12.dp,
                                bottom = 2.dp
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(end = 4.dp)
                            )
                            Text(
                                text = if (monthIdx == 0) stringResource(id = R.string.ledger_daily_record) else stringResource(id = R.string.ledger_monthly_record),
                                color = EmeraldPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = monthLedger.monthName,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                    }
                }

                if (!isCollapsed) {
                    items(
                        monthLedger.days,
                        key = { dayLedger -> "${monthLedger.monthKey}_${dayLedger.dayNumber}" }
                    ) { dayLedger ->
                        val dayKey = "${monthLedger.monthKey}_${dayLedger.dayNumber}"
                        val isDaySelected = selectedDayKeys.contains(dayKey)

                        DayCard(
                            dayLedger = dayLedger,
                            dayKey = dayKey,
                            isDaySelected = isDaySelected,
                            isDaySelectionMode = isDaySelectionMode,
                            isExpanded = expandedDayKeys.contains(dayKey),
                            haptic = haptic,
                            currencySymbol = currencySymbol,
                            formatCurrency = formatCurrency,
                            onDeleteTransaction = { txId -> viewModel.deleteTransactionById(txId) },
                            onEditTransaction = onEditTransaction,
                            onDayClick = onDayClick,
                            onDayLongClick = onDayLongClick,
                            isSelectionMode = isSelectionMode,
                            selectedTxIds = selectedTxIds,
                            onTransactionSelectToggle = onTransactionSelectToggle
                        )
                    }
                }

                if (monthIdx < monthlyLedger.size - 1) {
                    item(key = "transition_${monthLedger.monthKey}") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            MonthTransitionLine()
                        }
                    }
                }
            }
        }
    }
}
