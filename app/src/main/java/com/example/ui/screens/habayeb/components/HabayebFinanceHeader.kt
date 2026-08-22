package com.example.ui.screens.habayeb.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.helper.AutoScaleText
import com.example.ui.helper.HabayebMathHelper
import com.example.ui.screens.habayeb.components.header.HabayebDualMetricCards
import com.example.ui.screens.habayeb.components.header.HabayebHeaderSearchBar
import com.example.ui.theme.financialCreditColor
import com.example.ui.theme.financialDebtColor
import java.math.BigDecimal

private const val PRIVACY_MASK = "*****"

@Composable
fun HabayebFinanceHeader(
    isSearchActive: Boolean,
    onSearchActiveChanged: (Boolean) -> Unit,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onMenuClick: () -> Unit,
    haptic: HapticFeedback,
    totalOwedByThem: BigDecimal,
    totalOwedToThem: BigDecimal,
    selectedFilterTab: Int,
    onFilterTabSelected: (Int) -> Unit,
    isPrivacyMode: Boolean,
    onTogglePrivacy: () -> Unit,
    currencySymbol: String,
    onHeaderDoubleClick: () -> Unit = {},
    isFloatingActive: Boolean = false,
    onToggleFloatingClick: () -> Unit = {},
    activeThemeColor: Color,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val greenColor = financialCreditColor(isDark)
    val redColor = financialDebtColor(isDark)

    val netDebt = remember(totalOwedByThem, totalOwedToThem) {
        totalOwedByThem.subtract(totalOwedToThem)
    }

    val dynamicTitle = when {
        netDebt.compareTo(BigDecimal.ZERO) > 0 -> stringResource(id = R.string.habayeb_net_total_for_you)
        netDebt.compareTo(BigDecimal.ZERO) < 0 -> stringResource(id = R.string.habayeb_net_total_on_you)
        else -> stringResource(id = R.string.habayeb_net_total_label)
    }

    val formattedNetDebt = remember(netDebt, isPrivacyMode, currencySymbol) {
        if (isPrivacyMode) PRIVACY_MASK
        else {
            val sign = if (netDebt.compareTo(BigDecimal.ZERO) < 0) "-" else ""
            val absVal = HabayebMathHelper.formatSmart(netDebt.abs())
            "$sign$absVal $currencySymbol"
        }
    }

    val formattedOwedByThem = remember(totalOwedByThem, isPrivacyMode, currencySymbol) {
        if (isPrivacyMode) PRIVACY_MASK
        else "${HabayebMathHelper.formatSmart(totalOwedByThem)} $currencySymbol"
    }

    val formattedOwedToThem = remember(totalOwedToThem, isPrivacyMode, currencySymbol) {
        if (isPrivacyMode) PRIVACY_MASK
        else "${HabayebMathHelper.formatSmart(totalOwedToThem)} $currencySymbol"
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // 1. Curved Header Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                .background(activeThemeColor)
                .statusBarsPadding()
                .padding(bottom = 6.dp)
        ) {
            if (isSearchActive) {
                HabayebHeaderSearchBar(
                    searchQuery = searchQuery,
                    onSearchQueryChanged = onSearchQueryChanged,
                    onCloseSearch = {
                        onSearchQueryChanged("")
                        onSearchActiveChanged(false)
                    },
                    haptic = haptic
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Right Action: Hamburger Menu Button
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onMenuClick()
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.White.copy(alpha = 0.16f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource(id = R.string.ledger_nav_menu_desc),
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Center Section: Dynamic Title + Net Amount with Eye Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onHeaderDoubleClick()
                                    },
                                    onLongPress = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onHeaderDoubleClick()
                                    }
                                )
                            }
                    ) {
                        Text(
                            text = dynamicTitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(
                                onClick = onTogglePrivacy,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPrivacyMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = stringResource(id = R.string.ledger_visibility_desc),
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            AnimatedContent(
                                targetState = formattedNetDebt,
                                transitionSpec = {
                                    if (targetState == PRIVACY_MASK || initialState == PRIVACY_MASK) {
                                        fadeIn(animationSpec = tween(90)).togetherWith(fadeOut(animationSpec = tween(60)))
                                    } else {
                                        (fadeIn(animationSpec = tween(150)) + slideInVertically(animationSpec = tween(150)) { height -> height / 3 })
                                            .togetherWith(fadeOut(animationSpec = tween(100)) + slideOutVertically(animationSpec = tween(100)) { height -> -height / 3 })
                                    }
                                },
                                label = "net_debt_anim"
                            ) { animatedBalance ->
                                AutoScaleText(
                                    text = animatedBalance,
                                    baseFontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Left Actions: Floating Search Toggle & Search Icon Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TinyFloatingSearchToggle(
                            isFloatingActive = isFloatingActive,
                            activeThemeColor = Color.White,
                            onToggleClick = onToggleFloatingClick
                        )

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSearchActiveChanged(true)
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.16f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(id = R.string.habayeb_search_label),
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Independent Solid High-Contrast Dual Metric Cards ("لنا" & "علينا")
        HabayebDualMetricCards(
            selectedFilterTab = selectedFilterTab,
            onFilterTabSelected = onFilterTabSelected,
            formattedOwedByThem = formattedOwedByThem,
            formattedOwedToThem = formattedOwedToThem,
            isDark = isDark,
            greenColor = greenColor,
            redColor = redColor,
            haptic = haptic
        )
    }
}
