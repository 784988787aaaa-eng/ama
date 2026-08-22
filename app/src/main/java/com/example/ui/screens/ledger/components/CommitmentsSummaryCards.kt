package com.example.ui.screens.ledger.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entities.FixedCommitment
import com.example.ui.helper.AutoScaleText
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
fun CommitmentsSummaryCards(
    commitments: List<FixedCommitment>,
    computedCommitments: List<Triple<FixedCommitment, BigDecimal, BigDecimal>>,
    totalCash: BigDecimal,
    currencySymbol: String,
    formatCurrency: (BigDecimal, String) -> String,
    modifier: Modifier = Modifier
) {
    if (commitments.isEmpty()) return

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val greenColor = financialCreditColor(isDark)
    val redColor = financialDebtColor(isDark)

    val (totalRemainingCommitments, netAmount) = remember(computedCommitments, totalCash) {
        val remaining = computedCommitments.fold(BigDecimal.ZERO) { acc, triple -> acc.add(triple.third) }
        val allocated = computedCommitments.fold(BigDecimal.ZERO) { acc, triple ->
            val needed = (triple.first.targetAmount.subtract(triple.first.currentProgress)).max(BigDecimal.ZERO)
            acc.add(needed.subtract(triple.third))
        }
        val net = (totalCash.subtract(allocated)).max(BigDecimal.ZERO)
        Pair(remaining, net)
    }

    val cardBaseBg = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else Color.White

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Card 1: Right in RTL -> "الصافي" (Green)
        Card(
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) greenColor.copy(alpha = 0.12f) else cardBaseBg
            ),
            border = BorderStroke(
                1.dp,
                greenColor.copy(alpha = if (isDark) 0.35f else 0.22f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.ledger_net_prefix).replace(":", "").trim(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(2.dp))
                AutoScaleText(
                    text = formatCurrency(netAmount, currencySymbol).toWesternDigits(),
                    baseFontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = greenColor,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Card 2: Left in RTL -> "باقي الالتزامات" (Red)
        Card(
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) redColor.copy(alpha = 0.12f) else cardBaseBg
            ),
            border = BorderStroke(
                1.dp,
                redColor.copy(alpha = if (isDark) 0.35f else 0.22f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.ledger_remaining_commitments).replace(":", "").trim(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(2.dp))
                AutoScaleText(
                    text = formatCurrency(totalRemainingCommitments, currencySymbol).toWesternDigits(),
                    baseFontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = redColor,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
