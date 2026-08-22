package com.example.ui.screens.ledger.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.data.local.entities.FixedCommitment
import com.example.ui.helper.AutoScaleText
import com.example.ui.screens.habayeb.components.TinyFloatingSearchToggle
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LightRedTint
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.Slate200
import com.example.ui.theme.SoftLavender
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Unified High-Density Pinned Header for Smart Ledger (الدفتر الذكي)
 * - Single curved purple banner with:
 *     Top Row: Menu (Right) | Centered "المبلغ المتاح" + Large Bold Amount + Eye (Center) | Search & Toggle (Left)
 *     Conditional Row: Debt Inclusion Switch + Coverage Ratio Progress Bar (Only when commitments exist)
 * - Below Curved Banner: Independent Dual Metric Cards («الصافي» & «باقي الالتزامات»)
 * - 0% wasted space, 50%+ vertical height saved when no commitments are active!
 */
@Composable
fun PinnedMainLedgerHeader(
    isDaySelectionMode: Boolean,
    selectedDayKeys: List<String>,
    onCancelDaySelection: () -> Unit,
    onSelectAllDays: () -> Unit,
    onDeleteSelectedDays: () -> Unit,
    isSelectAllChecked: Boolean,
    selectedDayKeysCountText: String,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    isFloatingSearchActive: Boolean,
    onFloatingSearchActiveChanged: (Boolean) -> Unit,
    totalCash: BigDecimal,
    isPrivacyMode: Boolean,
    onTogglePrivacyMode: () -> Unit,
    currencySymbol: String,
    formatCurrency: (BigDecimal, String) -> String,
    commitments: List<FixedCommitment>,
    computedCommitments: List<Triple<FixedCommitment, BigDecimal, BigDecimal>>,
    linkHabayebDebts: Boolean,
    onLinkHabayebDebtsChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(10f)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Purple Curved Header Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 3.dp, shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp), clip = false)
                .clip(RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp))
                .background(EmeraldPrimary)
                .statusBarsPadding()
                .padding(bottom = if (commitments.isNotEmpty()) 8.dp else 6.dp)
        ) {
            if (isDaySelectionMode) {
                // Selection Mode Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .align(Alignment.Center),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                onCancelDaySelection()
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(id = R.string.common_cancel),
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        TextButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSelectAllDays()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = if (isSelectAllChecked) stringResource(id = R.string.ledger_cancel_all) else stringResource(id = R.string.ledger_select_all),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Text(
                        text = selectedDayKeysCountText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )

                    IconButton(
                        onClick = {
                            if (selectedDayKeys.isNotEmpty()) {
                                onDeleteSelectedDays()
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(id = R.string.ledger_bulk_delete_days_desc),
                            tint = if (selectedDayKeys.isEmpty()) Color.White.copy(alpha = 0.4f) else LightRedTint,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            } else {
                // Standard Smart Ledger Header Content
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Row: Menu (Right), Available Cash & Amount (Center), Search (Left)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Right: Navigation Menu Button
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

                        // Center: "المبلغ المتاح" (Line 1) + Amount & Visibility Eye (Line 2)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.ledger_actual_cash),
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
                                    onClick = onTogglePrivacyMode,
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

                                val formattedAmountState = remember(totalCash, currencySymbol, isPrivacyMode) {
                                    derivedStateOf {
                                        if (isPrivacyMode) "*****" else formatCurrency(totalCash, currencySymbol)
                                    }
                                }
                                val formattedAmount = formattedAmountState.value
                                AutoScaleText(
                                    text = formattedAmount,
                                    baseFontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.graphicsLayer {
                                        alpha = if (isPrivacyMode) 0.85f else 1.0f
                                    }
                                )
                            }
                        }

                        // Left: Tiny Floating Bubble Toggle + Search Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TinyFloatingSearchToggle(
                                isFloatingActive = isFloatingSearchActive,
                                activeThemeColor = Color.White,
                                onToggleClick = { onFloatingSearchActiveChanged(!isFloatingSearchActive) }
                            )

                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSearchClick()
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

                    // Conditional: Debt Inclusion Switch & Coverage Ratio Progress Bar
                    // Only rendered when commitments are present
                    if (commitments.isNotEmpty()) {
                        val percentFloatState = remember(commitments, computedCommitments) {
                            derivedStateOf {
                                val totalTarget = commitments.fold(BigDecimal.ZERO) { acc, fc -> acc.add(fc.targetAmount) }
                                val totalAllocated = computedCommitments.fold(BigDecimal.ZERO) { acc, triple -> acc.add(triple.second) }
                                if (totalTarget.compareTo(BigDecimal.ZERO) > 0) {
                                    totalAllocated.divide(totalTarget, 4, RoundingMode.HALF_EVEN).toFloat().coerceIn(0f, 1f)
                                } else {
                                    0f
                                }
                            }
                        }
                        val percentFloat = percentFloatState.value

                        Spacer(modifier = Modifier.height(4.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Debt Inclusion Toggle (Switch)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.ledger_link_debts),
                                        color = Color.White.copy(alpha = 0.95f),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Switch(
                                        checked = linkHabayebDebts,
                                        onCheckedChange = onLinkHabayebDebtsChange,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = SoftLavender,
                                            checkedTrackColor = PurpleAccent,
                                            uncheckedThumbColor = Slate200,
                                            uncheckedTrackColor = Color.White.copy(alpha = 0.25f)
                                        ),
                                        modifier = Modifier
                                            .height(18.dp)
                                            .scale(0.7f)
                                    )
                                }

                                // Commitments Coverage Ratio Percentage Badge
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.ledger_commitments_ratio),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.95f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(NeonGreen.copy(alpha = 0.22f))
                                            .border(1.dp, NeonGreen.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "${(percentFloat * 100).toInt()}%",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = NeonGreen
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            val neonGradient = remember {
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        NeonGreen,
                                        NeonCyan
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(2.5.dp))
                                        .background(Color.White.copy(alpha = 0.2f))
                                )
                                if (percentFloat > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(percentFloat)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(2.5.dp))
                                            .background(
                                                if (linkHabayebDebts) androidx.compose.ui.graphics.SolidColor(SoftLavender) else neonGradient
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Dual Metric Cards («الصافي» & «باقي الالتزامات»)
        // Conditional: Only rendered when commitments exist
        if (!isDaySelectionMode && commitments.isNotEmpty()) {
            CommitmentsSummaryCards(
                commitments = commitments,
                computedCommitments = computedCommitments,
                totalCash = totalCash,
                currencySymbol = currencySymbol,
                formatCurrency = formatCurrency,
                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
            )
        }
    }
}
