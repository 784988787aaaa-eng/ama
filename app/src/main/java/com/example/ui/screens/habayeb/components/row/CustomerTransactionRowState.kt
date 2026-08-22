package com.example.ui.screens.habayeb.components.row

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R
import com.example.data.local.entities.HabayebTransaction
import com.example.domain.model.TransactionType
import com.example.ui.helper.HabayebMathHelper
import com.example.ui.screens.habayeb.utils.CurrencyConfig
import com.example.ui.screens.habayeb.utils.HabayebDateFormatter
import com.example.ui.theme.AlertGoldBgDark
import com.example.ui.theme.AlertGoldBorderDark
import com.example.ui.theme.AlertGoldTextDark
import com.example.ui.theme.AlertGoldTextLight
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueBgDark
import com.example.ui.theme.InfoBlueBgLight
import com.example.ui.theme.InfoBlueTextDark
import com.example.ui.theme.InfoBlueTextLight
import com.example.ui.theme.MutedTextDark
import com.example.ui.theme.MutedTextLight
import com.example.ui.theme.SuccessGreenBgDark
import com.example.ui.theme.SuccessGreenBgLight
import com.example.ui.theme.SuccessGreenBorderDark
import com.example.ui.theme.SuccessGreenBorderLight
import com.example.ui.theme.WarningAmberBg
import com.example.ui.theme.WarningAmberBorder
import com.example.ui.theme.WarningRedBorder
import com.example.ui.theme.WarningRedBorderLight
import com.example.ui.theme.financialCreditColor
import com.example.ui.theme.financialDebtColor
import com.example.ui.viewmodel.FinanceConstants
import java.math.BigDecimal
import java.util.Date

private const val CURRENCY_NONE_TAG = "NONE"

object RowColors {
    fun creditGreen(isDark: Boolean) = financialCreditColor(isDark)
    fun debtRed(isDark: Boolean) = financialDebtColor(isDark)
    fun mutedGray(isDark: Boolean) = if (isDark) MutedTextDark else MutedTextLight
    
    fun alertGoldBg(isDark: Boolean) = if (isDark) AlertGoldBgDark else WarningAmberBg
    fun alertGoldBorder(isDark: Boolean) = if (isDark) AlertGoldBorderDark else WarningAmberBorder
    fun alertGoldText(isDark: Boolean) = if (isDark) AlertGoldTextDark else AlertGoldTextLight
    
    fun infoBlueBg(isDark: Boolean) = if (isDark) InfoBlueBgDark else InfoBlueBgLight
    fun infoBlueBorder(isDark: Boolean) = InfoBlue
    fun infoBlueText(isDark: Boolean) = if (isDark) InfoBlueTextDark else InfoBlueTextLight
    
    fun successGreenBg(isDark: Boolean) = if (isDark) SuccessGreenBgDark else SuccessGreenBgLight
    fun successGreenBorder(isDark: Boolean) = if (isDark) SuccessGreenBorderDark else SuccessGreenBorderLight
    fun warningRedBorder(isDark: Boolean) = if (isDark) WarningRedBorder else WarningRedBorderLight
    fun darkGray(isDark: Boolean) = if (isDark) MutedTextDark else MutedTextLight
}

@Immutable
data class TransactionRowCachedData(
    val cleanDescription: String,
    val indicatorColor: Color,
    val txArrow: ImageVector,
    val formattedAmount: String,
    val displayCurrency: String,
    val equivalentAmountText: String?,
    val dayNameResId: Int,
    val dateStr: String,
    val timeStr: String,
    val isTxForeign: Boolean,
    val isCalculated: Boolean,
    val typeResId: Int
)

object CustomerTransactionRowStateCalculator {
    fun calculate(
        tx: HabayebTransaction,
        isDark: Boolean,
        currencySymbol: String,
        initialType: String
    ): TransactionRowCachedData {
        val parsedCurrencyInfo = CurrencyConfig.parseTransactionCurrency(tx.description, CURRENCY_NONE_TAG)
        val txCurrencySymbol = if (tx.currencyCode != FinanceConstants.DEFAULT_CURRENCY_CODE && tx.currencyCode.isNotBlank()) {
            tx.currencyCode
        } else if (parsedCurrencyInfo.first != CURRENCY_NONE_TAG) {
            parsedCurrencyInfo.first
        } else if (tx.baseCurrencyCode != FinanceConstants.DEFAULT_CURRENCY_CODE && tx.baseCurrencyCode.isNotBlank()) {
            tx.baseCurrencyCode
        } else {
            currencySymbol
        }
        val isTxForeign = txCurrencySymbol != currencySymbol
        val cleanDescription = if (parsedCurrencyInfo.first != CURRENCY_NONE_TAG) parsedCurrencyInfo.second else tx.description

        val txType = TransactionType.fromValue(tx.type)
        val indicatorColor = when (txType) {
            TransactionType.OWED_BY_THEM, TransactionType.OWED_TO_THEM -> RowColors.debtRed(isDark)
            else -> RowColors.creditGreen(isDark)
        }

        val txArrow = when (txType) {
            TransactionType.OWED_BY_THEM, TransactionType.PAYMENT_TO_THEM -> Icons.Default.ArrowUpward
            TransactionType.PAYMENT_BY_THEM, TransactionType.OWED_TO_THEM -> Icons.Default.ArrowDownward
            else -> Icons.Default.ArrowUpward
        }

        val displayAmount: BigDecimal
        val displayCurrency: String
        val equivalentAmount: BigDecimal?
        val equivalentCurrency: String?

        val sourceAmount = if (tx.foreignAmount.compareTo(BigDecimal.ZERO) > 0) tx.foreignAmount else tx.amount

        if (tx.isRateCalculated) {
            val baseCurrency = if (tx.baseCurrencyCode != FinanceConstants.DEFAULT_CURRENCY_CODE && tx.baseCurrencyCode.isNotBlank()) {
                tx.baseCurrencyCode
            } else {
                FinanceConstants.FALLBACK_CURRENCY_SYMBOL
            }
            val origCurrency = txCurrencySymbol
            
            displayAmount = sourceAmount
            displayCurrency = origCurrency
            equivalentAmount = tx.equivalentAmount
            equivalentCurrency = baseCurrency
        } else {
            displayAmount = sourceAmount
            displayCurrency = txCurrencySymbol
            equivalentAmount = null
            equivalentCurrency = null
        }

        val formattedAmount = HabayebMathHelper.formatSmart(displayAmount)

        val equivalentAmountText = if (equivalentAmount != null && equivalentCurrency != null) {
            val formattedEquiv = HabayebMathHelper.formatSmart(equivalentAmount)
            "($formattedEquiv $equivalentCurrency)"
        } else null

        val d = Date(tx.timestamp * 1000L)
        val dateStr = HabayebDateFormatter.formatShortDate(d)
        val timeStr = HabayebDateFormatter.formatTime12h(d)
        val dayNameResId = HabayebDateFormatter.getDayOfWeekResId(tx.timestamp)

        val typeResId = when (txType) {
            TransactionType.OWED_BY_THEM -> R.string.habayeb_pdf_tx_owed_by
            TransactionType.PAYMENT_BY_THEM -> R.string.habayeb_pdf_tx_payment_by
            TransactionType.OWED_TO_THEM -> R.string.habayeb_pdf_tx_owed_to
            TransactionType.PAYMENT_TO_THEM -> R.string.habayeb_pdf_tx_payment_to
            else -> R.string.habayeb_pdf_tx_generic
        }

        return TransactionRowCachedData(
            cleanDescription = cleanDescription,
            indicatorColor = indicatorColor,
            txArrow = txArrow,
            formattedAmount = formattedAmount,
            displayCurrency = displayCurrency,
            equivalentAmountText = equivalentAmountText,
            dayNameResId = dayNameResId,
            dateStr = dateStr,
            timeStr = timeStr,
            isTxForeign = isTxForeign,
            isCalculated = tx.isRateCalculated,
            typeResId = typeResId
        )
    }
}
