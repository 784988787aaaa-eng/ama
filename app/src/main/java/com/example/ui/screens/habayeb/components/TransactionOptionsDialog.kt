package com.example.ui.screens.habayeb.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.local.entities.HabayebTransaction
import com.example.domain.FormatUtils
import com.example.domain.model.TransactionType
import com.example.ui.screens.habayeb.utils.CurrencyConfig
import com.example.ui.theme.SoftGreen
import com.example.ui.theme.SoftRed

private data class TransactionHeaderSummary(
    val shortDesc: String,
    val formattedAmount: String,
    val currencySymbol: String,
    val amountColor: Color
)

@Composable
fun TransactionOptionsDialog(
    transaction: HabayebTransaction,
    customerName: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAutoRepeat: () -> Unit,
    onWhatsAppShare: () -> Unit,
    onSmsShare: () -> Unit,
    activeThemeColor: Color,
    activeSubColor: Color,
    isRecurringOriginal: Boolean = false,
    onDeleteAutoRepeat: (() -> Unit)? = null,
    parentSeqNumber: Int? = null
) {
    val bgColor = MaterialTheme.colorScheme.background
    val isDark = remember(bgColor) { bgColor.red < 0.5f }
    var showShareMenu by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Raised Header: Left is Smart Share dropdown, Right is transaction details tag
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Smart Share icon with popup dropdown
                        Box {
                             IconButton(
                                onClick = { showShareMenu = true },
                                modifier = Modifier.size(36.dp)
                             ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = stringResource(id = R.string.share_label),
                                    tint = activeThemeColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            DropdownMenu(
                                expanded = showShareMenu,
                                onDismissRequest = { showShareMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.share_whatsapp), fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        showShareMenu = false
                                        onWhatsAppShare()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Chat,
                                            contentDescription = null,
                                            tint = if (isDark) SoftGreen else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.share_sms), fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        showShareMenu = false
                                        onSmsShare()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Sms,
                                            contentDescription = null,
                                            tint = if (isDark) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                            }
                        }

                        // Right: Raised details tag
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            val defaultRial = stringResource(id = R.string.habayeb_currency_rial)
                            val primaryColor = MaterialTheme.colorScheme.primary
                            val errorColor = MaterialTheme.colorScheme.error

                            val headerSummary = remember(
                                transaction, customerName, isDark, defaultRial, primaryColor, errorColor
                            ) {
                                val desc = if (transaction.description.isNotBlank()) transaction.description else customerName
                                val cleanDesc = CurrencyConfig.getCleanDetails(desc).ifBlank { customerName }
                                val shortDescStr = if (cleanDesc.length > 20) cleanDesc.take(20) + "..." else cleanDesc

                                val txAmount = transaction.foreignAmount
                                val currencyStr = if (transaction.currencyCode.isNotBlank() && transaction.currencyCode != "DEFAULT") {
                                    transaction.currencyCode
                                } else {
                                    defaultRial
                                }
                                val formattedAmountStr = FormatUtils.formatDouble(txAmount.toDouble())
                                val isPositive = transaction.type == TransactionType.PAYMENT_BY_THEM.value || transaction.type == TransactionType.OWED_TO_THEM.value
                                val color = if (isPositive) {
                                    if (isDark) SoftGreen else primaryColor
                                } else {
                                    if (isDark) SoftRed else errorColor
                                }
                                TransactionHeaderSummary(shortDescStr, formattedAmountStr, currencyStr, color)
                            }

                            Text(
                                text = headerSummary.shortDesc,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = " • ",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            Text(
                                text = "${headerSummary.formattedAmount} ${headerSummary.currencySymbol}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = headerSummary.amountColor
                            )
                        }
                    }

                    // Warning / Status Banner for Recurring Relationships
                    if (isRecurringOriginal) {
                        val warningBg = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                        val warningBorder = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
                        val warningText = MaterialTheme.colorScheme.onTertiaryContainer
                        Surface(
                            color = warningBg,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, warningBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = warningText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(id = R.string.habayeb_recurring_warning_banner),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = warningText,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    } else if (parentSeqNumber != null && parentSeqNumber > 0) {
                        val infoBg = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                        val infoBorder = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                        val infoText = MaterialTheme.colorScheme.onSecondaryContainer
                        Surface(
                            color = infoBg,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, infoBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = infoText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(id = R.string.habayeb_auto_generated_desc, parentSeqNumber),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = infoText,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    // Compact Actions Row (All in 1 Row!)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Edit (تعديل)
                        ActionCircleItem(
                            title = stringResource(id = R.string.habayeb_action_edit),
                            icon = Icons.Default.Edit,
                            containerColor = activeThemeColor.copy(alpha = 0.12f),
                            iconColor = activeThemeColor,
                            onClick = onEdit
                        )

                        // 2. Delete (حذف)
                        ActionCircleItem(
                            title = stringResource(id = R.string.habayeb_action_delete),
                            icon = Icons.Default.Delete,
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            iconColor = MaterialTheme.colorScheme.error,
                            onClick = onDelete
                        )

                        if (isRecurringOriginal) {
                            // 3. Edit Auto-Repeat (تعديل تكرار)
                            ActionCircleItem(
                                title = stringResource(id = R.string.habayeb_action_edit_recurring),
                                icon = Icons.Default.Sync,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                iconColor = MaterialTheme.colorScheme.secondary,
                                onClick = onAutoRepeat
                            )

                            // 4. Stop Auto-Repeat (إلغاء تكرار)
                            ActionCircleItem(
                                title = stringResource(id = R.string.habayeb_action_cancel_recurring),
                                icon = Icons.Default.Schedule,
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                iconColor = MaterialTheme.colorScheme.error,
                                onClick = { onDeleteAutoRepeat?.invoke() }
                            )
                        } else {
                            // 3. Setup Auto-Repeat (جدولة تكرار)
                            ActionCircleItem(
                                title = stringResource(id = R.string.habayeb_action_schedule_recurring),
                                icon = Icons.Default.Sync,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                iconColor = MaterialTheme.colorScheme.secondary,
                                onClick = onAutoRepeat
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun ActionCircleItem(
    title: String,
    icon: ImageVector,
    containerColor: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .width(72.dp)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(containerColor, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = title,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 11.sp
        )
    }
}

