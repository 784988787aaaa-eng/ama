package com.example.ui.screens.settings.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.example.data.local.entities.AppSettings
import com.example.ui.screens.SettingsDialogState
import com.example.ui.screens.habayeb.components.ExchangeRateSetupDialog
import com.example.ui.screens.habayeb.utils.ExchangeRateHelper
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.HabayebFinanceViewModel

@Composable
fun SettingsDialogHost(
    activeDialogState: SettingsDialogState,
    onDismissDialog: () -> Unit,
    onStateChange: (SettingsDialogState) -> Unit,
    settings: AppSettings,
    currencySymbol: String,
    currenciesToSetup: List<String>,
    currentSetupIndex: Int,
    onSetupIndexChange: (Int) -> Unit,
    onCurrenciesToSetupChange: (List<String>) -> Unit,
    viewModel: FinanceViewModel,
    habayebViewModel: HabayebFinanceViewModel,
    onLaunchPermissions: () -> Unit,
    onPermissionGrantedCallback: (() -> Unit)?
) {
    if (activeDialogState is SettingsDialogState.PermissionExplanation) {
        BackupPermissionExplanationDialog(
            onDismiss = onDismissDialog,
            onGrantPermissions = onLaunchPermissions,
            onUseInternalStorage = {
                onPermissionGrantedCallback?.invoke()
            }
        )
    }

    if (activeDialogState is SettingsDialogState.ResetDataTrap) {
        ResetTrapDialog(
            onDismiss = onDismissDialog,
            onConfirmDelete = {
                viewModel.deleteAllData()
                onDismissDialog()
            }
        )
    }

    if (activeDialogState is SettingsDialogState.CurrencySetup && currentSetupIndex < currenciesToSetup.size) {
        val targetCurrency = currenciesToSetup[currentSetupIndex]
        ExchangeRateSetupDialog(
            selectedCurrency = targetCurrency,
            initialRateStr = "",
            activeThemeColor = MaterialTheme.colorScheme.primary,
            onDismiss = {
                if (currentSetupIndex + 1 < currenciesToSetup.size) {
                    onSetupIndexChange(currentSetupIndex + 1)
                } else {
                    onDismissDialog()
                    onCurrenciesToSetupChange(emptyList())
                }
            },
            onConfirm = { newRate ->
                val migratedOriginalJson = ExchangeRateHelper.migrateRates(
                    settings.exchangeRatesJson,
                    settings.currencySymbol,
                    currencySymbol
                )
                val alreadyHasRate = ExchangeRateHelper.hasRate(
                    migratedOriginalJson,
                    currencySymbol,
                    targetCurrency
                )
                val existingRate = ExchangeRateHelper.getRate(
                    migratedOriginalJson,
                    currencySymbol,
                    targetCurrency
                )
                val oldRateBD = java.math.BigDecimal.valueOf(existingRate)
                val newRateBD = java.math.BigDecimal.valueOf(newRate)
                val rateChanged = existingRate > 0.0 && oldRateBD.compareTo(newRateBD) != 0

                if (alreadyHasRate && rateChanged) {
                    onStateChange(SettingsDialogState.RevalueConfirm(targetCurrency, newRate))
                } else {
                    val currentSettings = settings
                    val updatedSettings = currentSettings.copy(
                        exchangeRatesJson = ExchangeRateHelper.setRate(
                            currentSettings.exchangeRatesJson,
                            currencySymbol,
                            targetCurrency,
                            newRate
                        )
                    )
                    viewModel.saveSettings(updatedSettings)

                    if (currentSetupIndex + 1 < currenciesToSetup.size) {
                        onSetupIndexChange(currentSetupIndex + 1)
                    } else {
                        onDismissDialog()
                        onCurrenciesToSetupChange(emptyList())
                    }
                }
            }
        )
    }

    val revalueState = activeDialogState as? SettingsDialogState.RevalueConfirm
    if (revalueState != null) {
        val targetCurrency = revalueState.targetCurrency
        val newRate = revalueState.newRate

        RevalueConfirmDialog(
            targetCurrency = targetCurrency,
            onConfirmAll = {
                habayebViewModel.revalueHistoricalTransactions(currencySymbol, targetCurrency, java.math.BigDecimal.valueOf(newRate))
                val currentSettings = settings
                val updatedSettings = currentSettings.copy(
                    exchangeRatesJson = ExchangeRateHelper.setRate(
                        currentSettings.exchangeRatesJson,
                        currencySymbol,
                        targetCurrency,
                        newRate
                    )
                )
                viewModel.saveSettings(updatedSettings)

                if (currentSetupIndex + 1 < currenciesToSetup.size) {
                    onSetupIndexChange(currentSetupIndex + 1)
                    onStateChange(SettingsDialogState.CurrencySetup)
                } else {
                    onDismissDialog()
                    onCurrenciesToSetupChange(emptyList())
                }
            },
            onConfirmFutureOnly = {
                val currentSettings = settings
                val updatedSettings = currentSettings.copy(
                    exchangeRatesJson = ExchangeRateHelper.setRate(
                        currentSettings.exchangeRatesJson,
                        currencySymbol,
                        targetCurrency,
                        newRate
                    )
                )
                viewModel.saveSettings(updatedSettings)

                if (currentSetupIndex + 1 < currenciesToSetup.size) {
                    onSetupIndexChange(currentSetupIndex + 1)
                    onStateChange(SettingsDialogState.CurrencySetup)
                } else {
                    onDismissDialog()
                    onCurrenciesToSetupChange(emptyList())
                }
            },
            onDismiss = onDismissDialog
        )
    }
}
