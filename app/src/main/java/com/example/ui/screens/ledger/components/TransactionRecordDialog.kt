package com.example.ui.screens.ledger.components

import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.data.local.entities.TransactionDb
import com.example.ui.screens.CalculatorDialog
import com.example.ui.screens.habayeb.utils.CurrencyConfig
import com.example.ui.theme.financialCreditColor
import com.example.ui.theme.financialDebtColor

@Composable
fun TransactionRecordDialog(
    showTxDialog: Boolean,
    txDialogType: String,
    editingTransaction: TransactionDb?,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (id: String?, type: String, category: String, amount: Double, description: String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!showTxDialog) return

    val context = LocalContext.current
    val initialAmount = remember(editingTransaction, showTxDialog) { editingTransaction?.amount?.toPlainString() ?: "" }
    val initialDesc = remember(editingTransaction, showTxDialog) { editingTransaction?.description ?: "" }

    var numAmountTfv by remember(editingTransaction, showTxDialog) {
        mutableStateOf(TextFieldValue(text = initialAmount, selection = TextRange(initialAmount.length)))
    }
    var descriptionTfv by remember(editingTransaction, showTxDialog) {
        mutableStateOf(TextFieldValue(text = initialDesc, selection = TextRange(initialDesc.length)))
    }
    val numAmount = numAmountTfv.text
    val descriptionStr = descriptionTfv.text

    val categoryName = remember(editingTransaction, txDialogType) {
        editingTransaction?.category ?: if (txDialogType == "INCOME") context.getString(R.string.ledger_category_overall_income) else context.getString(R.string.ledger_category_expense)
    }

    var showCalcPopup by rememberSaveable { mutableStateOf(false) }
    var isSavingTx by remember(showTxDialog) { mutableStateOf(false) }

    val parsedAmount = remember(numAmount) {
        CurrencyConfig.normalizeDigits(numAmount).toDoubleOrNull() ?: 0.0
    }
    val isConfirmButtonEnabled = !isSavingTx && parsedAmount > 0.0

    val focusRequester = remember { FocusRequester() }
    val descriptionFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val softwareKeyboardController = LocalSoftwareKeyboardController.current

    val view = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(view) {
        val window = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        onDispose {}
    }

    LaunchedEffect(Unit) {
        try {
            kotlinx.coroutines.delay(150)
            focusRequester.requestFocus()
            softwareKeyboardController?.show()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
        }
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val isIncome = txDialogType == "INCOME"

    // Colors: Green for Income (وارد), Red for Expense (منصرف) matching action buttons
    val themeColor = if (isIncome) {
        financialCreditColor(isDark)
    } else {
        financialDebtColor(isDark)
    }
    val themeColorSub = themeColor.copy(alpha = 0.85f)

    // Dialog background & inputs using MaterialTheme color scheme
    val dialogBgColor = MaterialTheme.colorScheme.surface
    val textInputBgColor = if (isDark) themeColor.copy(alpha = 0.12f) else themeColor.copy(alpha = 0.05f)
    val textColor = MaterialTheme.colorScheme.onSurface

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = dialogBgColor,
            tonalElevation = 0.dp, // Disable tonal elevation to prevent neutral gray overlays
            border = BorderStroke(2.dp, themeColor),
            modifier = Modifier
                .width(280.dp)
                .wrapContentHeight()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .padding(14.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Text(
                        text = if (editingTransaction != null) stringResource(id = R.string.ledger_edit_transaction_title) else if (isIncome) stringResource(id = R.string.ledger_add_income_title) else stringResource(id = R.string.ledger_add_expense_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = themeColor,
                            fontSize = 15.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }

                // Compact fields column with distinct borders
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Amount Input
                    OutlinedTextField(
                        value = numAmountTfv,
                        onValueChange = { numAmountTfv = it },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { descriptionFocusRequester.requestFocus() }
                        ),
                        label = {
                            Text(
                                text = stringResource(id = R.string.ledger_amount_label, currencySymbol),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        singleLine = true,
                        leadingIcon = {
                            IconButton(
                                onClick = { showCalcPopup = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = stringResource(id = R.string.habayeb_calculator),
                                    tint = themeColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedContainerColor = textInputBgColor,
                            unfocusedContainerColor = textInputBgColor,
                            focusedBorderColor = themeColor,
                            unfocusedBorderColor = themeColor.copy(alpha = 0.6f),
                            focusedLabelColor = themeColor,
                            unfocusedLabelColor = if (isDark) themeColor.copy(alpha = 0.8f) else themeColorSub
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            color = textColor,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )

                    // Description Input
                    OutlinedTextField(
                        value = descriptionTfv,
                        onValueChange = { descriptionTfv = it },
                        label = {
                            Text(
                                text = stringResource(id = if (isIncome) R.string.ledger_desc_income_hint else R.string.ledger_desc_expense_hint),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        singleLine = true,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(descriptionFocusRequester),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedContainerColor = textInputBgColor,
                            unfocusedContainerColor = textInputBgColor,
                            focusedBorderColor = themeColor,
                            unfocusedBorderColor = themeColor.copy(alpha = 0.6f),
                            focusedLabelColor = themeColor,
                            unfocusedLabelColor = if (isDark) themeColor.copy(alpha = 0.8f) else themeColorSub
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            color = textColor,
                            textAlign = TextAlign.Right,
                            fontSize = 13.sp
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                softwareKeyboardController?.hide()
                                if (isConfirmButtonEnabled && parsedAmount > 0) {
                                    isSavingTx = true
                                    onSave(
                                        editingTransaction?.id,
                                        txDialogType,
                                        categoryName,
                                        parsedAmount,
                                        descriptionStr
                                    )
                                    onDismiss()
                                }
                            }
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel button
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isDark) themeColor.copy(alpha = 0.9f) else themeColorSub
                        )
                    ) {
                        Text(
                            text = stringResource(id = R.string.common_cancel),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Save button
                    Button(
                        enabled = isConfirmButtonEnabled,
                        onClick = {
                            if (isSavingTx) return@Button
                            if (parsedAmount > 0) {
                                isSavingTx = true
                                com.example.ui.helper.VibrationHelper.triggerSuccessVibration(context)
                                onSave(
                                    editingTransaction?.id,
                                    txDialogType,
                                    categoryName,
                                    parsedAmount,
                                    descriptionStr
                                )
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeColor,
                            contentColor = Color.White,
                            disabledContainerColor = themeColor.copy(alpha = 0.35f),
                            disabledContentColor = if (isDark) Color.White.copy(alpha = 0.4f) else Color.Gray.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(
                            text = stringResource(id = R.string.ledger_save_tx_btn),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }

    if (showCalcPopup) {
        CalculatorDialog(
            onDismiss = { showCalcPopup = false },
            onValueConfirmed = { calcResult ->
                val resultStr = if (calcResult % 1.0 == 0.0) calcResult.toInt().toString() else calcResult.toString()
                numAmountTfv = TextFieldValue(text = resultStr, selection = TextRange(resultStr.length))
                showCalcPopup = false
            },
            activeThemeColor = themeColor,
            activeSubColor = themeColor.copy(alpha = 0.15f)
        )
    }
}
