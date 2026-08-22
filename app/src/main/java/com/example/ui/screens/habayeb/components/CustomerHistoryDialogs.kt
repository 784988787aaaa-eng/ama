package com.example.ui.screens.habayeb.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.local.entities.HabayebTransaction
import com.example.ui.screens.habayeb.utils.CurrencyConfig

@Composable
fun DeleteBulkTxConfirmDialog(
    show: Boolean,
    selectedCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(id = R.string.habayeb_confirm_delete_txs), fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text(stringResource(id = R.string.habayeb_confirm_delete_txs_msg, selectedCount)) },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(id = R.string.habayeb_delete), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(id = R.string.habayeb_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun ExchangeRateModifyDialog(
    show: Boolean,
    tx: HabayebTransaction?,
    currencySymbol: String,
    activeThemeColor: Color,
    onDismissRequest: () -> Unit,
    onConfirmRateSetup: (String, Double) -> Unit,
    onDeactivateExchange: () -> Unit,
    hasStoredRateForCurrency: (String) -> Boolean,
    getStoredRateForCurrency: (String) -> Double
) {
    if (!show || tx == null) return

    var showRateSetupOverlay by remember { mutableStateOf(false) }
    var setupOverlayCurrency by remember { mutableStateOf("") }
    var setupOverlayInitialRate by remember { mutableStateOf("") }

    val (txCurrency, isExchangeRateRelevant) = remember(tx, currencySymbol) {
        val parsed = CurrencyConfig.parseTransactionCurrency(tx.description, "NONE")
        val curr = if (tx.currencyCode != "DEFAULT" && tx.currencyCode.isNotBlank()) {
            tx.currencyCode
        } else if (parsed.first != "NONE") {
            parsed.first
        } else if (tx.baseCurrencyCode != "DEFAULT" && tx.baseCurrencyCode.isNotBlank()) {
            tx.baseCurrencyCode
        } else {
            currencySymbol
        }
        val isRelevant = (curr != currencySymbol) || tx.isRateCalculated
        Pair(curr, isRelevant)
    }

    Dialog(onDismissRequest = {
        onDismissRequest()
        showRateSetupOverlay = false
    }) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .width(260.dp)
                    .padding(8.dp)
            ) {
                Crossfade(targetState = showRateSetupOverlay, label = "RateModifyTransition") { isSetup ->
                    if (isSetup) {
                        BackHandler {
                            showRateSetupOverlay = false
                        }
                        ExchangeRateSetupContent(
                            selectedCurrency = setupOverlayCurrency,
                            initialRateStr = setupOverlayInitialRate,
                            activeThemeColor = activeThemeColor,
                            onDismiss = {
                                showRateSetupOverlay = false
                            },
                            onConfirm = { newRate ->
                                onConfirmRateSetup(setupOverlayCurrency, newRate)
                                showRateSetupOverlay = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        if (isExchangeRateRelevant) {
                            Row(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!tx.isRateCalculated) {
                                    Button(
                                        onClick = {
                                            val storedRate = getStoredRateForCurrency(txCurrency)
                                            if (hasStoredRateForCurrency(txCurrency)) {
                                                onConfirmRateSetup(txCurrency, storedRate)
                                            } else {
                                                setupOverlayCurrency = txCurrency
                                                setupOverlayInitialRate = ""
                                                showRateSetupOverlay = true
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(38.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.habayeb_activate_exchange),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            onDeactivateExchange()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(38.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.habayeb_deactivate_exchange),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
