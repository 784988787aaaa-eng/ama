package com.example.ui.screens.trash.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entities.DeletedItemEntity
import com.example.ui.helper.getInitialColor
import com.example.ui.screens.trash.utils.ParsedTrashData
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.DebtRed
import com.example.ui.theme.financialCreditColor
import com.example.ui.theme.financialDebtColor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrashItemCard(
    item: DeletedItemEntity,
    parsedData: ParsedTrashData,
    isSelected: Boolean,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit,
    onOpenCustomerOverlay: () -> Unit = {},
    onOpenTransactionDetail: () -> Unit = {}
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val creditColor = financialCreditColor(isDark)
    val debtColor = financialDebtColor(isDark)

    val isBundleOrCustomer = item.originalTableName == "habayeb_bundle" || item.originalTableName == "habayeb_customers"
    val isTransaction = item.originalTableName == "habayeb_transactions"

    val avatarKey = if (isTransaction && parsedData.customerName.isNotEmpty()) {
        parsedData.customerName
    } else {
        parsedData.titleText
    }
    val avatarColor = remember(avatarKey) { getInitialColor(avatarKey) }
    val firstLetter = remember(avatarKey) {
        avatarKey.trim().firstOrNull()?.toString()?.uppercase() ?: "؟"
    }

    val transactionLabel = stringResource(R.string.trash_type_transaction)
    val accountLabel = stringResource(R.string.trash_type_account)
    val typeLabel = remember(item.originalTableName, transactionLabel, accountLabel) {
        when (item.originalTableName) {
            "habayeb_transactions" -> transactionLabel
            "habayeb_customers", "habayeb_bundle" -> accountLabel
            else -> item.originalTableName
        }
    }

    val cardBorder = if (isSelected) {
        BorderStroke(1.5.dp, primaryColor)
    } else {
        BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.35f else 0.25f))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onClick()
                    } else {
                        if (isBundleOrCustomer) {
                            onOpenCustomerOverlay()
                        } else if (isTransaction) {
                            onOpenTransactionDetail()
                        } else {
                            onClick()
                        }
                    }
                },
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                primaryColor.copy(alpha = if (isDark) 0.2f else 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 0.dp else 1.dp),
        border = cardBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Start: Avatar & Text Content
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar Circle or Selection Checkmark
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(primaryColor)
                            .border(1.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(avatarColor.copy(alpha = 0.12f))
                            .border(1.dp, avatarColor.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = firstLetter,
                            color = avatarColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Middle Text Hierarchy: Title, Customer/Account Line, Date & Foreign Metadata
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Line 1: Title & Type Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = parsedData.titleText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = primaryColor.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = typeLabel,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }

                    // Line 2: Full Context / Associated Account Subtext (Clean & Unsquished)
                    if (parsedData.subText.isNotEmpty()) {
                        Text(
                            text = parsedData.subText,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Line 3: Date & Foreign Currency Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = parsedData.parsedDate,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            maxLines = 1
                        )

                        if (parsedData.isForeign) {
                            val badgeColor = if (parsedData.isRateCalculated) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                            val badgeBg = if (parsedData.isRateCalculated) primaryColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
                            val badgeText = if (parsedData.isRateCalculated) {
                                stringResource(R.string.trash_exchange_active, parsedData.exchangeRateVal)
                            } else {
                                stringResource(R.string.trash_exchange_inactive)
                            }

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = badgeBg
                            ) {
                                Text(
                                    text = badgeText,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // End: Financial Amount & Clean Frameless Icon Actions
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Financial Amount with directional arrow
                if (parsedData.amountText.isNotEmpty()) {
                    val amountColor = if (parsedData.isExpense) debtColor else creditColor
                    val arrow = if (parsedData.isExpense) "↗️" else "↙️"

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = parsedData.amountText,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Black,
                            color = amountColor
                        )
                        if (isTransaction) {
                            Text(
                                text = arrow,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Actions: Sleek, Frameless, Clean Icon Buttons (Zero Bloat, Zero Clunky Frames)
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(primaryColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else if (!isSelectionMode) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Frameless Clean Restore Icon Button
                        IconButton(
                            onClick = onRestore,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestoreFromTrash,
                                contentDescription = stringResource(id = R.string.trash_action_restore_btn),
                                tint = CreditGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Frameless Clean Delete Permanently Icon Button
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = stringResource(id = R.string.trash_delete_permanently),
                                tint = DebtRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            confirmButton = {
                Button(
                    onClick = {
                        onPermanentDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = errorColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.trash_delete_permanently),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(
                        text = stringResource(id = R.string.trash_cancel),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            title = {
                Text(
                    text = stringResource(id = R.string.trash_delete_warning_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.trash_delete_warning_desc),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
