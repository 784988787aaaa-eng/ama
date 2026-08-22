package com.example.ui.screens.habayeb.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.local.entities.HabayebTransaction
import com.example.domain.FormatUtils
import com.example.ui.screens.habayeb.utils.HabayebRecurringManager
import com.example.ui.screens.habayeb.utils.RecurringConfig
import com.example.ui.viewmodel.FinanceConstants
import java.util.Calendar
import java.util.UUID

@Composable
fun RecurringTransactionPopup(
    transaction: HabayebTransaction,
    customerName: String,
    onDismiss: () -> Unit,
    activeThemeColor: Color,
    activeSubColor: Color
) {
    val context = LocalContext.current

    val existingConfigs = remember(transaction.id) { HabayebRecurringManager.getAllConfigs(context) }
    val existingConfig = remember(transaction.id) { existingConfigs.find { it.originalTxId == transaction.id } }

    var frequency by remember { mutableStateOf(existingConfig?.frequency ?: FinanceConstants.FREQ_DAILY) }
    var selectedDaysOfWeek by remember {
        mutableStateOf(existingConfig?.daysOfWeek?.toSet() ?: setOf(Calendar.MONDAY))
    }
    var selectedDaysOfMonth by remember {
        mutableStateOf(existingConfig?.daysOfMonth?.toSet() ?: setOf(1))
    }

    var hour by remember { mutableStateOf(existingConfig?.timeHour ?: 9) }
    var minute by remember { mutableStateOf(existingConfig?.timeMinute ?: 0) }

    var startDateMillis by remember { mutableStateOf(existingConfig?.startDateMillis ?: System.currentTimeMillis()) }
    var endDateMillis by remember {
        mutableStateOf(
            existingConfig?.endDateMillis ?: (System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = 520.dp)
                    .imePadding(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Header Banner (Refined and modern)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(activeThemeColor, activeSubColor)
                                )
                            )
                            .padding(vertical = 10.dp, horizontal = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(Color.White.copy(alpha = 0.22f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = stringResource(id = R.string.habayeb_recurring_title),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            val formattedAmount = remember(transaction.amount) {
                                FormatUtils.formatDouble(transaction.amount.toDouble())
                            }
                            val fallbackCurrency = stringResource(id = R.string.currency_yer)
                            val currStr = transaction.currencyCode.ifEmpty { fallbackCurrency }
                            Box(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$customerName • $formattedAmount $currStr",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. Frequency selector
                        RecurringFrequencySelector(
                            frequency = frequency,
                            onFrequencyChange = { frequency = it },
                            selectedDaysOfWeek = selectedDaysOfWeek,
                            onDaysOfWeekChange = { selectedDaysOfWeek = it },
                            selectedDaysOfMonth = selectedDaysOfMonth,
                            onDaysOfMonthChange = { selectedDaysOfMonth = it },
                            activeThemeColor = activeThemeColor,
                            isDark = false
                        )

                        // 2. Date and Time section (Intelligent Range & Time Picker)
                        RecurringDateTimeSection(
                            hour = hour,
                            minute = minute,
                            onTimeChange = { h, m -> hour = h; minute = m },
                            startDateMillis = startDateMillis,
                            onStartDateChange = { startDateMillis = it },
                            endDateMillis = endDateMillis,
                            onEndDateChange = { endDateMillis = it },
                            activeThemeColor = activeThemeColor
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // 3. Action Buttons
                        RecurringActionsRow(
                            existingConfig = existingConfig,
                            transaction = transaction,
                            customerName = customerName,
                            frequency = frequency,
                            selectedDaysOfWeek = selectedDaysOfWeek,
                            selectedDaysOfMonth = selectedDaysOfMonth,
                            hour = hour,
                            minute = minute,
                            startDateMillis = startDateMillis,
                            endDateMillis = endDateMillis,
                            activeThemeColor = activeThemeColor,
                            onDismiss = onDismiss
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecurringActionsRow(
    existingConfig: RecurringConfig?,
    transaction: HabayebTransaction,
    customerName: String,
    frequency: String,
    selectedDaysOfWeek: Set<Int>,
    selectedDaysOfMonth: Set<Int>,
    hour: Int,
    minute: Int,
    startDateMillis: Long,
    endDateMillis: Long,
    activeThemeColor: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier
                .weight(1f)
                .height(40.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = stringResource(id = R.string.habayeb_cancel),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (existingConfig != null) {
            OutlinedButton(
                onClick = {
                    HabayebRecurringManager.deleteConfig(context, existingConfig.id)
                    Toast.makeText(context, context.getString(R.string.habayeb_recurring_toast_stop_success), Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                modifier = Modifier
                    .weight(1.2f)
                    .height(40.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = stringResource(id = R.string.habayeb_recurring_btn_stop_short),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Button(
            onClick = {
                if (startDateMillis > endDateMillis) {
                    Toast.makeText(context, context.getString(R.string.habayeb_recurring_toast_date_error), Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val newConfig = RecurringConfig(
                    id = existingConfig?.id ?: "rec_tx_${UUID.randomUUID().toString().take(6)}",
                    originalTxId = transaction.id,
                    customerId = transaction.customerId,
                    customerName = customerName,
                    amount = transaction.amount,
                    type = transaction.type,
                    description = transaction.description,
                    frequency = frequency,
                    daysOfWeek = if (frequency == FinanceConstants.FREQ_WEEKLY) selectedDaysOfWeek.toList() else emptyList(),
                    daysOfMonth = if (frequency == FinanceConstants.FREQ_MONTHLY) selectedDaysOfMonth.toList() else emptyList(),
                    timeHour = hour,
                    timeMinute = minute,
                    startDateMillis = startDateMillis,
                    endDateMillis = endDateMillis,
                    lastExecutedTimestamp = existingConfig?.lastExecutedTimestamp ?: 0L,
                    isActive = true,
                    isForeign = transaction.isForeign,
                    currencyCode = transaction.currencyCode,
                    foreignAmount = transaction.foreignAmount,
                    exchangeRate = transaction.exchangeRate,
                    isRateCalculated = transaction.isRateCalculated,
                    equivalentAmount = transaction.equivalentAmount
                )

                HabayebRecurringManager.saveConfig(context, newConfig)
                Toast.makeText(context, context.getString(R.string.habayeb_recurring_toast_schedule_success), Toast.LENGTH_LONG).show()
                onDismiss()
            },
            modifier = Modifier
                .weight(1.5f)
                .height(40.dp),
            colors = ButtonDefaults.buttonColors(containerColor = activeThemeColor),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (existingConfig != null) stringResource(id = R.string.habayeb_recurring_btn_update) else stringResource(id = R.string.habayeb_recurring_btn_activate),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
