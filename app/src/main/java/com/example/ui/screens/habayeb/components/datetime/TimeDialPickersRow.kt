package com.example.ui.screens.habayeb.components.datetime

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import java.util.Calendar

/**
 * Interactive row selector for time picking (AM/PM period switch, Minutes dial, Hours dial)
 * arranged in natural RTL layout with haptic feedback.
 */
@Composable
fun TimeDialPickersRow(
    timeCalendar: Calendar,
    onTimeCalendarChange: (Calendar) -> Unit
) {
    val hour12 = timeCalendar.get(Calendar.HOUR)
    val displayHour = if (hour12 == 0) 12 else hour12
    val minute = timeCalendar.get(Calendar.MINUTE)
    val amPm = timeCalendar.get(Calendar.AM_PM)
    val haptic = LocalHapticFeedback.current

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. الفترة (AM / PM Switch)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 2.dp)
        ) {
            Text(
                text = stringResource(id = R.string.datetime_picker_period),
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Spacer(modifier = Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        RoundedCornerShape(6.dp)
                    )
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val newCal = (timeCalendar.clone() as Calendar).apply {
                            val currentAmPm = get(Calendar.AM_PM)
                            set(Calendar.AM_PM, if (currentAmPm == Calendar.AM) Calendar.PM else Calendar.AM)
                        }
                        onTimeCalendarChange(newCal)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (amPm == Calendar.AM) stringResource(id = R.string.datetime_picker_am) else stringResource(id = R.string.datetime_picker_pm),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // 2. الدقيقة (Minute)
        RollingDialPicker(
            label = stringResource(id = R.string.datetime_picker_minute),
            value = minute,
            range = 0..59,
            onValueChange = { newMin ->
                val newCal = (timeCalendar.clone() as Calendar).apply {
                    set(Calendar.MINUTE, newMin)
                }
                onTimeCalendarChange(newCal)
            },
            format = "%02d"
        )

        // 3. الساعة (Hour)
        RollingDialPicker(
            label = stringResource(id = R.string.datetime_picker_hour),
            value = displayHour,
            range = 1..12,
            onValueChange = { newHour ->
                val newCal = (timeCalendar.clone() as Calendar).apply {
                    val currentAmPm = get(Calendar.AM_PM)
                    val hour24 = if (currentAmPm == Calendar.PM) {
                        if (newHour == 12) 12 else newHour + 12
                    } else {
                        if (newHour == 12) 0 else newHour
                    }
                    set(Calendar.HOUR_OF_DAY, hour24)
                }
                onTimeCalendarChange(newCal)
            }
        )
    }
}
