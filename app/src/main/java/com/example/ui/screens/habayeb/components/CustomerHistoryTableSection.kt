package com.example.ui.screens.habayeb.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entities.HabayebCustomer
import com.example.data.local.entities.HabayebTransaction
import java.math.BigDecimal

@Composable
fun CustomerHistoryTableSection(
    displayedTxs: List<HabayebTransaction>,
    listState: LazyListState,
    txSearchQuery: String,
    activeCustomer: HabayebCustomer,
    isDark: Boolean,
    currencySymbol: String,
    runningBalances: Map<String, BigDecimal>,
    activeRecurringTxIds: Set<String>,
    txSequenceNumbers: Map<String, Int>,
    selectedTxIds: List<String>,
    isTxMultiSelectActive: Boolean,
    activeThemeColor: Color,
    contentPadding: PaddingValues,
    onSelectToggle: (String) -> Unit,
    onLongClick: (String) -> Unit,
    onOptionsClick: (HabayebTransaction) -> Unit,
    onScheduleClick: (HabayebTransaction) -> Unit,
    onExchangeRateClick: (HabayebTransaction) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // HIGH-DENSITY HIGH-FIDELITY TRANSACTION LIST DIRECTLY BELOW BALANCE CARDS
        if (displayedTxs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (txSearchQuery.isEmpty()) stringResource(id = R.string.habayeb_no_tx_recorded) else stringResource(id = R.string.habayeb_no_search_results),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                contentPadding = PaddingValues(
                    top = 4.dp,
                    bottom = contentPadding.calculateBottomPadding() + 80.dp
                )
            ) {
                items(displayedTxs, key = { it.id }, contentType = { "tx_row" }) { tx ->
                    val isSelected = selectedTxIds.contains(tx.id)
                    val hasActiveRecurring = tx.id in activeRecurringTxIds
                    val txSeqNo = txSequenceNumbers[tx.id] ?: 0
                    val parentTxSeq = remember(tx.linkedMainTxId, txSequenceNumbers, tx.id) {
                        val linkedId = tx.linkedMainTxId?.trim()
                        if (!linkedId.isNullOrBlank() && !linkedId.equals("null", ignoreCase = true) && linkedId != "0" && linkedId != tx.id) {
                            txSequenceNumbers[linkedId]
                        } else null
                    }

                    CustomerTransactionRow(
                        tx = tx,
                        isDark = isDark,
                        currencySymbol = currencySymbol,
                        initialType = activeCustomer.initialType,
                        isSelected = isSelected,
                        isTxMultiSelectActive = isTxMultiSelectActive,
                        hasActiveRecurring = hasActiveRecurring,
                        txSeqNo = txSeqNo,
                        parentTxSeq = parentTxSeq,
                        activeThemeColor = activeThemeColor,
                        onSelectToggle = onSelectToggle,
                        onLongClick = onLongClick,
                        onOptionsClick = onOptionsClick,
                        onScheduleClick = onScheduleClick,
                        onExchangeRateClick = onExchangeRateClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
