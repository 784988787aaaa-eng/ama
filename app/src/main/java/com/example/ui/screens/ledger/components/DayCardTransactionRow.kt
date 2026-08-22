package com.example.ui.screens.ledger.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entities.TransactionDb
import com.example.domain.DateUtils
import com.example.domain.model.TransactionType
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SoftRed
import com.example.ui.theme.financialCreditColor
import com.example.ui.theme.financialDebtColor
import java.math.BigDecimal

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
fun DayCardTransactionRow(
    tx: TransactionDb,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    isDark: Boolean,
    currencySymbol: String,
    formatCurrency: (BigDecimal, String) -> String,
    haptic: HapticFeedback,
    onEditTransaction: (TransactionDb) -> Unit,
    onDeleteRequest: (String) -> Unit,
    onTransactionSelectToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isIncome = tx.type == TransactionType.INCOME.value
    val txAmountColor = if (isIncome) financialCreditColor(isDark) else financialDebtColor(isDark)
    val txAmountSign = if (isIncome) "▲ +" else "▼ -"
    val formattedTxAmount = txAmountSign + formatCurrency(tx.amount, currencySymbol).toWesternDigits()
    val formattedTxTime = remember(tx.timestamp) {
        DateUtils.formatTime24Or12(tx.timestamp).toWesternDigits()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(5.dp))
            .background(if (isSelected) EmeraldPrimary.copy(alpha = if (isDark) 0.18f else 0.12f) else Color.Transparent)
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTransactionSelectToggle(tx.id)
                    }
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTransactionSelectToggle(tx.id)
                }
            )
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Right in RTL (Start): 16dp Sleek Vector Icon + Description & Time
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            // Selection Mode Checkbox
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .size(15.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) EmeraldPrimary else Color.Transparent)
                        .border(
                            1.2.dp,
                            if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            CircleShape
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTransactionSelectToggle(tx.id)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(8.dp)
                        )
                    }
                }
            }

            // Minimal Vector Directional Icon (16dp)
            Icon(
                imageVector = if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                contentDescription = null,
                tint = txAmountColor,
                modifier = Modifier.size(16.dp)
            )

            // Description (Line 1) and Time (Line 2) with zero wasted space
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Text(
                    text = tx.description.ifBlank {
                        if (isIncome) stringResource(id = R.string.transaction_income)
                        else stringResource(id = R.string.transaction_expense)
                    },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formattedTxTime,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f)
                )
            }
        }

        // Left in RTL (End): Amount + Compact Actions (Edit & Delete)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Amount Text
            Text(
                text = formattedTxAmount,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = txAmountColor
            )

            // Edit icon button (Ultra compact & clean)
            IconButton(
                modifier = Modifier.size(20.dp),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEditTransaction(tx)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(id = R.string.ledger_edit_transaction_title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.40f),
                    modifier = Modifier.size(11.5.dp)
                )
            }

            // Delete icon button (Ultra compact & clean)
            IconButton(
                modifier = Modifier.size(20.dp),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDeleteRequest(tx.id)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(id = R.string.ledger_commitment_delete),
                    tint = SoftRed.copy(alpha = 0.50f),
                    modifier = Modifier.size(11.5.dp)
                )
            }
        }
    }
}
