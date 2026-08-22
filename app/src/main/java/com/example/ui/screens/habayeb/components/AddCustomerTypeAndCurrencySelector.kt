package com.example.ui.screens.habayeb.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.TransactionType
import com.example.ui.screens.habayeb.utils.ExchangeRateHelper
import com.example.ui.theme.financialCreditColor
import com.example.ui.theme.financialDebtColor
import java.math.BigDecimal

@Composable
fun AddCustomerTypeAndCurrencySelector(
    currencySymbol: String,
    selectedTransactionCurrency: String,
    onCurrencySelected: (String) -> Unit,
    applyExchangeRate: Boolean,
    onApplyExchangeRateChange: (Boolean) -> Unit,
    initialType: String?,
    onTypeSelected: (String) -> Unit,
    isSavingCustomer: Boolean,
    onSaveClick: () -> Unit,
    activeThemeColor: Color,
    isDark: Boolean,
    exchangeRatesJson: String,
    onRequestRateSetup: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val debtRed = remember(isDark) { financialDebtColor(isDark) }
    val creditGreen = remember(isDark) { financialCreditColor(isDark) }

    val currentTypeThemeColor = remember(initialType, debtRed, creditGreen, activeThemeColor) {
        when (initialType) {
            TransactionType.OWED_BY_THEM.value -> debtRed
            TransactionType.OWED_TO_THEM.value -> creditGreen
            else -> activeThemeColor
        }
    }

    val currencySar = context.getString(R.string.currency_sar)
    val currencyUsd = context.getString(R.string.currency_usd)
    val currencyYer = context.getString(R.string.currency_yer)

    val famousCurrencies = remember(currencyYer, currencyUsd, currencySar) {
        listOf(
            Pair(currencyYer, context.getString(R.string.currency_label_yer)),
            Pair(currencyUsd, context.getString(R.string.currency_label_usd)),
            Pair(currencySar, context.getString(R.string.currency_label_sar))
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. خيارات العملة (Currency selection buttons)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            famousCurrencies.forEachIndexed { index, (sym, label) ->
                val isSelected = selectedTransactionCurrency == sym
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (selectedTransactionCurrency != sym) {
                                onCurrencySelected(sym)
                            }
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) currentTypeThemeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, if (isSelected) currentTypeThemeColor else MaterialTheme.colorScheme.outline, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(currentTypeThemeColor)
                            )
                        }
                    }
                }
                if (index < famousCurrencies.size - 1) {
                    Spacer(modifier = Modifier.width(10.dp))
                }
            }
        }

        // 2. سعر الصرف الاختياري (Optional currency exchange rate option)
        if (selectedTransactionCurrency != currencySymbol) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                // Clickable checkbox + label
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            val nextValue = !applyExchangeRate
                            onApplyExchangeRateChange(nextValue)
                            if (nextValue) {
                                val hasStoredRate = ExchangeRateHelper.hasRate(exchangeRatesJson, currencySymbol, selectedTransactionCurrency)
                                val currentRateVal = ExchangeRateHelper.getRate(exchangeRatesJson, currencySymbol, selectedTransactionCurrency)
                                if (!hasStoredRate || currentRateVal == 1.0) {
                                    onRequestRateSetup("")
                                }
                            }
                        }
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .border(1.5.dp, activeThemeColor, RoundedCornerShape(3.dp))
                            .background(if (applyExchangeRate) activeThemeColor else Color.Transparent, RoundedCornerShape(3.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (applyExchangeRate) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(id = R.string.habayeb_add_with_rate_question),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Graceful interactive rate display badge
                if (applyExchangeRate) {
                    val rateState = remember(exchangeRatesJson, currencySymbol, selectedTransactionCurrency) {
                        val has = ExchangeRateHelper.hasRate(exchangeRatesJson, currencySymbol, selectedTransactionCurrency)
                        val rate = ExchangeRateHelper.getRate(exchangeRatesJson, currencySymbol, selectedTransactionCurrency)
                        val formatted = try {
                            BigDecimal.valueOf(rate).setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
                        } catch (e: Exception) {
                            rate.toString()
                        }
                        Triple(has, rate, formatted)
                    }
                    val hasStoredRate = rateState.first
                    val currentRate = rateState.second
                    val formattedRateStr = rateState.third
                    if (hasStoredRate) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(activeThemeColor.copy(alpha = 0.06f))
                                .border(BorderStroke(0.8.dp, activeThemeColor.copy(alpha = 0.2f)), RoundedCornerShape(6.dp))
                                .clickable {
                                    onRequestRateSetup(currentRate.toString())
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
                                text = context.getString(R.string.currency_approved_rate_pattern, selectedTransactionCurrency, formattedRateStr, currencySymbol),
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
                                    onRequestRateSetup("")
                                }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = context.getString(R.string.currency_setup_rate_desc),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = context.getString(R.string.currency_setup_rate_to_start),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // 3. الأفعال المدمجة ( له / عليه ) وزر الحفظ (Merged Actions Row)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // له / عليه switcher
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // "عليه" option
                val isOwedByThem = initialType == TransactionType.OWED_BY_THEM.value
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTypeSelected(TransactionType.OWED_BY_THEM.value)
                        }
                        .padding(horizontal = 2.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.habayeb_owed),
                        fontSize = 10.sp,
                        fontWeight = if (isOwedByThem) FontWeight.Bold else FontWeight.Normal,
                        color = if (isOwedByThem) debtRed else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, if (isOwedByThem) debtRed else MaterialTheme.colorScheme.outlineVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isOwedByThem) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(debtRed)
                            )
                        }
                    }
                }

                // "له" option
                val isOwedToThem = initialType == TransactionType.OWED_TO_THEM.value
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTypeSelected(TransactionType.OWED_TO_THEM.value)
                        }
                        .padding(horizontal = 2.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.habayeb_to_them),
                        fontSize = 10.sp,
                        fontWeight = if (isOwedToThem) FontWeight.Bold else FontWeight.Normal,
                        color = if (isOwedToThem) creditGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, if (isOwedToThem) creditGreen else MaterialTheme.colorScheme.outlineVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isOwedToThem) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(creditGreen)
                            )
                        }
                    }
                }
            }

            // تأكيد وحفظ زر (Save Button)
            Button(
                enabled = !isSavingCustomer,
                onClick = onSaveClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = activeThemeColor,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(36.dp)
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.btn_save),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
