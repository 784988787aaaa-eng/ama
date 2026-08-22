package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.R
import com.example.data.local.entities.AppSettings
import com.example.ui.screens.habayeb.utils.ExchangeRateHelper
import com.example.ui.screens.settings.components.GeneralSettingsCard
import com.example.ui.screens.settings.components.QuadBackupCard
import com.example.ui.screens.settings.components.SettingsAutoBackupCard
import com.example.ui.screens.settings.components.SettingsDangerZoneCard
import com.example.ui.screens.settings.components.SettingsDeveloperFooter
import com.example.ui.screens.settings.components.SettingsDialogHost
import com.example.ui.screens.settings.components.SettingsHeaderCard
import com.example.ui.screens.settings.components.SettingsSecurityCard
import com.example.ui.screens.settings.components.SignatureCard
import com.example.ui.viewmodel.BackupSyncViewModel
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.HabayebFinanceViewModel

sealed interface SettingsDialogState {
    object None : SettingsDialogState
    object PermissionExplanation : SettingsDialogState
    object ResetDataTrap : SettingsDialogState
    object CurrencySetup : SettingsDialogState
    data class RevalueConfirm(val targetCurrency: String, val newRate: Double) : SettingsDialogState
}

@Composable
fun SettingsView(
    viewModel: FinanceViewModel,
    habayebViewModel: HabayebFinanceViewModel,
    backupSyncViewModel: BackupSyncViewModel,
    settings: AppSettings,
    onNavigateToSecurity: () -> Unit,
    contentPadding: PaddingValues = PaddingValues()
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDark = remember(settings.themeMode, systemDark) {
        when (settings.themeMode) {
            1 -> false
            2 -> true
            else -> systemDark
        }
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        LaunchedEffect(isDark) {
            val window = (context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    var activeDialogState by remember { mutableStateOf<SettingsDialogState>(SettingsDialogState.None) }
    var onPermissionGrantedCallback by remember { mutableStateOf<(() -> Unit)?>(null) }

    var currencySymbol by remember { mutableStateOf(settings.currencySymbol) }
    var currenciesToSetup by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentSetupIndex by remember { mutableStateOf(0) }
    var schoolExpenses by remember { mutableStateOf(settings.schoolExpensesEnabled) }
    var isAutoBackupEnabled by remember { mutableStateOf(settings.isAutoBackupEnabled) }

    LaunchedEffect(settings) {
        currencySymbol = settings.currencySymbol
        schoolExpenses = settings.schoolExpensesEnabled
        isAutoBackupEnabled = settings.isAutoBackupEnabled
    }

    val coroutineScope = rememberCoroutineScope()

    val checkBackupPermissionsGranted = remember(context) {
        {
            val hasWrite = if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true

            val hasRead = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED

            val hasNotification = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true

            val hasManage = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                android.os.Environment.isExternalStorageManager()
            } else true

            hasWrite && hasRead && hasNotification && hasManage
        }
    }

    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val writeGranted = if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
            results[android.Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: false
        } else true

        val readGranted = results[android.Manifest.permission.READ_EXTERNAL_STORAGE] ?: false

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (!android.os.Environment.isExternalStorageManager()) {
                Toast.makeText(context, context.getString(R.string.settings_toast_permission_manage_files), Toast.LENGTH_LONG).show()
                try {
                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    context.startActivity(intent)
                }
            } else {
                onPermissionGrantedCallback?.invoke()
            }
        } else {
            if (writeGranted && readGranted) {
                onPermissionGrantedCallback?.invoke()
            } else {
                Toast.makeText(context, context.getString(R.string.settings_toast_permission_denied_err), Toast.LENGTH_LONG).show()
            }
        }
    }

    val saveAllSettings = remember(settings, currencySymbol, schoolExpenses, isAutoBackupEnabled) {
        {
            var finalJson = settings.exchangeRatesJson
            if (settings.currencySymbol != currencySymbol) {
                finalJson = ExchangeRateHelper.migrateRates(
                    settings.exchangeRatesJson,
                    settings.currencySymbol,
                    currencySymbol
                )
            }
            val updated = settings.copy(
                currencySymbol = currencySymbol,
                schoolExpensesEnabled = schoolExpenses,
                isAutoBackupEnabled = isAutoBackupEnabled,
                exchangeRatesJson = finalJson
            )
            viewModel.saveSettings(updated)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 40.dp)
    ) {
        item(key = "settings_header_card") {
            SettingsHeaderCard()
        }

        item(key = "general_preferences_card") {
            GeneralSettingsCard(
                currencySymbol = currencySymbol,
                onCurrencySymbolChange = { newSymbol ->
                    val oldSymbol = settings.currencySymbol
                    currencySymbol = newSymbol

                    val migratedJson = if (oldSymbol != newSymbol) {
                        ExchangeRateHelper.migrateRates(
                            settings.exchangeRatesJson,
                            oldSymbol,
                            newSymbol
                        )
                    } else {
                        settings.exchangeRatesJson
                    }

                    val updated = settings.copy(
                        currencySymbol = newSymbol,
                        schoolExpensesEnabled = schoolExpenses,
                        isAutoBackupEnabled = isAutoBackupEnabled,
                        exchangeRatesJson = migratedJson
                    )
                    viewModel.saveSettings(updated)

                    val otherCurrencies = listOf(
                        context.getString(R.string.currency_yer),
                        context.getString(R.string.currency_usd),
                        context.getString(R.string.currency_sar)
                    ).filter { it != newSymbol }
                    val missingRates = otherCurrencies.filter { other ->
                        !ExchangeRateHelper.hasRate(migratedJson, newSymbol, other)
                    }
                    if (missingRates.isNotEmpty()) {
                        currenciesToSetup = missingRates
                        currentSetupIndex = 0
                        activeDialogState = SettingsDialogState.CurrencySetup
                    }
                }
            )
        }

        item(key = "business_signature_card") {
            SignatureCard()
        }

        item(key = "security_portal_button") {
            SettingsSecurityCard(onNavigateToSecurity = onNavigateToSecurity)
        }

        item(key = "quad_backup_central_card") {
            QuadBackupCard(
                backupSyncViewModel = backupSyncViewModel,
                settings = settings,
                onRestoreSuccess = { restoredSettings ->
                    currencySymbol = restoredSettings.currencySymbol
                    schoolExpenses = restoredSettings.schoolExpensesEnabled
                }
            )
        }

        item(key = "auto_backup_schedule_card") {
            SettingsAutoBackupCard(
                isAutoBackupEnabled = isAutoBackupEnabled,
                onCheckedChange = { checked ->
                    if (checked) {
                        val enableAutoBackup = {
                            isAutoBackupEnabled = true
                            saveAllSettings()
                            com.example.AutoBackupWorker.scheduleDailyBackupWorker(context)
                            Toast.makeText(context, context.getString(R.string.settings_toast_auto_backup_enabled), Toast.LENGTH_SHORT).show()
                        }
                        if (checkBackupPermissionsGranted()) {
                            enableAutoBackup()
                        } else {
                            onPermissionGrantedCallback = enableAutoBackup
                            activeDialogState = SettingsDialogState.PermissionExplanation
                        }
                    } else {
                        isAutoBackupEnabled = false
                        saveAllSettings()
                        com.example.AutoBackupWorker.cancelDailyBackupWorker(context)
                        Toast.makeText(context, context.getString(R.string.settings_toast_auto_backup_disabled), Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        item(key = "danger_zone_wipe_card") {
            SettingsDangerZoneCard(
                onTriggerResetTrap = {
                    activeDialogState = SettingsDialogState.ResetDataTrap
                }
            )
        }

        item(key = "developer_seal_footer_item") {
            SettingsDeveloperFooter(context = context)
        }
    }

    SettingsDialogHost(
        activeDialogState = activeDialogState,
        onDismissDialog = { activeDialogState = SettingsDialogState.None },
        onStateChange = { activeDialogState = it },
        settings = settings,
        currencySymbol = currencySymbol,
        currenciesToSetup = currenciesToSetup,
        currentSetupIndex = currentSetupIndex,
        onSetupIndexChange = { currentSetupIndex = it },
        onCurrenciesToSetupChange = { currenciesToSetup = it },
        viewModel = viewModel,
        habayebViewModel = habayebViewModel,
        onLaunchPermissions = {
            val permissions = mutableListOf<String>()
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
                permissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
            multiplePermissionsLauncher.launch(permissions.toTypedArray())
        },
        onPermissionGrantedCallback = onPermissionGrantedCallback
    )
}
