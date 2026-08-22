package com.example.ui.screens.habayeb

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entities.CustomCategory
import com.example.data.local.entities.HabayebCustomer
import com.example.ui.screens.habayeb.components.AddCustomerPopup
import com.example.ui.screens.habayeb.components.AddTransactionPopup
import com.example.ui.screens.habayeb.components.CustomerContextBottomSheet
import com.example.ui.screens.habayeb.components.DeleteConfirmDialog
import com.example.ui.screens.habayeb.components.EditCustomerDialog
import com.example.ui.screens.habayeb.components.HabayebBulkAssignDialog
import com.example.ui.screens.habayeb.components.MicroAddCategoryDialog
import com.example.ui.screens.ledger.components.DeviceActivationDialog
import com.example.ui.viewmodel.HabayebFinanceViewModel
import com.example.ui.viewmodel.SecurityAndLicenseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Coordinates all dialogs, bottom sheets, and modal flows for the Habayeb Screen.
 * Isolates dialog state handling from the main screen layout tree.
 */
@Composable
fun HabayebDialogHost(
    activeDialogState: HabayebDialogState,
    viewModel: HabayebFinanceViewModel,
    securityViewModel: SecurityAndLicenseViewModel,
    activeThemeColor: Color,
    activeSubColor: Color,
    selectedCategory: String?,
    customCategories: List<CustomCategory>,
    selectedCustomerIds: MutableList<String>,
    onMultiSelectActiveChanged: (Boolean) -> Unit,
    listState: LazyListState,
    coroutineScope: CoroutineScope,
    isSearchActive: Boolean,
    onSearchActiveChanged: (Boolean) -> Unit,
    onJumpToAccount: (String) -> Unit,
    onDismissDialog: () -> Unit,
    onOpenEditCustomer: (HabayebCustomer) -> Unit,
    onOpenDeleteConfirm: () -> Unit
) {
    when (val dialogState = activeDialogState) {
        is HabayebDialogState.None -> {}
        is HabayebDialogState.AddCustomer -> {
            AddCustomerPopup(
                viewModel = viewModel,
                onDismiss = onDismissDialog,
                onCustomerAdded = { newCustomerId ->
                    onJumpToAccount(newCustomerId)
                    onDismissDialog()
                },
                activeThemeColor = activeThemeColor,
                activeSubColor = activeSubColor
            )
        }
        is HabayebDialogState.AddTransaction -> {
            AddTransactionPopup(
                customer = dialogState.customer,
                viewModel = viewModel,
                initialSelectedType = dialogState.defaultType,
                editingTransaction = dialogState.editingTx,
                onDismiss = onDismissDialog,
                onTransactionSaved = {
                    onJumpToAccount(dialogState.customer.id)
                    onDismissDialog()
                },
                activeThemeColor = activeThemeColor,
                activeSubColor = activeSubColor
            )
        }
        is HabayebDialogState.EditCustomer -> {
            EditCustomerDialog(
                customer = dialogState.customer,
                viewModel = viewModel,
                activeThemeColor = activeThemeColor,
                onDismiss = onDismissDialog,
                onCustomerUpdated = {
                    onJumpToAccount(dialogState.customer.id)
                }
            )
        }
        is HabayebDialogState.DeleteConfirm -> {
            DeleteConfirmDialog(
                selectedCustomerIds = selectedCustomerIds.toList(),
                viewModel = viewModel,
                onDismiss = onDismissDialog,
                onSuccessBulkDelete = {
                    selectedCustomerIds.clear()
                    onMultiSelectActiveChanged(false)
                    onDismissDialog()
                }
            )
        }
        is HabayebDialogState.AddCategory -> {
            MicroAddCategoryDialog(
                activeThemeColor = activeThemeColor,
                onDismiss = onDismissDialog,
                onSave = { categoryName ->
                    viewModel.saveCustomCategory(categoryName)
                    onDismissDialog()
                }
            )
        }
        is HabayebDialogState.BulkAssignCategory -> {
            HabayebBulkAssignDialog(
                customCategories = customCategories,
                onDismiss = onDismissDialog,
                onAssign = { categoryName ->
                    viewModel.assignCategoryToCustomers(selectedCustomerIds.toList(), categoryName)
                    onMultiSelectActiveChanged(false)
                    selectedCustomerIds.clear()
                    onDismissDialog()
                }
            )
        }
        is HabayebDialogState.ContextMenu -> {
            val pinnedSet by viewModel.pinnedCustomerIds.collectAsStateWithLifecycle()
            val isPinned = pinnedSet.contains(dialogState.customer.id)

            CustomerContextBottomSheet(
                customer = dialogState.customer,
                customCategories = customCategories,
                isPinned = isPinned,
                activeThemeColor = activeThemeColor,
                onDismiss = onDismissDialog,
                onTogglePin = {
                    val targetId = dialogState.customer.id
                    val wasPinned = isPinned
                    viewModel.togglePinCustomer(targetId)
                    if (wasPinned) {
                        coroutineScope.launch {
                            listState.scrollToItem(0, 0)
                        }
                    }
                },
                onAssignCategory = { category ->
                    viewModel.assignCategoryToCustomers(listOf(dialogState.customer.id), category)
                },
                onEnableMultiSelect = {
                    onMultiSelectActiveChanged(true)
                    selectedCustomerIds.add(dialogState.customer.id)
                    onDismissDialog()
                },
                onDelete = {
                    selectedCustomerIds.clear()
                    selectedCustomerIds.add(dialogState.customer.id)
                    onOpenDeleteConfirm()
                },
                onEditClick = {
                    onOpenEditCustomer(dialogState.customer.originalCustomer)
                },
                onUpdateCustomerType = { newType ->
                    viewModel.updateHabayebCustomer(dialogState.customer.originalCustomer.copy(initialType = newType))
                },
                currentActiveCategory = selectedCategory
            )
        }
        is HabayebDialogState.DeviceActivation -> {
            val deviceId by securityViewModel.deviceIdState.collectAsStateWithLifecycle()
            DeviceActivationDialog(
                deviceId = deviceId,
                viewModel = securityViewModel,
                onDismiss = onDismissDialog,
                isAutoTriggered = true
            )
        }
    }
}
