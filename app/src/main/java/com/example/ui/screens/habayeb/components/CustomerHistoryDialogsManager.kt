package com.example.ui.screens.habayeb.components

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.R
import com.example.data.local.entities.HabayebCustomer
import com.example.data.local.entities.HabayebTransaction
import com.example.ui.screens.habayeb.utils.CustomerShareHelper
import com.example.ui.screens.habayeb.utils.ExchangeRateHelper
import com.example.ui.screens.habayeb.utils.HabayebRecurringManager
import com.example.ui.viewmodel.HabayebFinanceViewModel
import java.math.BigDecimal

data class CustomerHistoryDialogState(
    val confirmDeleteCust: Boolean = false,
    val showEditNameDialog: Boolean = false,
    val editingTransactionForDialog: HabayebTransaction? = null,
    val showAddTransactionDialogFromHistory: HabayebCustomer? = null,
    val defaultTransactionTypeFromHistory: String = "OWED_BY_THEM",
    val transactionForOptionsDialog: HabayebTransaction? = null,
    val transactionForAutoRepeatDialog: HabayebTransaction? = null,
    val showDeleteBulkTxConfirmDialog: Boolean = false,
    val showFilterMenu: Boolean = false,
    val dateFilterMode: Int = 0,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val typeFilterMode: Int = 0,
    val showRateModifyDialog: Boolean = false,
    val exchangeTxToModify: HabayebTransaction? = null
)

@Composable
fun CustomerHistoryDialogsManager(
    activeCustomer: HabayebCustomer,
    viewModel: HabayebFinanceViewModel,
    currencySymbol: String,
    netDebt: Double,
    activeThemeColor: Color,
    activeSubColor: Color,
    dialogState: CustomerHistoryDialogState,
    onDialogStateChange: ((CustomerHistoryDialogState) -> CustomerHistoryDialogState) -> Unit,
    onCustomerDeleted: () -> Unit,
    selectedTxIds: SnapshotStateList<String>,
    onIsTxMultiSelectActiveChange: (Boolean) -> Unit,
    activeRecurringTxIds: Set<String>,
    txSequenceNumbers: Map<String, Int>,
    onRefreshRecurringTrigger: () -> Unit,
    allCustomerTxs: List<HabayebTransaction> = emptyList()
) {
    val context = LocalContext.current

    fun updateState(transform: (CustomerHistoryDialogState) -> CustomerHistoryDialogState) {
        onDialogStateChange(transform)
    }

    if (dialogState.confirmDeleteCust) {
        CustomerDeleteConfirmationDialog(
            customer = activeCustomer,
            onConfirm = {
                viewModel.deleteHabayebCustomer(activeCustomer.id)
                Toast.makeText(context, context.getString(R.string.habayeb_toast_delete_success), Toast.LENGTH_SHORT).show()
                updateState { it.copy(confirmDeleteCust = false) }
                onCustomerDeleted()
            },
            onDismiss = { updateState { it.copy(confirmDeleteCust = false) } }
        )
    }

    if (dialogState.showEditNameDialog) {
        CustomerEditDialog(
            customer = activeCustomer,
            activeThemeColor = activeThemeColor,
            onConfirm = { newName, newPhone ->
                if (newName.isNotBlank()) {
                    viewModel.updateHabayebCustomer(
                        activeCustomer.copy(
                            name = newName.trim(),
                            phone = newPhone.trim()
                        )
                    )
                    Toast.makeText(context, context.getString(R.string.habayeb_toast_update_success), Toast.LENGTH_SHORT).show()
                }
                updateState { it.copy(showEditNameDialog = false) }
            },
            onDismiss = { updateState { it.copy(showEditNameDialog = false) } }
        )
    }

    if (dialogState.showAddTransactionDialogFromHistory != null) {
        AddTransactionPopup(
            customer = dialogState.showAddTransactionDialogFromHistory,
            viewModel = viewModel,
            initialSelectedType = dialogState.defaultTransactionTypeFromHistory,
            editingTransaction = dialogState.editingTransactionForDialog,
            onDismiss = {
                updateState { it.copy(showAddTransactionDialogFromHistory = null, editingTransactionForDialog = null) }
            },
            activeThemeColor = activeThemeColor,
            activeSubColor = activeSubColor
        )
    }

    if (dialogState.transactionForOptionsDialog != null) {
        val optTx = dialogState.transactionForOptionsDialog
        val isRecurringOriginal = optTx.id in activeRecurringTxIds
        val parentSeq = if (!optTx.linkedMainTxId.isNullOrBlank() && !optTx.linkedMainTxId.equals("null", ignoreCase = true) && optTx.linkedMainTxId != "0" && optTx.linkedMainTxId != optTx.id) {
            txSequenceNumbers[optTx.linkedMainTxId]
        } else null

        val onWhatsAppShare = remember(optTx, activeCustomer, netDebt, currencySymbol, allCustomerTxs) {
            { CustomerShareHelper.triggerSingleTxWhatsApp(context, optTx, activeCustomer, netDebt, currencySymbol, allCustomerTxs) }
        }
        val onSmsShare = remember(optTx, activeCustomer, netDebt, currencySymbol, allCustomerTxs) {
            { CustomerShareHelper.triggerSingleTxSms(context, optTx, activeCustomer, netDebt, currencySymbol, allCustomerTxs) }
        }
        val onDeleteAutoRepeat = remember(optTx) {
            {
                val txId = optTx.id
                HabayebRecurringManager.deleteConfigForTransaction(context, txId)
                Toast.makeText(context, context.getString(R.string.habayeb_toast_stop_recurring_success), Toast.LENGTH_SHORT).show()
                onRefreshRecurringTrigger()
                updateState { it.copy(transactionForOptionsDialog = null) }
            }
        }

        TransactionOptionsDialog(
            transaction = optTx,
            customerName = activeCustomer.name,
            onDismiss = { updateState { it.copy(transactionForOptionsDialog = null) } },
            onEdit = {
                updateState {
                    it.copy(
                        editingTransactionForDialog = optTx,
                        defaultTransactionTypeFromHistory = optTx.type,
                        showAddTransactionDialogFromHistory = activeCustomer,
                        transactionForOptionsDialog = null
                    )
                }
            },
            onDelete = {
                val txId = optTx.id
                viewModel.deleteHabayebTransaction(txId)
                HabayebRecurringManager.deleteConfigForTransaction(context, txId)
                Toast.makeText(context, context.getString(R.string.habayeb_toast_delete_tx_success), Toast.LENGTH_SHORT).show()
                onRefreshRecurringTrigger()
                updateState { it.copy(transactionForOptionsDialog = null) }
            },
            onAutoRepeat = {
                updateState { it.copy(transactionForAutoRepeatDialog = optTx, transactionForOptionsDialog = null) }
            },
            onWhatsAppShare = onWhatsAppShare,
            onSmsShare = onSmsShare,
            activeThemeColor = activeThemeColor,
            activeSubColor = activeSubColor,
            isRecurringOriginal = isRecurringOriginal,
            onDeleteAutoRepeat = onDeleteAutoRepeat,
            parentSeqNumber = parentSeq
        )
    }

    if (dialogState.transactionForAutoRepeatDialog != null) {
        RecurringTransactionPopup(
            transaction = dialogState.transactionForAutoRepeatDialog,
            customerName = activeCustomer.name,
            onDismiss = {
                updateState { it.copy(transactionForAutoRepeatDialog = null) }
                onRefreshRecurringTrigger()
            },
            activeThemeColor = activeThemeColor,
            activeSubColor = activeSubColor
        )
    }

    DeleteBulkTxConfirmDialog(
        show = dialogState.showDeleteBulkTxConfirmDialog,
        selectedCount = selectedTxIds.size,
        onDismiss = { updateState { it.copy(showDeleteBulkTxConfirmDialog = false) } },
        onConfirm = {
            val idsToDelete = selectedTxIds.toList()
            viewModel.deleteMultipleHabayebTransactions(idsToDelete)
            idsToDelete.forEach { txId ->
                HabayebRecurringManager.deleteConfigForTransaction(context, txId)
            }
            Toast.makeText(context, context.getString(R.string.habayeb_toast_delete_bulk_success), Toast.LENGTH_SHORT).show()
            selectedTxIds.clear()
            onIsTxMultiSelectActiveChange(false)
            onRefreshRecurringTrigger()
            updateState { it.copy(showDeleteBulkTxConfirmDialog = false) }
        }
    )

    if (dialogState.showFilterMenu) {
        CustomerHistoryFilterSheet(
            dateFilterMode = dialogState.dateFilterMode,
            onDateFilterModeChange = { mode -> updateState { it.copy(dateFilterMode = mode) } },
            customStartDate = dialogState.customStartDate,
            onCustomStartDateChange = { start -> updateState { it.copy(customStartDate = start) } },
            customEndDate = dialogState.customEndDate,
            onCustomEndDateChange = { end -> updateState { it.copy(customEndDate = end) } },
            typeFilterMode = dialogState.typeFilterMode,
            onTypeFilterModeChange = { mode -> updateState { it.copy(typeFilterMode = mode) } },
            activeThemeColor = activeThemeColor,
            onDismissRequest = { updateState { it.copy(showFilterMenu = false) } }
        )
    }

    if (dialogState.showRateModifyDialog && dialogState.exchangeTxToModify != null) {
        val txToModify = dialogState.exchangeTxToModify
        ExchangeRateModifyDialog(
            show = dialogState.showRateModifyDialog,
            tx = txToModify,
            currencySymbol = currencySymbol,
            activeThemeColor = activeThemeColor,
            onDismissRequest = {
                updateState { it.copy(showRateModifyDialog = false, exchangeTxToModify = null) }
            },
            onConfirmRateSetup = { targetCurrency, newRate ->
                val settings = viewModel.settingsState.value
                val targetCurr = currencySymbol
                val newSettings = settings.copy(
                    exchangeRatesJson = ExchangeRateHelper.setRate(settings.exchangeRatesJson, targetCurr, targetCurrency, newRate)
                )
                viewModel.saveSettings(newSettings)
                val rateBigDecimal = BigDecimal.valueOf(newRate)
                viewModel.updateTransactionExchangeRate(txToModify.id, rateBigDecimal, true)
                updateState { it.copy(showRateModifyDialog = false, exchangeTxToModify = null) }
            },
            onDeactivateExchange = {
                viewModel.updateTransactionExchangeRate(txToModify.id, txToModify.exchangeRate, false)
                updateState { it.copy(showRateModifyDialog = false, exchangeTxToModify = null) }
            },
            hasStoredRateForCurrency = { curr ->
                ExchangeRateHelper.hasRate(viewModel.settingsState.value.exchangeRatesJson, currencySymbol, curr)
            },
            getStoredRateForCurrency = { curr ->
                ExchangeRateHelper.getRate(viewModel.settingsState.value.exchangeRatesJson, currencySymbol, curr)
            }
        )
    }
}
