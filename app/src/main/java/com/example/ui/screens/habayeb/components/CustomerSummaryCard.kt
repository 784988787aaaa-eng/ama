package com.example.ui.screens.habayeb.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import java.math.BigDecimal
import java.math.RoundingMode
import com.example.ui.theme.ChipGreenHeaderDark
import com.example.ui.theme.ChipGreenHeaderLight
import com.example.ui.theme.ChipRedHeaderDark
import com.example.ui.theme.ChipRedHeaderLight
import com.example.ui.theme.financialCreditColor
import com.example.ui.theme.financialDebtColor

import com.example.domain.model.TransactionType

@Composable
fun AutoSizeText(
    text: String,
    fontSize: TextUnit,
    fontWeight: FontWeight? = null,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    modifier: Modifier = Modifier,
    maxLines: Int = 1
) {
    var fontSizeState by remember(text, fontSize) { mutableStateOf(fontSize) }
    var readyToDraw by remember(text, fontSize) { mutableStateOf(false) }

    Text(
        text = text,
        style = TextStyle(fontSize = fontSizeState, fontWeight = fontWeight, color = color),
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = TextOverflow.Clip,
        softWrap = false,
        modifier = modifier.drawWithContent {
            if (readyToDraw) {
                drawContent()
            }
        },
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.hasVisualOverflow) {
                val currentSize = fontSizeState.value
                if (currentSize > 8f) {
                    fontSizeState = (currentSize - 0.5f).sp
                } else {
                    readyToDraw = true
                }
            } else {
                readyToDraw = true
            }
        }
    )
}

@Composable
fun BalanceCompactChip(
    amount: BigDecimal,
    currencyCode: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    initialType: String = TransactionType.OWED_BY_THEM.value,
    modifier: Modifier = Modifier
) {
    val clearedStr = stringResource(id = R.string.status_account_cleared)
    val remainingOnHimStr = stringResource(id = R.string.status_remaining_on_him)
    val remainingForHimStr = stringResource(id = R.string.status_remaining_for_him)
    val remainingWithHimStr = stringResource(id = R.string.status_remaining_with_him)

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val surfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline

    val chipState = remember(amount, currencyCode, initialType, isDark, clearedStr, remainingOnHimStr, remainingForHimStr, remainingWithHimStr, surfaceVariantColor, outlineColor) {
        val cmp = amount.setScale(4, RoundingMode.HALF_EVEN).compareTo(BigDecimal.ZERO)
        val isZero = cmp == 0
        val isPositive = cmp > 0
        val isNegative = cmp < 0

        val redColor = financialDebtColor(isDark)
        val redHeaderColor = if (isDark) ChipRedHeaderDark else ChipRedHeaderLight
        val greenColor = financialCreditColor(isDark)
        val greenHeaderColor = if (isDark) ChipGreenHeaderDark else ChipGreenHeaderLight

        val (chipColor, headerTextColor, stateLabel) = when {
            isZero -> Triple(outlineColor, outlineColor, clearedStr)
            initialType == TransactionType.OWED_TO_THEM.value -> {
                if (isNegative) {
                    Triple(greenColor, greenHeaderColor, remainingForHimStr)
                } else if (isPositive) {
                    Triple(redColor, redHeaderColor, remainingWithHimStr)
                } else {
                    Triple(outlineColor, outlineColor, clearedStr)
                }
            }
            else -> {
                if (isPositive) {
                    Triple(redColor, redHeaderColor, remainingOnHimStr)
                } else if (isNegative) {
                    Triple(greenColor, greenHeaderColor, remainingForHimStr)
                } else {
                    Triple(outlineColor, outlineColor, clearedStr)
                }
            }
        }

        val bdAmount = amount.abs()
        val formattedAmountStr = "${com.example.ui.helper.HabayebMathHelper.formatSmart(bdAmount)} $currencyCode"
        Triple(chipColor, headerTextColor, Pair(stateLabel, formattedAmountStr))
    }

    val targetChipColor = chipState.first
    val targetHeaderTextColor = chipState.second
    val stateLabel = chipState.third.first
    val formattedAmountStr = chipState.third.second

    val targetBgColor = if (isSelected) targetChipColor.copy(alpha = 0.16f) else if (isDark) targetChipColor.copy(alpha = 0.10f) else targetChipColor.copy(alpha = 0.08f)
    val targetBorderColor = if (isSelected) targetChipColor else targetChipColor.copy(alpha = 0.45f)
    val borderWidth = if (isSelected) 1.5.dp else 1.dp

    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(targetBgColor)
            .border(borderWidth, targetBorderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onSelect)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stateLabel,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = targetHeaderTextColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(2.dp))
        AutoSizeText(
            text = formattedAmountStr,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Black,
            color = targetChipColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun BalanceCompactChip(
    amount: Double,
    currencyCode: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    initialType: String = TransactionType.OWED_BY_THEM.value,
    modifier: Modifier = Modifier
) {
    BalanceCompactChip(
        amount = BigDecimal.valueOf(amount),
        currencyCode = currencyCode,
        isSelected = isSelected,
        onSelect = onSelect,
        initialType = initialType,
        modifier = modifier
    )
}

@Composable
fun CustomerSummaryCard(
    currencySymbol: String,
    netDebtMap: Map<String, Double> = emptyMap(),
    netDebtBDMap: Map<String, BigDecimal> = emptyMap(),
    initialType: String = TransactionType.OWED_BY_THEM.value,
    selectedCurrencyFilter: String? = null,
    onCurrencyFilterSelected: (String?) -> Unit = {}
) {
    val effectiveBDMap = remember(netDebtMap, netDebtBDMap) {
        if (netDebtBDMap.isNotEmpty()) {
            netDebtBDMap
        } else {
            netDebtMap.mapValues { BigDecimal.valueOf(it.value) }
        }
    }

    val allCurrencies = remember(effectiveBDMap, currencySymbol) {
        val foreignCurrencies = effectiveBDMap.keys.filter { it != currencySymbol }.sorted()
        listOf(currencySymbol) + foreignCurrencies
    }

    val isCompact = allCurrencies.size <= 3

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val rowModifier = if (isCompact) {
            Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp)
        } else {
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 1.dp)
        }

        Row(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (curr in allCurrencies) {
                val netDebtVal = effectiveBDMap[curr] ?: BigDecimal.ZERO
                val chipModifier = if (isCompact) Modifier.weight(1f) else Modifier.widthIn(min = 100.dp)
                BalanceCompactChip(
                    amount = netDebtVal,
                    currencyCode = curr,
                    isSelected = selectedCurrencyFilter == curr,
                    onSelect = {
                        if (selectedCurrencyFilter == curr) {
                            onCurrencyFilterSelected(null)
                        } else {
                            onCurrencyFilterSelected(curr)
                        }
                    },
                    initialType = initialType,
                    modifier = chipModifier
                )
            }
        }
    }
}
