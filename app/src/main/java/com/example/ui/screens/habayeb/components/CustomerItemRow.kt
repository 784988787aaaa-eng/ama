package com.example.ui.screens.habayeb.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.TransactionType
import com.example.ui.helper.AutoScaleText
import com.example.ui.helper.HabayebMathHelper
import com.example.ui.screens.habayeb.utils.HabayebDateFormatter
import com.example.ui.state.CustomerUiState
import com.example.ui.theme.financialCreditColor
import com.example.ui.theme.financialDebtColor
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Modern High-Density Customer Row (Ultra-Spacious Name & Clean Non-Breaking Badges)
 * - Right: Quick Add Action Button (+) 36dp (transforms into Checkmark when selected)
 * - Center (Weight 1f):
 *     Line 1: Full Customer Name (bold, non-breaking) + Category Micro-Badge
 *     Line 2: Short Date/Time + Non-breaking Foreign Cash Badge [🌐 نقد أجنبي]
 * - Left:
 *     Line 1: Dynamic Balance Amount & Currency + Direction Arrow (▲ / ▼)
 *     Line 2: Status Text ("بقي له" / "بقي عليه" / "مصفّى")
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomerItemRow(
    customer: CustomerUiState,
    isSelected: Boolean,
    activeThemeColor: Color,
    activeSubColor: Color,
    isPinned: Boolean = false,
    isHighlighted: Boolean = false,
    haptic: HapticFeedback,
    onCustomerClick: (CustomerUiState) -> Unit,
    onCustomerLongClick: (String) -> Unit,
    onQuickAdd: (CustomerUiState) -> Unit,
    currentActiveCategory: String? = null,
    onRemoveFromCategory: (() -> Unit)? = null
) {
    val lastTxTime = customer.lastTransactionTimestamp
    val textSecondaryColor = MaterialTheme.colorScheme.onSurfaceVariant
    val formattedDate = remember(lastTxTime) {
        HabayebDateFormatter.formatFullDateTime(lastTxTime)
    }
    val nonZeroForeign = remember(customer.foreignDebts) {
        customer.foreignDebts.filter { entry ->
            entry.value.setScale(4, RoundingMode.HALF_EVEN)
                .compareTo(BigDecimal.ZERO) != 0
        }
    }
    val hasNonZeroForeign = nonZeroForeign.isNotEmpty()

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val containerColor = when {
        isSelected -> if (isDark) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
        isHighlighted -> if (isDark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surface
    }

    val cardBorder = when {
        isSelected -> BorderStroke(1.5.dp, activeThemeColor)
        isHighlighted -> BorderStroke(1.5.dp, activeThemeColor)
        else -> BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    }

    val onCardClick = remember(customer, onCustomerClick) {
        { onCustomerClick(customer) }
    }
    val onCardLongClick = remember(customer.id, haptic, onCustomerLongClick) {
        {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onCustomerLongClick(customer.id)
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        border = cardBorder,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHighlighted || isSelected) 3.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = onCardLongClick
            )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Right: Quick Add Action Button (+) / Selection Check (40dp circle)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) activeThemeColor
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        )
                        .clickable { onQuickAdd(customer) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(21.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(id = R.string.habayeb_add_tx_button_clean),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 2. Middle (Weight 1f): Prominent Full Name (15.5sp SemiBold), Date (11sp) & Sleek Foreign Badge
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Line 1: Prominent Full Customer Name
                    Text(
                        text = customer.name,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Line 2: Date/Time (11sp) + Sleek Foreign Cash Micro-Tag
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = formattedDate,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = textSecondaryColor,
                            maxLines = 1,
                            softWrap = false
                        )

                        if (hasNonZeroForeign) {
                            val badgeBg = if (isDark) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            }
                            val badgeTextColor = if (isDark) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                            val badgeBorder = BorderStroke(
                                width = 0.5.dp,
                                color = badgeTextColor.copy(alpha = 0.3f)
                            )

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = badgeBg,
                                border = badgeBorder,
                                modifier = Modifier
                                    .height(18.dp)
                                    .wrapContentWidth()
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 5.dp)
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.currency_foreign_cash),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = badgeTextColor,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 3. Left: Balance Amount & Currency, Debt Status
                CustomerDebtSummarySection(
                    customer = customer,
                    textSecondaryColor = textSecondaryColor,
                    modifier = Modifier.wrapContentWidth()
                )
            }

            // Pinned indicator badge
            if (isPinned) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 3.dp, start = 6.dp)
                ) {
                    Text(
                        text = "📌",
                        fontSize = 9.sp,
                        modifier = Modifier.graphicsLayer {
                            rotationZ = -15f
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerDebtSummarySection(
    customer: CustomerUiState,
    textSecondaryColor: Color,
    modifier: Modifier = Modifier
) {
    val netDebtVal = customer.displayNetDebt
    val netDebtDecimal = remember(netDebtVal) {
        HabayebMathHelper.toBigDecimal(netDebtVal)
    }
    val isZero = remember(netDebtDecimal) { netDebtDecimal.compareTo(BigDecimal.ZERO) == 0 }
    val isPositive = remember(netDebtDecimal) { netDebtDecimal.compareTo(BigDecimal.ZERO) > 0 }
    val isNegative = remember(netDebtDecimal) { netDebtDecimal.compareTo(BigDecimal.ZERO) < 0 }
    val itemCurrencySymbol = customer.displayCurrencySymbol
    val initialType = customer.originalCustomer.initialType

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val activeRed = financialDebtColor(isDark)
    val activeGreen = financialCreditColor(isDark)

    val (debtColor, isOwedByThem) = remember(initialType, isPositive, isNegative, isZero, activeRed, activeGreen, textSecondaryColor) {
        when (initialType) {
            TransactionType.OWED_BY_THEM.value -> {
                if (isPositive) Pair(activeRed, true)
                else if (isNegative) Pair(activeGreen, false)
                else Pair(textSecondaryColor, null as Boolean?)
            }
            TransactionType.OWED_TO_THEM.value -> {
                if (isNegative) Pair(activeGreen, false)
                else if (isPositive) Pair(activeRed, true)
                else Pair(textSecondaryColor, null as Boolean?)
            }
            else -> Pair(textSecondaryColor, null as Boolean?)
        }
    }

    val resolvedStatusText = when (initialType) {
        TransactionType.OWED_BY_THEM.value -> {
            if (isPositive) stringResource(id = R.string.status_remaining_on_him)
            else if (isNegative) stringResource(id = R.string.status_remaining_for_him)
            else stringResource(id = R.string.habayeb_balanced)
        }
        TransactionType.OWED_TO_THEM.value -> {
            if (isNegative) stringResource(id = R.string.status_remaining_for_him)
            else if (isPositive) stringResource(id = R.string.status_remaining_with_him)
            else stringResource(id = R.string.habayeb_balanced)
        }
        else -> stringResource(id = R.string.habayeb_balanced)
    }

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        if (!isZero) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (isOwedByThem != null) {
                    Text(
                        text = if (isOwedByThem) "▼" else "▲",
                        color = debtColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 1.dp)
                    )
                }
                AutoScaleText(
                    text = "${HabayebMathHelper.formatSmart(netDebtDecimal.abs())} $itemCurrencySymbol",
                    baseFontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = debtColor
                )
            }
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = resolvedStatusText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = textSecondaryColor.copy(alpha = 0.85f),
                maxLines = 1,
                softWrap = false
            )
        } else {
            Text(
                text = stringResource(id = R.string.habayeb_status_balanced_short),
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold,
                color = textSecondaryColor,
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = stringResource(id = R.string.habayeb_status_balanced),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = textSecondaryColor.copy(alpha = 0.85f),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
