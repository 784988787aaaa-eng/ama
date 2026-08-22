package com.example.ui.screens.habayeb.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entities.HabayebTransaction
import com.example.ui.screens.habayeb.utils.ExchangeRateHelper
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun TransactionCurrencySelector(
    selectedTransactionCurrency: String,
    currencySymbol: String,
    activeThemeColor: Color,
    dynamicThemeColor: Color = activeThemeColor,
    applyExchangeRate: Boolean,
    exchangeRatesJson: String,
    editingTransaction: HabayebTransaction?,
    haptic: HapticFeedback,
    onCurrencySelected: (String) -> Unit,
    onApplyExchangeRateChange: (Boolean) -> Unit,
    onSetupRateClick: (String) -> Unit
) {
    val isForeignSelected = selectedTransactionCurrency != currencySymbol

    val yerSym = stringResource(R.string.currency_yer)
    val yerLabel = stringResource(R.string.currency_label_yer)
    val usdSym = stringResource(R.string.currency_usd)
    val usdLabel = stringResource(R.string.currency_label_usd)
    val sarSym = stringResource(R.string.currency_sar)
    val sarLabel = stringResource(R.string.currency_label_sar)

    val famousCurrencies = remember(yerSym, yerLabel, usdSym, usdLabel, sarSym, sarLabel) {
        listOf(
            Pair(yerSym, yerLabel),
            Pair(usdSym, usdLabel),
            Pair(sarSym, sarLabel)
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            famousCurrencies.forEachIndexed { index, (sym, label) ->
                val isSelected = selectedTransactionCurrency == sym
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (selectedTransactionCurrency != sym) {
                                onCurrencySelected(sym)
                            }
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) dynamicThemeColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .border(2.dp, if (isSelected) dynamicThemeColor else MaterialTheme.colorScheme.outline, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(dynamicThemeColor)
                            )
                        }
                    }
                }
                if (index < famousCurrencies.size - 1) {
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }
        }

        if (isForeignSelected) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            val newApply = !applyExchangeRate
                            onApplyExchangeRateChange(newApply)
                            if (newApply) {
                                val hasStoredRate = ExchangeRateHelper.hasRate(exchangeRatesJson, currencySymbol, selectedTransactionCurrency)
                                val currentRateVal = ExchangeRateHelper.getRate(exchangeRatesJson, currencySymbol, selectedTransactionCurrency)
                                if (!hasStoredRate || currentRateVal == 1.0) {
                                    onSetupRateClick("")
                                }
                            }
                        }
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .border(1.dp, activeThemeColor, RoundedCornerShape(4.dp))
                            .background(if (applyExchangeRate) activeThemeColor else Color.Transparent, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (applyExchangeRate) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(id = R.string.add_transaction_exchange_rate_prompt),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (applyExchangeRate) {
                    val rateInfo = remember(editingTransaction, exchangeRatesJson, currencySymbol, selectedTransactionCurrency) {
                        val isEditingHistoricalRate = editingTransaction != null && editingTransaction.currencyCode == selectedTransactionCurrency && editingTransaction.exchangeRate.compareTo(BigDecimal.ZERO) > 0
                        val hasStoredRate = ExchangeRateHelper.hasRate(exchangeRatesJson, currencySymbol, selectedTransactionCurrency) || isEditingHistoricalRate
                        val currentRateRaw = if (isEditingHistoricalRate) editingTransaction.exchangeRate.toPlainString() else ExchangeRateHelper.getRate(exchangeRatesJson, currencySymbol, selectedTransactionCurrency).toString()
                        val formattedRateStr = try {
                            BigDecimal(currentRateRaw).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
                        } catch (e: Exception) {
                            currentRateRaw
                        }
                        Triple(hasStoredRate, currentRateRaw, formattedRateStr)
                    }

                    val hasStoredRate = rateInfo.first
                    val currentRateRaw = rateInfo.second
                    val formattedRateStr = rateInfo.third

                    if (hasStoredRate) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(activeThemeColor.copy(alpha = 0.06f))
                                .border(BorderStroke(0.8.dp, activeThemeColor.copy(alpha = 0.2f)), RoundedCornerShape(6.dp))
                                .clickable {
                                    onSetupRateClick(currentRateRaw)
                                }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = activeThemeColor,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.currency_approved_rate_pattern, selectedTransactionCurrency, formattedRateStr, currencySymbol),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeThemeColor
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .border(BorderStroke(0.8.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)), RoundedCornerShape(6.dp))
                                .clickable {
                                    onSetupRateClick("")
                                }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.currency_setup_rate_desc),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.currency_setup_rate_to_start),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
