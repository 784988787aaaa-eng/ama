package com.example.ui.screens.trash.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.trash.utils.ParsedTrashData

@Composable
fun TrashDetailAmountCard(
    parsedData: ParsedTrashData,
    amountColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = amountColor.copy(alpha = if (isDark) 0.12f else 0.08f)
        ),
        border = BorderStroke(1.dp, amountColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = parsedData.amountText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = amountColor,
                textAlign = TextAlign.Center
            )

            if (parsedData.txTypeDisplay.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = amountColor.copy(alpha = 0.15f),
                    border = BorderStroke(0.5.dp, amountColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = parsedData.txTypeDisplay,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = amountColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
