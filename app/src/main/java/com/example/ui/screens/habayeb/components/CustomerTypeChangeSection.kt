package com.example.ui.screens.habayeb.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.DarkNeutralTrack
import com.example.ui.theme.LightNeutralTrack
import com.example.ui.theme.financialCreditColor
import com.example.ui.theme.financialDebtColor
import com.example.ui.viewmodel.FinanceConstants

@Composable
fun CustomerTypeChangeSection(
    currentType: String,
    activeThemeColor: Color,
    onUpdateCustomerType: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingTypeChange by remember { mutableStateOf<String?>(null) }

    val bgColor = MaterialTheme.colorScheme.background
    val isDark = remember(bgColor) { bgColor.run { red < 0.5f } }
    val activeRedColor = remember(isDark) { financialDebtColor(isDark) }
    val activeGreenColor = remember(isDark) { financialCreditColor(isDark) }
    val inactiveBgColor = remember(isDark) { if (isDark) DarkNeutralTrack else LightNeutralTrack }
    val inactiveTextColor = remember(isDark) { if (isDark) Color.Gray else Color.DarkGray }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(id = R.string.context_menu_change_type_label),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // زر "له" (OWED_TO_THEM)
                val isToThemSelected = currentType == FinanceConstants.TYPE_OWED_TO_THEM
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(26.dp)
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isToThemSelected) activeGreenColor else inactiveBgColor)
                        .clickable {
                            if (!isToThemSelected) {
                                pendingTypeChange = FinanceConstants.TYPE_OWED_TO_THEM
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.habayeb_to_them),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isToThemSelected) Color.White else inactiveTextColor
                    )
                }

                // زر "عليه" (OWED_BY_THEM)
                val isByThemSelected = currentType == FinanceConstants.TYPE_OWED_BY_THEM
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(26.dp)
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isByThemSelected) activeRedColor else inactiveBgColor)
                        .clickable {
                            if (!isByThemSelected) {
                                pendingTypeChange = FinanceConstants.TYPE_OWED_BY_THEM
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.habayeb_owed),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isByThemSelected) Color.White else inactiveTextColor
                    )
                }
            }
        }
    }

    if (pendingTypeChange != null) {
        AlertDialog(
            onDismissRequest = { pendingTypeChange = null },
            title = {
                Text(
                    text = stringResource(id = R.string.context_menu_change_type_confirm_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.context_menu_change_type_confirm_msg),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { pendingTypeChange = null },
                    colors = ButtonDefaults.buttonColors(containerColor = activeThemeColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.context_menu_cancel),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                val confirmButtonColor = when (pendingTypeChange) {
                    FinanceConstants.TYPE_OWED_TO_THEM -> financialCreditColor(isDark)
                    FinanceConstants.TYPE_OWED_BY_THEM -> financialDebtColor(isDark)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                TextButton(
                    onClick = {
                        val newType = pendingTypeChange!!
                        onUpdateCustomerType(newType)
                        pendingTypeChange = null
                        onDismiss()
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.context_menu_confirm),
                        color = confirmButtonColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
