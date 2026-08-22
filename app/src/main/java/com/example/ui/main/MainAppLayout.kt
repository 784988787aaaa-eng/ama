package com.example.ui.main

import androidx.compose.material3.MaterialTheme

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.entities.AppSettings
import com.example.ui.components.*
import com.example.ui.navigation.Screen
import com.example.ui.screens.ledger.components.DeviceActivationDialog
import com.example.ui.screens.BackupRestoreBottomSheet
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.HabayebFinanceViewModel
import com.example.ui.viewmodel.SecurityAndLicenseViewModel
import com.example.ui.viewmodel.BackupSyncViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppLayout(
    viewModel: FinanceViewModel,
    habayebViewModel: HabayebFinanceViewModel,
    securityViewModel: SecurityAndLicenseViewModel,
    backupSyncViewModel: BackupSyncViewModel,
    settings: AppSettings,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val versionName = remember(context) {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.2"
        } catch (e: Exception) {
            "1.2"
        }
    }
    val sdfName = remember { java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm", java.util.Locale.US) }
    val defaultStartDest by viewModel.defaultStartDestinationState.collectAsStateWithLifecycle()
    val isActivated by securityViewModel.isActivatedState.collectAsStateWithLifecycle()
    val deviceId by securityViewModel.deviceIdState.collectAsStateWithLifecycle()
    
    var showActivationDialog by remember { mutableStateOf(false) }
    var isActivationAutoTriggered by remember { mutableStateOf(false) }

    val showSecurityActivationRequired by securityViewModel.showActivationRequired.collectAsStateWithLifecycle()
    val showHabayebActivationRequired by habayebViewModel.showActivationRequired.collectAsStateWithLifecycle()
    val showBackupActivationRequired by backupSyncViewModel.showActivationRequired.collectAsStateWithLifecycle()

    LaunchedEffect(showSecurityActivationRequired) {
        if (showSecurityActivationRequired) {
            isActivationAutoTriggered = true
            showActivationDialog = true
            securityViewModel.showActivationRequired.value = false
        }
    }

    LaunchedEffect(showHabayebActivationRequired) {
        if (showHabayebActivationRequired) {
            isActivationAutoTriggered = true
            showActivationDialog = true
            habayebViewModel.resetActivationRequired()
        }
    }

    LaunchedEffect(showBackupActivationRequired) {
        if (showBackupActivationRequired) {
            isActivationAutoTriggered = true
            showActivationDialog = true
            backupSyncViewModel.showActivationRequired.value = false
        }
    }
    var showComprehensiveReportDialog by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableStateOf(Screen.HABAYEB) }
    var hasInitializedStartScreen by remember { mutableStateOf(false) }

    LaunchedEffect(defaultStartDest) {
        if (!hasInitializedStartScreen) {
            currentScreen = try {
                Screen.valueOf(defaultStartDest)
            } catch (e: Exception) {
                Screen.HABAYEB
            }
            hasInitializedStartScreen = true
        }
    }

    var showExitConfirmDialog by remember { mutableStateOf(false) }
    var showBackupRestoreSheet by remember { mutableStateOf(false) }
    var showCurrencyBallSelector by remember { mutableStateOf(false) }

    val floatingSearchPrefs = remember(context) { context.getSharedPreferences("floating_search_prefs", android.content.Context.MODE_PRIVATE) }
    var isFloatingSearchActive by remember {
        mutableStateOf(floatingSearchPrefs.getBoolean("KEY_FLOATING_SEARCH_ACTIVE", false))
    }
    var isSearchActive by remember { mutableStateOf(false) }
    var isHistoryOverlayActive by remember { mutableStateOf(false) }
    var isHistorySearchActive by remember { mutableStateOf(false) }
    var habayebFabOverlay by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }

    val activity = context as? android.app.Activity
    LaunchedEffect(activity) {
        val navigateTo = activity?.intent?.getStringExtra("navigate_to")
        if (navigateTo == "backup_settings") {
            showBackupRestoreSheet = true
            activity?.intent?.removeExtra("navigate_to")
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val safExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            backupSyncViewModel.getBackupJsonForClipboard { jsonStr ->
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                                outputStream.write(jsonStr.toByteArray())
                            }
                        }
                        Toast.makeText(context, context.getString(R.string.toast_backup_export_success), Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, context.getString(R.string.toast_backup_export_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val safRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val jsonText = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                    }
                    if (jsonText.isNotBlank()) {
                        backupSyncViewModel.executeMasterRestore(jsonText, context) { success, _ ->
                            if (success) {
                                Toast.makeText(context, context.getString(R.string.toast_restore_success), Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, context.getString(R.string.toast_restore_invalid_file), Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    BackHandler {
        val defaultStart = try {
            Screen.valueOf(defaultStartDest)
        } catch (e: Exception) {
            Screen.HABAYEB
        }
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else if (currentScreen != defaultStart) {
            currentScreen = defaultStart
        } else {
            if (settings.doubleCheckExit) {
                showExitConfirmDialog = true
            } else {
                onExit()
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentScreen == Screen.HABAYEB || currentScreen == Screen.LEDGER,
        drawerContent = {
            BackHandler(enabled = drawerState.isOpen) {
                scope.launch { drawerState.close() }
            }
            AppNavigationDrawer(
                currentScreen = currentScreen,
                onScreenSelected = { screen ->
                    currentScreen = screen
                    scope.launch { drawerState.close() }
                },
                onBackupClick = {
                    scope.launch { drawerState.close() }
                    showBackupRestoreSheet = true
                },
                isActivated = isActivated,
                onActivateProClick = {
                    scope.launch { drawerState.close() }
                    isActivationAutoTriggered = false
                    showActivationDialog = true
                },
                settings = settings,
                securityViewModel = securityViewModel,
                onSaveSettings = { updated, targetCurrency, newRate, revalueHistorical ->
                    viewModel.saveSettings(updated)
                    if (revalueHistorical && targetCurrency.isNotEmpty() && newRate > 0.0) {
                        habayebViewModel.revalueHistoricalTransactions(updated.currencySymbol, targetCurrency, java.math.BigDecimal.valueOf(newRate))
                    }
                },
                versionName = versionName,
                onComprehensiveReportClick = {
                    scope.launch { drawerState.close() }
                    showComprehensiveReportDialog = true
                }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                bottomBar = {
                    MainBottomNavigation(
                        currentScreen = currentScreen,
                        isVisible = currentScreen == Screen.HABAYEB || currentScreen == Screen.LEDGER,
                        onNavigate = { currentScreen = it }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    MainAppContent(
                        currentScreen = currentScreen,
                        viewModel = viewModel,
                        habayebViewModel = habayebViewModel,
                        securityViewModel = securityViewModel,
                        backupSyncViewModel = backupSyncViewModel,
                        settings = settings,
                        contentPadding = innerPadding,
                        onNavigate = { currentScreen = it },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        isDrawerOpen = drawerState.isOpen,
                        onExit = {
                            if (settings.doubleCheckExit) {
                                showExitConfirmDialog = true
                            } else {
                                onExit()
                            }
                        },
                        onHeaderDoubleClick = { showComprehensiveReportDialog = true },
                        isFloatingSearchActive = isFloatingSearchActive,
                        onFloatingSearchActiveChanged = {
                            isFloatingSearchActive = it
                            floatingSearchPrefs.edit().putBoolean("KEY_FLOATING_SEARCH_ACTIVE", it).apply()
                        },
                        isSearchActive = isSearchActive,
                        onSearchActiveChanged = { isSearchActive = it },
                        isHistoryOverlayActive = isHistoryOverlayActive,
                        onHistoryOverlayActiveChanged = { isHistoryOverlayActive = it },
                        isHistorySearchActive = isHistorySearchActive,
                        onHistorySearchActiveChanged = { isHistorySearchActive = it },
                        onFabOverlayChanged = { habayebFabOverlay = it },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (currentScreen == Screen.HABAYEB) {
                habayebFabOverlay?.invoke()
            }

            val hideBubble = if (currentScreen == Screen.HABAYEB) {
                if (isHistoryOverlayActive) isHistorySearchActive else isSearchActive
            } else {
                isSearchActive
            }
            if (isFloatingSearchActive && !hideBubble && (currentScreen == Screen.HABAYEB || currentScreen == Screen.LEDGER)) {
                com.example.ui.screens.habayeb.components.FloatingSearchBubble(
                    activeThemeColor = if (currentScreen == Screen.LEDGER) com.example.ui.theme.EmeraldPrimary else MaterialTheme.colorScheme.primary,
                    onSearchClick = {
                        if (currentScreen == Screen.HABAYEB) {
                            if (isHistoryOverlayActive) {
                                isHistorySearchActive = true
                            } else {
                                isSearchActive = true
                            }
                        } else if (currentScreen == Screen.LEDGER) {
                            isSearchActive = true
                        }
                    }
                )
            }
        }
    }

    ExitConfirmDialog(
        show = showExitConfirmDialog,
        onDismiss = { showExitConfirmDialog = false },
        onConfirm = { dontShowAgain ->
            if (dontShowAgain) {
                viewModel.saveSettings(settings.copy(doubleCheckExit = false))
            }
            showExitConfirmDialog = false
            onExit()
        }
    )

    if (showBackupRestoreSheet) {
        BackupRestoreBottomSheet(
            settings = settings,
            backupSyncViewModel = backupSyncViewModel,
            onExportMzd = {
                val dateStr = sdfName.format(java.util.Date())
                safExportLauncher.launch("Mizan_$dateStr.mzd")
            },
            onImportMzd = {
                safRestoreLauncher.launch(arrayOf("application/*"))
            },
            onDismiss = { showBackupRestoreSheet = false }
        )
    }

    if (showActivationDialog) {
        DeviceActivationDialog(
            deviceId = deviceId,
            viewModel = securityViewModel,
            backupSyncViewModel = backupSyncViewModel,
            onDismiss = { showActivationDialog = false },
            isAutoTriggered = isActivationAutoTriggered
        )
    }

    if (showComprehensiveReportDialog) {
        val habayebCustomersState by habayebViewModel.customersUiState.collectAsStateWithLifecycle()
        val selectedCustomerIds by habayebViewModel.selectedCustomerIdsState.collectAsStateWithLifecycle()
        com.example.ui.screens.habayeb.components.ComprehensiveReportDialog(
            customers = habayebCustomersState.customers,
            currencySymbol = settings.currencySymbol,
            activeThemeColor = MaterialTheme.colorScheme.primary,
            onDismiss = { showComprehensiveReportDialog = false },
            selectedCustomerIds = selectedCustomerIds
        )
    }
}
