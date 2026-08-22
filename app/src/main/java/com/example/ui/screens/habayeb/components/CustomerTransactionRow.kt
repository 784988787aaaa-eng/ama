package com.example.ui.screens.habayeb.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.local.entities.HabayebTransaction
import com.example.ui.screens.habayeb.components.row.CustomerTransactionRowStateCalculator
import com.example.ui.screens.habayeb.components.row.TransactionRowAmountSection
import com.example.ui.screens.habayeb.components.row.TransactionRowDateSection
import com.example.ui.screens.habayeb.components.row.TransactionRowDetailsSection

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomerTransactionRow(
    tx: HabayebTransaction,
    isDark: Boolean,
    currencySymbol: String,
    initialType: String,
    isSelected: Boolean,
    isTxMultiSelectActive: Boolean,
    hasActiveRecurring: Boolean,
    txSeqNo: Int,
    parentTxSeq: Int?,
    activeThemeColor: Color,
    onSelectToggle: (String) -> Unit,
    onLongClick: (String) -> Unit,
    onOptionsClick: (HabayebTransaction) -> Unit,
    onScheduleClick: (HabayebTransaction) -> Unit,
    onExchangeRateClick: (HabayebTransaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val cached = remember(tx, isDark, currencySymbol, initialType) {
        CustomerTransactionRowStateCalculator.calculate(tx, isDark, currencySymbol, initialType)
    }

    val rowBgColor = if (isSelected) {
        activeThemeColor.copy(alpha = if (isDark) 0.20f else 0.12f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderColor = if (isSelected) activeThemeColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    val onCardClick = remember(tx, isTxMultiSelectActive, onSelectToggle, onOptionsClick) {
        {
            if (isTxMultiSelectActive) {
                onSelectToggle(tx.id)
            } else {
                onOptionsClick(tx)
            }
        }
    }
    val onCardLongClick = remember(tx.id, onLongClick) {
        { onLongClick(tx.id) }
    }

    val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .graphicsLayer {
                shadowElevation = if (isSelected) 3f else 1f
                shape = RoundedCornerShape(8.dp)
                clip = true
            }
            .drawBehind {
                val barWidth = 4.dp.toPx()
                val xOffset = if (layoutDirection == LayoutDirection.Rtl) {
                    size.width - barWidth
                } else {
                    0f
                }
                drawRect(
                    color = cached.indicatorColor,
                    topLeft = androidx.compose.ui.geometry.Offset(xOffset, 0f),
                    size = androidx.compose.ui.geometry.Size(barWidth, size.height)
                )
            }
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = onCardLongClick
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = rowBgColor),
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Date/Time (Rightmost)
            TransactionRowDateSection(
                isSelected = isSelected,
                txSeqNo = txSeqNo,
                hasActiveRecurring = hasActiveRecurring,
                activeThemeColor = activeThemeColor,
                cached = cached,
                isDark = isDark,
                tx = tx,
                onScheduleClick = onScheduleClick,
                modifier = Modifier.weight(1.0f)
            )

            // 2. Details (Middle-Right)
            TransactionRowDetailsSection(
                cached = cached,
                isDark = isDark,
                hasActiveRecurring = hasActiveRecurring,
                parentTxSeq = parentTxSeq,
                tx = tx,
                currencySymbol = currencySymbol,
                onExchangeRateClick = onExchangeRateClick,
                modifier = Modifier.weight(2.2f)
            )

            // 3. Amount with colorful indicator arrow (Middle-Left)
            TransactionRowAmountSection(
                cached = cached,
                modifier = Modifier.weight(1.0f)
            )
        }
    }
}
