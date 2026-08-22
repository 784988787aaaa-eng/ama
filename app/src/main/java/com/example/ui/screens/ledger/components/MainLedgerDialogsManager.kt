package com.example.ui.screens.ledger.components

import android.content.Context
import androidx.compose.runtime.Composable
import com.example.data.local.entities.FixedCommitment
import com.example.data.local.entities.TransactionDb
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.MonthLedger
import com.example.ui.viewmodel.SecurityAndLicenseViewModel
import kotlinx.coroutines.CoroutineScope
import java.math.BigDecimal

@Composable
fun MainLedgerDialogsManager(
    showTxDialog: Boolean,
    txDialogType: String,
    editingTransaction: TransactionDb?,
    currencySymbol: String,
    onDismissTxDialog: () -> Unit,
    onSaveTransaction: (id: String?, type: String, category: String, amount: Double, description: String) -> Unit,
    showSearch: Boolean,
    searchQuery: String,
    searchResults: List<TransactionDb>,
    onSearchQueryChange: (String) -> Unit,
    onDismissSearch: () -> Unit,
    showCommitmentsListSheet: Boolean,
    commitments: List<FixedCommitment>,
    computedCommitments: List<Triple<FixedCommitment, BigDecimal, BigDecimal>>,
    totalCash: BigDecimal,
    formatCurrency: (BigDecimal, String) -> String,
    formatDoubleCurrency: (Double, String) -> String,
    onDismissCommitmentsList: () -> Unit,
    onAddCommitmentClick: () -> Unit,
    onEditCommitmentClick: (FixedCommitment) -> Unit,
    onDeleteCommitment: (String) -> Unit,
    onReorderCommitment: (FixedCommitment, Int) -> Unit,
    onCommitmentCheckedChange: (FixedCommitment, Boolean) -> Unit,
    onSetReorderTarget: (FixedCommitment) -> Unit,
    showCommitmentDialog: Boolean,
    editingCommitment: FixedCommitment?,
    onDismissCommitmentDialog: () -> Unit,
    onSaveCommitment: (name: String, targetAmount: BigDecimal, currentProgress: BigDecimal) -> Unit,
    reorderCommitmentTarget: FixedCommitment?,
    onDismissReorderTarget: () -> Unit,
    onApplyReorderTarget: (FixedCommitment, Int) -> Unit,
    showActivationDialog: Boolean,
    deviceId: String,
    securityViewModel: SecurityAndLicenseViewModel,
    onDismissActivationDialog: () -> Unit,
    showDeleteDaysDialog: Boolean,
    onDismissDeleteDaysDialog: () -> Unit,
    monthlyLedger: List<MonthLedger>,
    selectedDayKeys: MutableList<String>,
    viewModel: FinanceViewModel,
    scope: CoroutineScope,
    context: Context,
    onSuccessDeleteDays: () -> Unit
) {
    if (showTxDialog) {
        TransactionRecordDialog(
            showTxDialog = showTxDialog,
            txDialogType = txDialogType,
            editingTransaction = editingTransaction,
            currencySymbol = currencySymbol,
            onDismiss = onDismissTxDialog,
            onSave = onSaveTransaction
        )
    }

    if (showSearch) {
        SearchLedgerDialog(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            results = searchResults,
            formatCurrency = { amt ->
                formatCurrency(BigDecimal.valueOf(amt), currencySymbol)
            },
            onDismiss = onDismissSearch
        )
    }

    if (showCommitmentsListSheet) {
        CommitmentsListDialog(
            showCommitmentsListSheet = showCommitmentsListSheet,
            commitments = commitments,
            computedCommitments = computedCommitments,
            totalCash = totalCash,
            currencySymbol = currencySymbol,
            formatCurrency = formatCurrency,
            onDismissRequest = onDismissCommitmentsList,
            onAddCommitmentClick = onAddCommitmentClick,
            onEditCommitmentClick = onEditCommitmentClick,
            onDeleteCommitment = onDeleteCommitment,
            onReorderCommitment = onReorderCommitment,
            onCheckedChange = onCommitmentCheckedChange,
            onSetReorderTarget = onSetReorderTarget
        )
    }

    if (showCommitmentDialog) {
        CommitmentEditDialog(
            showCommitmentDialog = showCommitmentDialog,
            editingCommitment = editingCommitment,
            onDismissRequest = onDismissCommitmentDialog,
            onSaveCommitment = onSaveCommitment,
            onDeleteCommitment = { name ->
                onDeleteCommitment(name)
                onDismissCommitmentDialog()
            }
        )
    }

    ReorderCommitmentDialog(
        reorderCommitmentTarget = reorderCommitmentTarget,
        commitmentsSize = commitments.size,
        onDismiss = onDismissReorderTarget,
        onApplyReorder = onApplyReorderTarget,
        context = context
    )

    if (showActivationDialog) {
        DeviceActivationDialog(
            deviceId = deviceId,
            viewModel = securityViewModel,
            onDismiss = onDismissActivationDialog,
            isAutoTriggered = true
        )
    }

    DeleteDaysConfirmDialog(
        showDeleteDaysDialog = showDeleteDaysDialog,
        onDismiss = onDismissDeleteDaysDialog,
        monthlyLedger = monthlyLedger,
        selectedDayKeys = selectedDayKeys,
        viewModel = viewModel,
        scope = scope,
        context = context,
        onSuccess = onSuccessDeleteDays
    )
}
