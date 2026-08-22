package com.example.ui.screens.ledger.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.financialCreditColor
import com.example.ui.theme.financialDebtColor
import java.math.BigDecimal

private fun String.toWesternDigits(): String {
    var result = this
    val eastern = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    val western = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    for (i in 0..9) {
        result = result.replace(eastern[i], western[i])
    }
    return result
}

@Composable
fun DayCardSummaryBar(
    dailyIncome: BigDecimal,
    dailyExpense: BigDecimal,
    dailyNet: BigDecimal,
    isDark: Boolean,
    currencySymbol: String,
    formatCurrency: (BigDecimal, String) -> String,
    modifier: Modifier = Modifier
) {
    val barBg = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else Color(0xFFF7F9FC)
    val netSummaryColor = if (dailyNet > BigDecimal.ZERO) financialCreditColor(isDark)
    else if (dailyNet < BigDecimal.ZERO) financialDebtColor(isDark)
    else MaterialTheme.colorScheme.onSurfaceVariant
    val netSummaryPrefix = if (dailyNet > BigDecimal.ZERO) "+" else ""

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = barBg,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cell 1: Right in RTL -> الوارد (Income)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${stringResource(id = R.string.ledger_daily_income).replace("اليوم", "").trim()}: ${formatCurrency(dailyIncome, currencySymbol).toWesternDigits()}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = financialCreditColor(isDark),
                    maxLines = 1
                )
            }

            VerticalDivider(
                modifier = Modifier.height(18.dp),
                thickness = 0.8.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )

            // Cell 2: Center -> المنصرف (Expense)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${stringResource(id = R.string.ledger_daily_expense).replace("اليوم", "").trim()}: ${formatCurrency(dailyExpense, currencySymbol).toWesternDigits()}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = financialDebtColor(isDark),
                    maxLines = 1
                )
            }

            VerticalDivider(
                modifier = Modifier.height(18.dp),
                thickness = 0.8.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )

            // Cell 3: Left in RTL -> الصافي (Net)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${stringResource(id = R.string.ledger_daily_net)}: $netSummaryPrefix${formatCurrency(dailyNet, currencySymbol).toWesternDigits()}",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = netSummaryColor,
                    maxLines = 1
                )
            }
        }
    }
}
