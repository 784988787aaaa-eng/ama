package com.example.ui.screens.habayeb.components.datetime

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class RangeTab { START, END }

/**
 * Intelligent Date Range and Time Picker Dialog Engine.
 * Used for Scheduling/Recurring transactions (From, To, Execution Time)
 * and History Filtering (From, To).
 */
@Composable
fun CustomDateRangePickerContent(
    initialStartMillis: Long,
    initialEndMillis: Long,
    initialHour: Int? = null,
    initialMinute: Int? = null,
    includeTime: Boolean = true,
    initialSelectedTab: RangeTab = RangeTab.START,
    title: String? = null,
    onDismiss: () -> Unit,
    onRangeSelected: (startMillis: Long, endMillis: Long, hour: Int, minute: Int) -> Unit
) {
    var activeTab by remember { mutableStateOf(initialSelectedTab) }

    var startCalendar by remember {
        mutableStateOf(Calendar.getInstance().apply {
            timeInMillis = initialStartMillis
            if (initialHour != null && initialMinute != null) {
                set(Calendar.HOUR_OF_DAY, initialHour)
                set(Calendar.MINUTE, initialMinute)
            }
        })
    }

    var endCalendar by remember {
        mutableStateOf(Calendar.getInstance().apply {
            timeInMillis = initialEndMillis
            if (initialHour != null && initialMinute != null) {
                set(Calendar.HOUR_OF_DAY, initialHour)
                set(Calendar.MINUTE, initialMinute)
            }
        })
    }

    // Common Time Calendar for Execution Time if includeTime is true
    var timeCalendar by remember {
        mutableStateOf(Calendar.getInstance().apply {
            val h = initialHour ?: startCalendar.get(Calendar.HOUR_OF_DAY)
            val m = initialMinute ?: startCalendar.get(Calendar.MINUTE)
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
        })
    }

    val dateFormatter = remember { SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .padding(horizontal = 4.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(vertical = 14.dp, horizontal = 14.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Title
                    Text(
                        text = title ?: if (includeTime) {
                            stringResource(id = R.string.datetime_picker_schedule_title)
                        } else {
                            stringResource(id = R.string.datetime_picker_filter_title)
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    // Range Switcher Tabs (من / إلى)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Start Date Tab (من)
                        val isStart = activeTab == RangeTab.START
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isStart) MaterialTheme.colorScheme.primary else Color.Transparent
                                )
                                .clickable { activeTab = RangeTab.START },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(id = R.string.datetime_picker_range_tab_start),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isStart) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = dateFormatter.format(startCalendar.time),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = if (isStart) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f) else MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // End Date Tab (إلى)
                        val isEnd = activeTab == RangeTab.END
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isEnd) MaterialTheme.colorScheme.primary else Color.Transparent
                                )
                                .clickable { activeTab = RangeTab.END },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(id = R.string.datetime_picker_range_tab_end),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isEnd) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = dateFormatter.format(endCalendar.time),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = if (isEnd) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f) else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Active Tab Content
                    val activeCalendar = if (activeTab == RangeTab.START) startCalendar else endCalendar
                    DateAndTimeSection(
                        calendar = activeCalendar,
                        onCalendarChange = { updated ->
                            if (activeTab == RangeTab.START) {
                                startCalendar = updated
                                // Ensure end date is not before start date
                                if (endCalendar.timeInMillis < updated.timeInMillis) {
                                    endCalendar = (updated.clone() as Calendar).apply {
                                        add(Calendar.DAY_OF_MONTH, 30)
                                    }
                                }
                            } else {
                                endCalendar = updated
                            }
                        },
                        showTime = false
                    )

                    // Optional Quick Adjust & Execution Time section for Scheduling
                    if (includeTime) {
                        Spacer(modifier = Modifier.height(8.dp))

                        // Time Picker Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = stringResource(id = R.string.datetime_picker_time_section),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                TimeDialPickersRow(
                                    timeCalendar = timeCalendar,
                                    onTimeCalendarChange = { timeCalendar = it }
                                )
                            }
                        }
                    }

                    // Quick Action Chips (اليوم / +30 يوم)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .clickable {
                                    if (activeTab == RangeTab.START) {
                                        startCalendar = Calendar.getInstance()
                                    } else {
                                        endCalendar = Calendar.getInstance()
                                    }
                                }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.datetime_picker_quick_today),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .clickable {
                                    endCalendar = (startCalendar.clone() as Calendar).apply {
                                        add(Calendar.DAY_OF_MONTH, 30)
                                    }
                                }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.datetime_picker_quick_plus_month),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dialog Actions (إلغاء وموافق)
                    DialogActionButtons(
                        onDismiss = onDismiss,
                        onConfirm = {
                            val h = timeCalendar.get(Calendar.HOUR_OF_DAY)
                            val m = timeCalendar.get(Calendar.MINUTE)
                            onRangeSelected(
                                startCalendar.timeInMillis,
                                endCalendar.timeInMillis,
                                h,
                                m
                            )
                        }
                    )
                }
            }
        }
    }
}
