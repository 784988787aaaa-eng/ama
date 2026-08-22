package com.example.ui.screens.ledger.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.TransactionType
import com.example.ui.theme.WhatsAppLightGreen
import com.example.ui.viewmodel.DayLedger
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
fun DayCardWhatsAppShareButton(
    dayLedger: DayLedger,
    dailyIncome: BigDecimal,
    dailyExpense: BigDecimal,
    currencySymbol: String,
    formatCurrency: (BigDecimal, String) -> String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = WhatsAppLightGreen.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, WhatsAppLightGreen.copy(alpha = 0.28f)),
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable {
                shareDayLedgerViaWhatsApp(
                    context = context,
                    dayLedger = dayLedger,
                    dailyIncome = dailyIncome,
                    dailyExpense = dailyExpense,
                    currencySymbol = currencySymbol,
                    formatCurrency = formatCurrency
                )
            }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("💬", fontSize = 11.sp, modifier = Modifier.padding(end = 4.dp))
            Text(
                text = stringResource(id = R.string.ledger_whatsapp_whatsapp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = WhatsAppLightGreen
            )
        }
    }
}

private fun shareDayLedgerViaWhatsApp(
    context: Context,
    dayLedger: DayLedger,
    dailyIncome: BigDecimal,
    dailyExpense: BigDecimal,
    currencySymbol: String,
    formatCurrency: (BigDecimal, String) -> String
) {
    val builder = java.lang.StringBuilder()
    builder.append(context.getString(R.string.ledger_daily_record_share_title, dayLedger.dayOfWeek, dayLedger.fullDate))
    builder.append(context.getString(R.string.ledger_share_total_income, formatCurrency(dailyIncome, currencySymbol).toWesternDigits()))
    builder.append(context.getString(R.string.ledger_share_total_expense, formatCurrency(dailyExpense, currencySymbol).toWesternDigits()))
    builder.append("___________________\n\n")

    val txs = dayLedger.transactions.sortedBy { it.timestamp }
    if (txs.isEmpty()) {
        builder.append(context.getString(R.string.ledger_no_txs_today))
    } else {
        txs.forEach { tx ->
            val isTxIncome = tx.type == TransactionType.INCOME.value
            val icon = if (isTxIncome) "🟢 (+)" else "🔴 (-)"
            val title = tx.description.ifBlank {
                if (isTxIncome) context.getString(R.string.transaction_income)
                else context.getString(R.string.transaction_expense)
            }
            builder.append(context.getString(R.string.ledger_share_tx_format, icon, title, formatCurrency(tx.amount, currencySymbol).toWesternDigits()))
        }
    }

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, builder.toString())
    }
    try {
        shareIntent.setPackage("com.whatsapp")
        context.startActivity(shareIntent)
    } catch (e: ActivityNotFoundException) {
        try {
            shareIntent.setPackage("com.whatsapp.w4b")
            context.startActivity(shareIntent)
        } catch (e2: ActivityNotFoundException) {
            shareIntent.setPackage(null)
            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.ledger_share_via)))
        }
    }
}
