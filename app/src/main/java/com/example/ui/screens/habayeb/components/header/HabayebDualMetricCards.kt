package com.example.ui.screens.habayeb.components.header

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.helper.AutoScaleText
import com.example.ui.theme.*

@Composable
fun HabayebDualMetricCards(
    selectedFilterTab: Int,
    onFilterTabSelected: (Int) -> Unit,
    formattedOwedByThem: String,
    formattedOwedToThem: String,
    isDark: Boolean,
    greenColor: Color,
    redColor: Color,
    haptic: HapticFeedback,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Right Card: "لنا" (Red Color - Solid Safe Background)
        val isOwedBySelected = selectedFilterTab == 1
        val owedByCardBg = if (isDark) {
            if (isOwedBySelected) ChipRedBgDarkSelected else DebtContainerDark
        } else {
            if (isOwedBySelected) ChipRedBgLightSelected else DebtContainerLight
        }
        val owedByBorderColor = if (isDark) {
            if (isOwedBySelected) redColor else DebtBorderDark
        } else {
            if (isOwedBySelected) redColor else DebtBorderLight
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = owedByCardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isOwedBySelected) 3.dp else 1.dp),
            border = BorderStroke(
                width = if (isOwedBySelected) 1.5.dp else 1.dp,
                color = owedByBorderColor
            ),
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = redColor)
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onFilterTabSelected(if (isOwedBySelected) 0 else 1)
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.habayeb_filter_owed_by),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = redColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    AutoScaleText(
                        text = formattedOwedByThem,
                        baseFontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = redColor,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Left Card: "علينا" (Emerald Green Color - Solid Safe Background)
        val isOwedToSelected = selectedFilterTab == 2
        val owedToCardBg = if (isDark) {
            if (isOwedToSelected) ChipGreenBgDarkSelected else CreditContainerDark
        } else {
            if (isOwedToSelected) ChipGreenBgLightSelected else CreditContainerLight
        }
        val owedToBorderColor = if (isDark) {
            if (isOwedToSelected) greenColor else CreditBorderDark
        } else {
            if (isOwedToSelected) greenColor else CreditBorderLight
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = owedToCardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isOwedToSelected) 3.dp else 1.dp),
            border = BorderStroke(
                width = if (isOwedToSelected) 1.5.dp else 1.dp,
                color = owedToBorderColor
            ),
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = greenColor)
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onFilterTabSelected(if (isOwedToSelected) 0 else 2)
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.habayeb_filter_owed_to),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = greenColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    AutoScaleText(
                        text = formattedOwedToThem,
                        baseFontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = greenColor,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
