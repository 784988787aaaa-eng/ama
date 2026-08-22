package com.example.ui.screens.habayeb.components

import androidx.compose.foundation.background
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

@Composable
fun HabayebSortDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    financialSortMode: Int,
    historicalSortMode: Int,
    onFinancialSortModeChanged: (Int) -> Unit,
    onHistoricalSortModeChanged: (Int) -> Unit,
    onScrollToTop: () -> Unit,
    haptic: HapticFeedback,
    neutralWhite: Color,
    textPrimary: Color,
    backgroundLight: Color,
    modifier: Modifier = Modifier
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.background(neutralWhite)
    ) {
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(id = R.string.filter_sort_default),
                    fontSize = 12.sp,
                    fontWeight = if (financialSortMode == 0 && historicalSortMode == 1) FontWeight.Bold else FontWeight.Normal,
                    color = textPrimary
                )
            },
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onFinancialSortModeChanged(0)
                onHistoricalSortModeChanged(1)
                onDismissRequest()
                onScrollToTop()
            }
        )
        HorizontalDivider(color = backgroundLight)
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(id = R.string.filter_sort_largest),
                    fontSize = 12.sp,
                    fontWeight = if (financialSortMode == 1) FontWeight.Bold else FontWeight.Normal,
                    color = textPrimary
                )
            },
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onHistoricalSortModeChanged(0)
                onFinancialSortModeChanged(1)
                onDismissRequest()
                onScrollToTop()
            }
        )
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(id = R.string.filter_sort_smallest),
                    fontSize = 12.sp,
                    fontWeight = if (financialSortMode == 2) FontWeight.Bold else FontWeight.Normal,
                    color = textPrimary
                )
            },
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onHistoricalSortModeChanged(0)
                onFinancialSortModeChanged(2)
                onDismissRequest()
                onScrollToTop()
            }
        )
        HorizontalDivider(color = backgroundLight)
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(id = R.string.filter_sort_oldest),
                    fontSize = 12.sp,
                    fontWeight = if (historicalSortMode == 2) FontWeight.Bold else FontWeight.Normal,
                    color = textPrimary
                )
            },
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onFinancialSortModeChanged(0)
                onHistoricalSortModeChanged(2)
                onDismissRequest()
                onScrollToTop()
            }
        )
    }
}
