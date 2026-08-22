package com.example.ui.screens.habayeb.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.StringUtils
import com.example.domain.model.TransactionType
import com.example.ui.helper.rememberContactPicker
import com.example.ui.screens.CalculatorDialog
import com.example.ui.screens.habayeb.utils.ExchangeRateHelper
import com.example.ui.theme.financialCreditColor
import com.example.ui.theme.financialDebtColor
import com.example.ui.viewmodel.HabayebFinanceViewModel
import java.util.Calendar

@Composable
fun AddCustomerPopup(
    viewModel: HabayebFinanceViewModel,
    onDismiss: () -> Unit,
    onCustomerAdded: (String) -> Unit = {},
    activeThemeColor: Color,
    activeSubColor: Color
) {
    val isDark = MaterialTheme.colorScheme.background.run { red < 0.5f }
    val context = LocalContext.current

    var nameStr by rememberSaveable { mutableStateOf("") }
    var phoneStr by rememberSaveable { mutableStateOf("") }
    var notesStr by rememberSaveable { mutableStateOf("") }
    var initialAmountStr by rememberSaveable { mutableStateOf("") }
    var initialType by rememberSaveable { mutableStateOf<String?>(null) }

    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val debtRed = remember(isDark) { financialDebtColor(isDark) }
    val creditGreen = remember(isDark) { financialCreditColor(isDark) }
    val defaultPrimary = MaterialTheme.colorScheme.primary

    val dynamicThemeColor = remember(initialType, isDark, debtRed, creditGreen, defaultPrimary) {
        when (initialType) {
            TransactionType.OWED_BY_THEM.value -> debtRed     // عند اختيار "عليه": يتلون بالأحمر المالي الجذاب
            TransactionType.OWED_TO_THEM.value -> creditGreen // عند اختيار "له": يتلون بالأخضر المالي الجذاب
            else -> defaultPrimary // أرجواني افتراضي قبل تحديد النوع
        }
    }
    val currencySymbol = settings.currencySymbol
    var selectedTransactionCurrency by rememberSaveable { mutableStateOf(currencySymbol) }
    var applyExchangeRate by rememberSaveable { mutableStateOf(false) }
    var showRateSetupOverlay by rememberSaveable { mutableStateOf(false) }
    var tempRateStr by rememberSaveable { mutableStateOf("") }

    val settingsRate = remember(settings.exchangeRatesJson, currencySymbol, selectedTransactionCurrency) {
        val currentRateVal = ExchangeRateHelper.getRate(settings.exchangeRatesJson, currencySymbol, selectedTransactionCurrency)
        if (currentRateVal > 0.0) currentRateVal else 1.0
    }

    var showCalculator by rememberSaveable { mutableStateOf(false) }
    var isSavingCustomer by rememberSaveable { mutableStateOf(false) }

    val existingCustomers by viewModel.habayebCustomersState.collectAsStateWithLifecycle()
    val normalizedInputName = remember(nameStr) { StringUtils.normalizeArabic(nameStr.trim()) }
    val isDuplicateName = remember(normalizedInputName, existingCustomers, isSavingCustomer) {
        if (isSavingCustomer || normalizedInputName.isBlank()) false
        else {
            existingCustomers.any { customer ->
                val normalizedExisting = StringUtils.normalizeArabic(customer.name.trim())
                normalizedExisting.isNotBlank() && normalizedExisting.equals(normalizedInputName, ignoreCase = true)
            }
        }
    }

    var selectedCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    var showCustomDatePicker by remember { mutableStateOf(false) }

    if (showCustomDatePicker) {
        CustomDateTimePickerDialog(
            initialMillis = selectedCalendar.timeInMillis,
            onDismiss = { showCustomDatePicker = false },
            onDateTimeSelected = { millis ->
                selectedCalendar = (selectedCalendar.clone() as Calendar).apply { timeInMillis = millis }
                showCustomDatePicker = false
            }
        )
    }

    val focusRequester = remember { FocusRequester() }
    val phoneFocusRequester = remember { FocusRequester() }
    val initialAmountFocusRequester = remember { FocusRequester() }
    val notesFocusRequester = remember { FocusRequester() }
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
            e.printStackTrace()
        }
    }

    val launchContactPicker = rememberContactPicker { name, phone ->
        if (name.isNotBlank()) nameStr = name
        if (phone.isNotBlank()) phoneStr = phone
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .widthIn(max = 350.dp)
                    .fillMaxWidth(0.94f)
                    .imePadding()
                    .padding(2.dp)
            ) {
                Crossfade(targetState = showRateSetupOverlay, label = "CustomerFormTransition") { isSetup ->
                    if (isSetup) {
                        BackHandler {
                            showRateSetupOverlay = false
                            applyExchangeRate = false
                        }
                        ExchangeRateSetupContent(
                            selectedCurrency = selectedTransactionCurrency,
                            initialRateStr = tempRateStr,
                            activeThemeColor = dynamicThemeColor,
                            onDismiss = {
                                showRateSetupOverlay = false
                                applyExchangeRate = false
                            },
                            onConfirm = { newRate ->
                                val newSettings = settings.copy(
                                    exchangeRatesJson = ExchangeRateHelper.setRate(settings.exchangeRatesJson, currencySymbol, selectedTransactionCurrency, newRate)
                                )
                                viewModel.saveSettings(newSettings)
                                applyExchangeRate = true
                                showRateSetupOverlay = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .navigationBarsPadding()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(id = R.string.habayeb_cancel),
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = stringResource(id = R.string.dialog_title_add_account),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = dynamicThemeColor,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.size(24.dp))
                            }

                            AddCustomerFormFields(
                                nameStr = nameStr,
                                onNameChange = { nameStr = it },
                                phoneStr = phoneStr,
                                onPhoneChange = { phoneStr = it },
                                notesStr = notesStr,
                                onNotesChange = { notesStr = it },
                                initialAmountStr = initialAmountStr,
                                onInitialAmountChange = { initialAmountStr = it },
                                isDuplicateName = isDuplicateName,
                                selectedTransactionCurrency = selectedTransactionCurrency,
                                activeThemeColor = dynamicThemeColor,
                                onCalculatorClick = { showCalculator = true },
                                onCalendarClick = {
                                    focusManager.clearFocus()
                                    softwareKeyboardController?.hide()
                                    showCustomDatePicker = true
                                },
                                onContactPickerClick = { launchContactPicker() },
                                onDone = { focusManager.clearFocus() },
                                isDark = isDark,
                                focusRequester = focusRequester,
                                initialAmountFocusRequester = initialAmountFocusRequester,
                                notesFocusRequester = notesFocusRequester,
                                phoneFocusRequester = phoneFocusRequester
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            AddCustomerTypeAndCurrencySelector(
                                currencySymbol = currencySymbol,
                                selectedTransactionCurrency = selectedTransactionCurrency,
                                onCurrencySelected = {
                                    selectedTransactionCurrency = it
                                    applyExchangeRate = false
                                },
                                applyExchangeRate = applyExchangeRate,
                                onApplyExchangeRateChange = { applyExchangeRate = it },
                                initialType = initialType,
                                onTypeSelected = { initialType = it },
                                isSavingCustomer = isSavingCustomer,
                                onSaveClick = {
                                    val formData = AddCustomerFormData(
                                        nameStr = nameStr,
                                        phoneStr = phoneStr,
                                        notesStr = notesStr,
                                        initialAmountStr = initialAmountStr,
                                        initialType = initialType,
                                        selectedTransactionCurrency = selectedTransactionCurrency,
                                        currencySymbol = currencySymbol,
                                        applyExchangeRate = applyExchangeRate,
                                        selectedCalendar = selectedCalendar,
                                        settingsRate = settingsRate,
                                        isDuplicateName = isDuplicateName
                                    )
                                    AddCustomerSaveHelper.handleSave(
                                        context = context,
                                        viewModel = viewModel,
                                        formData = formData,
                                        onIsSavingChange = { isSavingCustomer = it },
                                        onShowRateSetup = { rate ->
                                            tempRateStr = rate
                                            showRateSetupOverlay = true
                                        },
                                        onSuccess = onCustomerAdded,
                                        onDismiss = onDismiss
                                    )
                                },
                                activeThemeColor = dynamicThemeColor,
                                isDark = isDark,
                                exchangeRatesJson = settings.exchangeRatesJson,
                                onRequestRateSetup = { rate ->
                                    tempRateStr = rate
                                    showRateSetupOverlay = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCalculator) {
        CalculatorDialog(
            onDismiss = { showCalculator = false },
            onValueConfirmed = { value ->
                initialAmountStr = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
                showCalculator = false
            },
            activeThemeColor = dynamicThemeColor,
            activeSubColor = activeSubColor
        )
    }
}
