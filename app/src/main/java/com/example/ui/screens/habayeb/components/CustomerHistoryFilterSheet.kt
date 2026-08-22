package com.example.ui.screens.habayeb.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHistoryFilterSheet(
    dateFilterMode: Int,
    onDateFilterModeChange: (Int) -> Unit,
    customStartDate: Long?,
    onCustomStartDateChange: (Long?) -> Unit,
    customEndDate: Long?,
    onCustomEndDateChange: (Long?) -> Unit,
    typeFilterMode: Int,
    onTypeFilterModeChange: (Int) -> Unit,
    activeThemeColor: Color,
    onDismissRequest: () -> Unit
) {
    var showRangePicker by remember { mutableStateOf(false) }
    var selectedRangeTab by remember { mutableStateOf(RangeTab.START) }

    val strAllTime = stringResource(id = R.string.habayeb_filter_all_time)
    val strToday = stringResource(id = R.string.habayeb_filter_today)
    val strMonth = stringResource(id = R.string.habayeb_filter_month)
    val strCustom = stringResource(id = R.string.habayeb_filter_custom)

    val dateModes = remember(strAllTime, strToday, strMonth, strCustom) {
        listOf(
            0 to strAllTime,
            1 to strToday,
            2 to strMonth,
            3 to strCustom
        )
    }

    val strAll = stringResource(id = R.string.habayeb_filter_all)
    val strDebts = stringResource(id = R.string.habayeb_filter_type_debts)
    val strPayments = stringResource(id = R.string.habayeb_filter_type_payments)

    val typeModes = remember(strAll, strDebts, strPayments) {
        listOf(
            0 to strAll,
            1 to strDebts,
            2 to strPayments
        )
    }

    val (startStr, endStr) = remember(customStartDate, customEndDate) {
        val formatter = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val start = customStartDate?.let { formatter.format(Date(it)) } ?: "..."
        val end = customEndDate?.let { formatter.format(Date(it)) } ?: "..."
        Pair(start, end)
    }

    if (showRangePicker) {
        val now = System.currentTimeMillis()
        val initStart = customStartDate ?: now
        val initEnd = customEndDate ?: (initStart + 30L * 24 * 60 * 60 * 1000)
        CustomDateRangePickerDialog(
            initialStartMillis = initStart,
            initialEndMillis = initEnd,
            includeTime = false,
            initialSelectedTab = selectedRangeTab,
            onDismiss = { showRangePicker = false },
            onRangeSelected = { start, end, _, _ ->
                onCustomStartDateChange(start)
                onCustomEndDateChange(end)
                onDateFilterModeChange(3)
                showRangePicker = false
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.habayeb_smart_filter),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Date Filter Segment
            Text(
                text = stringResource(id = R.string.habayeb_filter_date),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dateModes.forEach { (mode, label) ->
                    val isSelected = dateFilterMode == mode
                    val chipBg = if (isSelected) activeThemeColor else MaterialTheme.colorScheme.outlineVariant
                    val chipText = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(chipBg)
                            .clickable {
                                onDateFilterModeChange(mode)
                                if (mode == 3 && customStartDate == null) {
                                    selectedRangeTab = RangeTab.START
                                    showRangePicker = true
                                }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = chipText)
                    }
                }
            }

            if (dateFilterMode == 3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.outlineVariant)
                            .clickable {
                                selectedRangeTab = RangeTab.START
                                showRangePicker = true
                            }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Event, contentDescription = null, tint = activeThemeColor, modifier = Modifier.size(14.dp))
                            Text(startStr, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Text(stringResource(id = R.string.habayeb_to_text), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.outlineVariant)
                            .clickable {
                                selectedRangeTab = RangeTab.END
                                showRangePicker = true
                            }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Event, contentDescription = null, tint = activeThemeColor, modifier = Modifier.size(14.dp))
                            Text(endStr, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Type Filter Segment
            Text(
                text = stringResource(id = R.string.habayeb_filter_by_type),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                typeModes.forEach { (mode, label) ->
                    val isSelected = typeFilterMode == mode
                    val chipBg = if (isSelected) activeThemeColor else MaterialTheme.colorScheme.outlineVariant
                    val chipText = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(chipBg)
                            .clickable { onTypeFilterModeChange(mode) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = chipText)
                    }
                }
            }
        }
    }
}
