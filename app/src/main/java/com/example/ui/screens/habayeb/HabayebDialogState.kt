package com.example.ui.screens.habayeb

import com.example.data.local.entities.HabayebCustomer
import com.example.data.local.entities.HabayebTransaction
import com.example.domain.model.TransactionType
import com.example.ui.state.CustomerUiState

/**
 * Sealed hierarchy of all dialog and bottom-sheet states managed within the Habayeb subsystem.
 */
sealed interface HabayebDialogState {
    object None : HabayebDialogState
    object AddCustomer : HabayebDialogState
    data class AddTransaction(
        val customer: HabayebCustomer,
        val defaultType: String = TransactionType.OWED_BY_THEM.value,
        val editingTx: HabayebTransaction? = null
    ) : HabayebDialogState
    data class EditCustomer(val customer: HabayebCustomer) : HabayebDialogState
    object DeleteConfirm : HabayebDialogState
    object AddCategory : HabayebDialogState
    object BulkAssignCategory : HabayebDialogState
    data class ContextMenu(val customer: CustomerUiState) : HabayebDialogState
    object DeviceActivation : HabayebDialogState
}
