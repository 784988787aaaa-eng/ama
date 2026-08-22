package com.example.ui.screens.trash.components

import java.math.BigDecimal

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entities.DeletedItemEntity
import com.example.ui.helper.HabayebMathHelper
import com.example.ui.helper.getInitialColor
import com.example.ui.screens.trash.utils.ParsedTrashData
import com.example.ui.theme.financialCreditColor
import com.example.ui.theme.financialDebtColor

@Composable
fun TrashCustomerHistoryOverlay(
    item: DeletedItemEntity,
    parsedData: ParsedTrashData,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onRestoreFullAccount: () -> Unit,
    onDeleteFullAccountPermanently: () -> Unit,
    onRestoreSingleTx: (String) -> Unit
) {
    BackHandler { onDismiss() }

    var showConfirmDeleteAccount by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val isDark = MaterialTheme.colorScheme.background.run { red < 0.5f }
    val creditColor = financialCreditColor(isDark)
    val debtColor = financialDebtColor(isDark)
    val avatarColor = remember(parsedData.titleText) { getInitialColor(parsedData.titleText) }
    val firstLetter = remember(parsedData.titleText) {
        parsedData.titleText.trim().firstOrNull()?.toString()?.uppercase() ?: "؟"
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = stringResource(id = R.string.trash_back),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(avatarColor.copy(alpha = 0.15f))
                                .border(1.dp, avatarColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = firstLetter,
                                color = avatarColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column {
                            Text(
                                text = parsedData.titleText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Top Action Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onRestoreFullAccount,
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestoreFromTrash,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(id = R.string.trash_action_restore_btn),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        IconButton(
                            onClick = { showConfirmDeleteAccount = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(errorColor.copy(alpha = 0.1f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = null,
                                tint = errorColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Multi-Currency Breakdown Summary Bar
            if (parsedData.currencyBreakdown.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    parsedData.currencyBreakdown.forEach { (curr, sum) ->
                        val isNeg = sum < BigDecimal.ZERO
                        val chipBg = if (isNeg) debtColor.copy(alpha = 0.1f) else creditColor.copy(alpha = 0.1f)
                        val chipText = if (isNeg) debtColor else creditColor
                        val formattedSum = com.example.ui.helper.HabayebMathHelper.formatSmart(sum.abs())

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = chipBg,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, chipText.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (sum.compareTo(BigDecimal.ZERO) == 0) stringResource(id = R.string.trash_status_balanced) else if (isNeg) stringResource(id = R.string.trash_status_due_prefix) else stringResource(id = R.string.trash_status_owed_prefix),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = chipText
                                )
                                Text(
                                    text = "$formattedSum $curr",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = chipText
                                )
                            }
                        }
                    }
                }
            }

            // Transactions List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(parsedData.bundleTransactions, key = { it.id }) { tx ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = tx.description,
                                    fontSize = 13.sp,
                                    fontWeight = if (!tx.hasNotes) FontWeight.Normal else FontWeight.Bold,
                                    color = if (!tx.hasNotes) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = tx.dateText,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )

                                    if (tx.exchangeRateText.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(creditColor.copy(alpha = 0.12f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = stringResource(id = R.string.trash_rate_active_format, tx.exchangeRateText),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = creditColor
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    val isNegative = tx.isNegative
                                    val txColor = if (isNegative) debtColor else creditColor
                                    val arrowSymbol = if (isNegative) "↗" else "↙"

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = tx.displayAmountText,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = txColor
                                        )
                                        Text(
                                            text = arrowSymbol,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = txColor
                                        )
                                    }

                                    if (tx.equivalentAmountText.isNotEmpty()) {
                                        Text(
                                            text = tx.equivalentAmountText,
                                            fontSize = 11.sp,
                                            color = txColor.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onRestoreSingleTx(tx.id) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(primaryColor.copy(alpha = 0.12f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Restore,
                                        contentDescription = stringResource(id = R.string.trash_action_restore_btn),
                                        tint = primaryColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConfirmDeleteAccount) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteAccount = false },
            title = {
                Text(
                    text = stringResource(id = R.string.trash_delete_warning_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(text = stringResource(id = R.string.trash_delete_warning_desc))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDeleteAccount = false
                        onDeleteFullAccountPermanently()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = errorColor)
                ) {
                    Text(text = stringResource(id = R.string.trash_delete_permanently))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteAccount = false }) {
                    Text(text = stringResource(id = R.string.trash_cancel))
                }
            }
        )
    }
}
