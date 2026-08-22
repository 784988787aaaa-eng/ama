package com.example.ui.screens.habayeb.components.datetime

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import java.util.Calendar

/**
 * Composite visual component displaying date dials and optional time dials.
 */
@Composable
fun DateAndTimeSection(
    calendar: Calendar,
    onCalendarChange: (Calendar) -> Unit,
    showTime: Boolean
) {
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH) + 1
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    fun updateCalendar(field: Int, newValue: Int) {
        val newCal = (calendar.clone() as Calendar).apply {
            if (field == Calendar.MONTH) {
                set(Calendar.MONTH, newValue - 1)
            } else if (field == Calendar.HOUR) {
                val currentAmPm = get(Calendar.AM_PM)
                val hour24 = if (currentAmPm == Calendar.PM) {
                    if (newValue == 12) 12 else newValue + 12
                } else {
                    if (newValue == 12) 0 else newValue
                }
                set(Calendar.HOUR_OF_DAY, hour24)
            } else {
                set(field, newValue)
            }
        }
        onCalendarChange(newCal)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Date Block
        Box(
            modifier = Modifier
                .weight(if (showTime) 1.2f else 1f)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                    RoundedCornerShape(14.dp)
                )
                .padding(vertical = 6.dp, horizontal = 4.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.datetime_picker_date),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Day
                    RollingDialPicker(
                        label = stringResource(id = R.string.datetime_picker_day),
                        value = day,
                        range = 1..maxDays,
                        onValueChange = { updateCalendar(Calendar.DAY_OF_MONTH, it) }
                    )

                    // Month
                    RollingDialPicker(
                        label = stringResource(id = R.string.datetime_picker_month),
                        value = month,
                        range = 1..12,
                        onValueChange = { updateCalendar(Calendar.MONTH, it) }
                    )

                    // Year
                    RollingDialPicker(
                        label = stringResource(id = R.string.datetime_picker_year),
                        value = year,
                        range = 1980..2100,
                        onValueChange = { updateCalendar(Calendar.YEAR, it) }
                    )
                }
            }
        }

        if (showTime) {
            // Divider
            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .height(64.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            )

            // Time Block
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(vertical = 6.dp, horizontal = 4.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.datetime_picker_time),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    TimeDialPickersRow(
                        timeCalendar = calendar,
                        onTimeCalendarChange = onCalendarChange
                    )
                }
            }
        }
    }
}
