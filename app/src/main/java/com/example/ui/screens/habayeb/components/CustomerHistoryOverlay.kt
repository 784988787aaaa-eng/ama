package com.example.ui.screens.habayeb.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.map
import com.example.R
import com.example.data.local.entities.HabayebCustomer
import com.example.ui.screens.habayeb.utils.CustomerHistoryCalculator
import com.example.ui.screens.habayeb.utils.HabayebRecurringManager
import com.example.ui.screens.habayeb.utils.rememberFilteredCustomerTransactions
import com.example.ui.viewmodel.HabayebFinanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHistoryOverlay(
    customer: HabayebCustomer,
    viewModel: HabayebFinanceViewModel,
    onDismiss: () -> Unit,
    activeThemeColor: Color,
    activeSubColor: Color,
    currencySymbol: String,
    contentPadding: PaddingValues = PaddingValues(),
    isSearchActive: Boolean = false,
    onSearchActiveChanged: (Boolean) -> Unit = {},
    onTxMultiSelectActiveChanged: (Boolean) -> Unit = {}
) {
    val bgColor = MaterialTheme.colorScheme.background
    val isDark = remember(bgColor) { bgColor.luminance() < 0.5f }
    val customers by viewModel.habayebCustomersState.collectAsStateWithLifecycle()
    val activeCustomer = customers.find { it.id == customer.id } ?: customer

    val initialTxs = remember(activeCustomer.id) {
        viewModel.getInitialTransactionsForCustomer(activeCustomer.id)
    }

    val transactions by remember(activeCustomer.id) {
        viewModel.getTransactionsForCustomerFlow(activeCustomer.id)
    }.collectAsStateWithLifecycle(initialValue = initialTxs)

    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var isPdfExporting by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var txSearchQuery by remember { mutableStateOf("") }
    var showShareSheet by remember { mutableStateOf(false) }
    var dialogState by remember { mutableStateOf(CustomerHistoryDialogState()) }
    var selectedCurrencyFilter by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(showShareSheet, dialogState.showFilterMenu, isPdfExporting) {
        if (showShareSheet || dialogState.showFilterMenu || isPdfExporting) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    val allCustomerTxs = remember(transactions) {
        transactions.sortedBy { it.timestamp }
    }

    val displayedTxs by rememberFilteredCustomerTransactions(
        context = context,
        allCustomerTxs = allCustomerTxs,
        txSearchQuery = txSearchQuery,
        dateFilterMode = dialogState.dateFilterMode,
        customStartDate = dialogState.customStartDate,
        customEndDate = dialogState.customEndDate,
        typeFilterMode = dialogState.typeFilterMode,
        selectedCurrencyFilter = selectedCurrencyFilter,
        currencySymbol = currencySymbol,
        exchangeRatesJson = settings.exchangeRatesJson
    )

    val calcResult = remember(allCustomerTxs, currencySymbol, settings.exchangeRatesJson) {
        CustomerHistoryCalculator.calculate(allCustomerTxs, currencySymbol, settings.exchangeRatesJson)
    }

    var isTxMultiSelectActive by remember { mutableStateOf(false) }
    val selectedTxIds = remember { mutableStateListOf<String>() }

    LaunchedEffect(isTxMultiSelectActive) { onTxMultiSelectActiveChanged(isTxMultiSelectActive) }

    BackHandler {
        if (isTxMultiSelectActive) {
            isTxMultiSelectActive = false
            selectedTxIds.clear()
        } else if (selectedCurrencyFilter != null) {
            selectedCurrencyFilter = null
        } else if (isSearchActive || txSearchQuery.isNotEmpty()) {
            onSearchActiveChanged(false)
            txSearchQuery = ""
        } else {
            onDismiss()
        }
    }

    var refreshRecurringTrigger by remember { mutableStateOf(0) }
    val activeRecurringTxIds = remember(activeCustomer.id, refreshRecurringTrigger, allCustomerTxs) {
        val existingTxIds = allCustomerTxs.map { it.id }.toSet()
        HabayebRecurringManager.getAllConfigs(context)
            .filter { config ->
                config.isActive &&
                config.customerId == activeCustomer.id &&
                config.originalTxId.isNotBlank() &&
                !config.originalTxId.equals("null", ignoreCase = true) &&
                config.originalTxId != "0" &&
                existingTxIds.contains(config.originalTxId)
            }
            .map { it.originalTxId }
            .toSet()
    }

    LaunchedEffect(activeCustomer.id) {
        listState.scrollToItem(0)
        HabayebRecurringManager.checkAndExecuteRecurring(context, viewModel) { count ->
            Toast.makeText(context, context.getString(R.string.customer_history_toast_recurring_added, count, activeCustomer.name), Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(activeCustomer.id) {
        viewModel.uiEventFlow.collect { event ->
            when (event) {
                is com.example.ui.viewmodel.HabayebUiEvent.ScrollToAccount -> {
                    if (event.accountId == activeCustomer.id) {
                        listState.scrollToItem(0, 0)
                    }
                }
                is com.example.ui.viewmodel.HabayebUiEvent.ResetScrollToTop -> {
                    listState.scrollToItem(0, 0)
                }
            }
        }
    }

    val newestTxId = displayedTxs.firstOrNull()?.id
    var previousNewestTxId by remember(activeCustomer.id) { mutableStateOf(newestTxId) }
    var previousTxCount by remember(activeCustomer.id) { mutableStateOf(displayedTxs.size) }

    LaunchedEffect(newestTxId, displayedTxs.size) {
        if (displayedTxs.size > previousTxCount || (newestTxId != null && newestTxId != previousNewestTxId)) {
            listState.scrollToItem(0, 0)
        }
        previousTxCount = displayedTxs.size
        previousNewestTxId = newestTxId
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        CustomerHistoryTopBar(
                            customerName = activeCustomer.name,
                            customerPhone = activeCustomer.phone,
                            isSearchActive = isSearchActive,
                            txSearchQuery = txSearchQuery,
                            activeThemeColor = activeThemeColor,
                            isPdfExporting = isPdfExporting,
                            onSearchQueryChange = { txSearchQuery = it },
                            onSearchClose = {
                                onSearchActiveChanged(false)
                                txSearchQuery = ""
                            },
                            onSearchOpen = { onSearchActiveChanged(true) },
                            onDeleteClick = { dialogState = dialogState.copy(confirmDeleteCust = true) },
                            onEditClick = { dialogState = dialogState.copy(showEditNameDialog = true) },
                            onFilterClick = { dialogState = dialogState.copy(showFilterMenu = true) },
                            onShareClick = { showShareSheet = true },
                            onDismiss = onDismiss
                        )

                        if (!isSearchActive) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                            CustomerSummaryCard(
                                currencySymbol = currencySymbol,
                                netDebtMap = calcResult.netDebtMap,
                                netDebtBDMap = calcResult.netDebtBigDecimalMap,
                                initialType = activeCustomer.initialType,
                                selectedCurrencyFilter = selectedCurrencyFilter,
                                onCurrencyFilterSelected = { selectedCurrencyFilter = it }
                            )
                        }
                    }
                }

                CustomerHistoryTableSection(
                    displayedTxs = displayedTxs,
                    listState = listState,
                    txSearchQuery = txSearchQuery,
                    activeCustomer = activeCustomer,
                    isDark = isDark,
                    currencySymbol = currencySymbol,
                    runningBalances = calcResult.runningBalances,
                    activeRecurringTxIds = activeRecurringTxIds,
                    txSequenceNumbers = calcResult.txSequenceNumbers,
                    selectedTxIds = selectedTxIds,
                    isTxMultiSelectActive = isTxMultiSelectActive,
                    activeThemeColor = activeThemeColor,
                    contentPadding = contentPadding,
                    onSelectToggle = { txId ->
                        if (selectedTxIds.contains(txId)) selectedTxIds.remove(txId) else selectedTxIds.add(txId)
                        if (selectedTxIds.isEmpty()) isTxMultiSelectActive = false
                    },
                    onLongClick = { txId ->
                        if (!isTxMultiSelectActive) {
                            isTxMultiSelectActive = true
                            selectedTxIds.add(txId)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    onOptionsClick = { tx -> dialogState = dialogState.copy(transactionForOptionsDialog = tx) },
                    onScheduleClick = { tx -> dialogState = dialogState.copy(transactionForAutoRepeatDialog = tx) },
                    onExchangeRateClick = { tx ->
                        dialogState = dialogState.copy(exchangeTxToModify = tx, showRateModifyDialog = true)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }

            MultiSelectFloatingBar(
                isVisible = isTxMultiSelectActive,
                selectedTxIds = selectedTxIds,
                totalTxCount = displayedTxs.size,
                activeThemeColor = activeThemeColor,
                contentPadding = contentPadding,
                onCancel = {
                    isTxMultiSelectActive = false
                    selectedTxIds.clear()
                },
                onToggleSelectAll = {
                    val allSelected = displayedTxs.isNotEmpty() && selectedTxIds.size >= displayedTxs.size
                    if (allSelected) {
                        selectedTxIds.clear()
                    } else {
                        val set = selectedTxIds.toSet()
                        displayedTxs.forEach { if (!set.contains(it.id)) selectedTxIds.add(it.id) }
                    }
                },
                onDelete = {
                    if (selectedTxIds.isNotEmpty()) dialogState = dialogState.copy(showDeleteBulkTxConfirmDialog = true)
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    CustomerHistoryShareBottomSheet(
        showShareSheet = showShareSheet,
        activeCustomer = activeCustomer,
        allCustomerTxs = allCustomerTxs,
        currencySymbol = currencySymbol,
        exchangeRatesJson = settings.exchangeRatesJson,
        netDebt = calcResult.netDebt,
        activeThemeColor = activeThemeColor,
        onDismissRequest = { showShareSheet = false },
        onPdfExportStart = { isPdfExporting = true },
        onPdfExportFinish = { isPdfExporting = false }
    )

    CustomerHistoryDialogsManager(
        activeCustomer = activeCustomer,
        viewModel = viewModel,
        currencySymbol = currencySymbol,
        netDebt = calcResult.netDebt,
        activeThemeColor = activeThemeColor,
        activeSubColor = activeSubColor,
        dialogState = dialogState,
        onDialogStateChange = { transform -> dialogState = transform(dialogState) },
        onCustomerDeleted = onDismiss,
        selectedTxIds = selectedTxIds,
        onIsTxMultiSelectActiveChange = { isTxMultiSelectActive = it },
        activeRecurringTxIds = activeRecurringTxIds,
        txSequenceNumbers = calcResult.txSequenceNumbers,
        onRefreshRecurringTrigger = { refreshRecurringTrigger++ },
        allCustomerTxs = allCustomerTxs
    )
}
