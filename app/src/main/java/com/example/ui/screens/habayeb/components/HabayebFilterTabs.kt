package com.example.ui.screens.habayeb.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.helper.HabayebMathHelper
import java.math.BigDecimal

import com.example.ui.theme.ChipGreenBgDarkSelected
import com.example.ui.theme.ChipGreenBgDarkUnselected
import com.example.ui.theme.ChipGreenBgLightSelected
import com.example.ui.theme.ChipGreenBgLightUnselected
import com.example.ui.theme.ChipGreenBorderDarkSelected
import com.example.ui.theme.ChipGreenBorderDarkUnselected
import com.example.ui.theme.ChipGreenBorderLightSelected
import com.example.ui.theme.ChipGreenBorderLightUnselected
import com.example.ui.theme.ChipGreenHeaderDark
import com.example.ui.theme.ChipGreenHeaderLight
import com.example.ui.theme.ChipGreenTextDark
import com.example.ui.theme.ChipGreenTextLight
import com.example.ui.theme.ChipRedBgDarkSelected
import com.example.ui.theme.ChipRedBgDarkUnselected
import com.example.ui.theme.ChipRedBgLightSelected
import com.example.ui.theme.ChipRedBgLightUnselected
import com.example.ui.theme.ChipRedBorderDarkSelected
import com.example.ui.theme.ChipRedBorderDarkUnselected
import com.example.ui.theme.ChipRedBorderLightSelected
import com.example.ui.theme.ChipRedBorderLightUnselected
import com.example.ui.theme.ChipRedHeaderDark
import com.example.ui.theme.ChipRedHeaderLight
import com.example.ui.theme.ChipRedTextDark
import com.example.ui.theme.ChipRedTextLight

private const val PRIVACY_MASK = "*****"

private object ChipColors {
    fun redBg(isDark: Boolean, isSelected: Boolean): Color {
        return if (isDark) {
            if (isSelected) ChipRedBgDarkSelected else ChipRedBgDarkUnselected
        } else {
            if (isSelected) ChipRedBgLightSelected else ChipRedBgLightUnselected
        }
    }

    fun redBorder(isDark: Boolean, isSelected: Boolean): Color {
        return if (isDark) {
            if (isSelected) ChipRedBorderDarkSelected else ChipRedBorderDarkUnselected
        } else {
            if (isSelected) ChipRedBorderLightSelected else ChipRedBorderLightUnselected
        }
    }

    fun redText(isDark: Boolean): Color = if (isDark) ChipRedTextDark else ChipRedTextLight
    fun redHeader(isDark: Boolean): Color = if (isDark) ChipRedHeaderDark else ChipRedHeaderLight

    fun greenBg(isDark: Boolean, isSelected: Boolean): Color {
        return if (isDark) {
            if (isSelected) ChipGreenBgDarkSelected else ChipGreenBgDarkUnselected
        } else {
            if (isSelected) ChipGreenBgLightSelected else ChipGreenBgLightUnselected
        }
    }

    fun greenBorder(isDark: Boolean, isSelected: Boolean): Color {
        return if (isDark) {
            if (isSelected) ChipGreenBorderDarkSelected else ChipGreenBorderDarkUnselected
        } else {
            if (isSelected) ChipGreenBorderLightSelected else ChipGreenBorderLightUnselected
        }
    }

    fun greenText(isDark: Boolean): Color = if (isDark) ChipGreenTextDark else ChipGreenTextLight
    fun greenHeader(isDark: Boolean): Color = if (isDark) ChipGreenHeaderDark else ChipGreenHeaderLight
}

@Composable
fun HabayebFilterTabs(
    selectedFilterTab: Int,
    onFilterTabSelected: (Int) -> Unit,
    totalOwedByThem: BigDecimal,
    totalOwedToThem: BigDecimal,
    currencySymbol: String,
    isPrivacyMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val formattedOwedByThem = remember(totalOwedByThem) {
        HabayebMathHelper.formatSmart(totalOwedByThem)
    }

    val formattedOwedToThem = remember(totalOwedToThem) {
        HabayebMathHelper.formatSmart(totalOwedToThem)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. كبسولة لي عند الناس (المدينين)
        val isOwedByThemSelected = selectedFilterTab == 1
        FilterTabChip(
            title = stringResource(id = R.string.habayeb_filter_owed_by),
            formattedAmount = formattedOwedByThem,
            currencySymbol = currencySymbol,
            isSelected = isOwedByThemSelected,
            isPrivacyMode = isPrivacyMode,
            headerColor = ChipColors.redHeader(isDark),
            textColor = ChipColors.redText(isDark),
            targetBgColor = ChipColors.redBg(isDark, isOwedByThemSelected),
            targetBorderColor = ChipColors.redBorder(isDark, isOwedByThemSelected),
            animLabel = "owedByTextAnim",
            onClick = { onFilterTabSelected(if (isOwedByThemSelected) 0 else 1) }
        )

        // 2. كبسولة علي للناس (الدائنين)
        val isOwedToThemSelected = selectedFilterTab == 2
        FilterTabChip(
            title = stringResource(id = R.string.habayeb_filter_owed_to),
            formattedAmount = formattedOwedToThem,
            currencySymbol = currencySymbol,
            isSelected = isOwedToThemSelected,
            isPrivacyMode = isPrivacyMode,
            headerColor = ChipColors.greenHeader(isDark),
            textColor = ChipColors.greenText(isDark),
            targetBgColor = ChipColors.greenBg(isDark, isOwedToThemSelected),
            targetBorderColor = ChipColors.greenBorder(isDark, isOwedToThemSelected),
            animLabel = "owedToTextAnim",
            onClick = { onFilterTabSelected(if (isOwedToThemSelected) 0 else 2) }
        )
    }
}

@Composable
private fun RowScope.FilterTabChip(
    title: String,
    formattedAmount: String,
    currencySymbol: String,
    isSelected: Boolean,
    isPrivacyMode: Boolean,
    headerColor: Color,
    textColor: Color,
    targetBgColor: Color,
    targetBorderColor: Color,
    animLabel: String,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .shadow(
                elevation = if (isSelected) 2.dp else 0.dp,
                shape = RoundedCornerShape(14.dp)
            )
            .clip(RoundedCornerShape(14.dp))
            .background(targetBgColor)
            .border(
                BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = targetBorderColor
                ),
                RoundedCornerShape(14.dp)
            )
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = headerColor,
                textAlign = TextAlign.Center
            )
            val displayValue = if (isPrivacyMode) PRIVACY_MASK else "$formattedAmount $currencySymbol"
            AnimatedContent(
                targetState = displayValue,
                transitionSpec = {
                    if (targetState == PRIVACY_MASK || initialState == PRIVACY_MASK) {
                        fadeIn(animationSpec = tween(90)).togetherWith(fadeOut(animationSpec = tween(60)))
                    } else {
                        (fadeIn(animationSpec = tween(150)) + slideInVertically(animationSpec = tween(150)) { height -> height / 3 })
                            .togetherWith(fadeOut(animationSpec = tween(100)) + slideOutVertically(animationSpec = tween(100)) { height -> -height / 3 })
                    }
                },
                label = animLabel
            ) { animatedVal ->
                Text(
                    text = animatedVal,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

