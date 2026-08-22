package com.example.ui.screens.habayeb.components

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ui.theme.financialCreditColor
import com.example.ui.theme.financialDebtColor
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.screens.habayeb.utils.CurrencyConfig

@Composable
fun ExchangeRateSetupContent(
    selectedCurrency: String,
    initialRateStr: String,
    activeThemeColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var rateTfv by remember(initialRateStr) {
        mutableStateOf(
            androidx.compose.ui.text.input.TextFieldValue(
                text = initialRateStr,
                selection = androidx.compose.ui.text.TextRange(initialRateStr.length)
            )
        )
    }
    val rateStr = rateTfv.text
    var isChecked by remember { mutableStateOf(false) }
    var showUncheckedError by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

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
            keyboardController?.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val statusColor by animateColorAsState(
        targetValue = when {
            isChecked -> financialCreditColor(isDark)
            showUncheckedError -> financialDebtColor(isDark)
            else -> activeThemeColor.copy(alpha = 0.85f)
        },
        animationSpec = tween(durationMillis = 180),
        label = "statusColor"
    )

    val inputBorderColor by animateColorAsState(
        targetValue = if (isFocused) activeThemeColor else MaterialTheme.colorScheme.outlineVariant,
        label = "inputBorder"
    )

    val usdStr = stringResource(id = R.string.currency_usd)
    val sarStr = stringResource(id = R.string.currency_sar)
    val rateUsdStr = stringResource(id = R.string.habayeb_enter_exchange_rate_usd)
    val rateSarStr = stringResource(id = R.string.habayeb_enter_exchange_rate_sar)
    val rateGenericStr = stringResource(id = R.string.habayeb_enter_exchange_rate_generic, selectedCurrency)

    val currencyLabel = remember(selectedCurrency, usdStr, sarStr, rateUsdStr, rateSarStr, rateGenericStr) {
        when (selectedCurrency) {
            usdStr -> rateUsdStr
            sarStr -> rateSarStr
            else -> rateGenericStr
        }
    }

    val validRateToastStr = stringResource(id = R.string.habayeb_toast_enter_valid_rate)
    val confirmRateFirstToastStr = stringResource(id = R.string.habayeb_toast_confirm_rate_first)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = modifier
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = currencyLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = activeThemeColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .border(1.dp, inputBorderColor, RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicTextField(
                    value = rateTfv,
                    onValueChange = { inputTfv ->
                        val cleanedText = CurrencyConfig.normalizeDigits(inputTfv.text)
                        if (cleanedText.isEmpty() || cleanedText.toDoubleOrNull() != null || cleanedText.last() == '.') {
                            rateTfv = inputTfv.copy(text = cleanedText)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused }
                        .focusRequester(focusRequester),
                    singleLine = true,
                    textStyle = TextStyle(
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeThemeColor
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    ),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (rateStr.isEmpty()) {
                                Text(
                                    text = stringResource(id = R.string.habayeb_exchange_rate_placeholder),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .clickable {
                        isChecked = !isChecked
                        if (isChecked) {
                            showUncheckedError = false
                        }
                    }
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .border(
                            width = 1.dp,
                            color = statusColor,
                            shape = RoundedCornerShape(3.dp)
                        )
                        .background(
                            color = if (isChecked) statusColor else Color.Transparent,
                            shape = RoundedCornerShape(3.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isChecked) {
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
                    text = stringResource(id = R.string.habayeb_confirm_exchange_rate_question),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    textAlign = TextAlign.Start
                )
            }

            Text(
                text = stringResource(id = R.string.habayeb_exchange_rate_hint_text),
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                lineHeight = 10.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(stringResource(id = R.string.habayeb_cancel), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val doubleRate = rateStr.trim().toDoubleOrNull()
                        if (doubleRate == null || doubleRate <= 0.0) {
                            Toast.makeText(context, validRateToastStr, Toast.LENGTH_SHORT).show()
                        } else if (!isChecked) {
                            showUncheckedError = true
                            Toast.makeText(context, confirmRateFirstToastStr, Toast.LENGTH_SHORT).show()
                        } else {
                            onConfirm(doubleRate)
                        }
                    },
                    modifier = Modifier
                        .weight(1.2f)
                        .height(32.dp),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = statusColor,
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(id = R.string.habayeb_save), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ExchangeRateSetupDialog(
    selectedCurrency: String,
    initialRateStr: String,
    activeThemeColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(
            modifier = Modifier
                .width(260.dp)
                .wrapContentHeight()
                .imePadding()
                .shadow(8.dp, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, activeThemeColor.copy(alpha = 0.12f))
        ) {
            ExchangeRateSetupContent(
                selectedCurrency = selectedCurrency,
                initialRateStr = initialRateStr,
                activeThemeColor = activeThemeColor,
                onDismiss = onDismiss,
                onConfirm = onConfirm,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
