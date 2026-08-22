package com.example

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.WelcomeOnboardingDialog
import android.content.pm.PackageManager
import java.security.MessageDigest
import com.example.ui.main.MainAppLayout
import com.example.ui.screens.AppLockScreen
import com.example.ui.theme.AppTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.HabayebFinanceViewModel
import com.example.ui.viewmodel.BackupSyncViewModel
import com.example.ui.screens.habayeb.utils.HabayebRecurringManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class MainActivity : FragmentActivity() {
    private lateinit var backupSyncViewModel: BackupSyncViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val viewModel = androidx.lifecycle.ViewModelProvider(this)[FinanceViewModel::class.java]
        val securityViewModel = androidx.lifecycle.ViewModelProvider(this)[com.example.ui.viewmodel.SecurityAndLicenseViewModel::class.java]
        backupSyncViewModel = androidx.lifecycle.ViewModelProvider(this)[BackupSyncViewModel::class.java]

        splashScreen.setKeepOnScreenCondition {
            !viewModel.isSettingsLoaded.value
        }

        lifecycleScope.launch(Dispatchers.IO) {
            logAppSignatureSHA1(this@MainActivity)
            AutoBackupWorker.scheduleDailyBackupWorker(this@MainActivity)
            AutoBackupWorker.checkAndTriggerBackupIfMissed(this@MainActivity)
            BackupReminderWorker.scheduleReminder(this@MainActivity)
        }

        val secPrefs = getSharedPreferences("mizan_sec_prefs", MODE_PRIVATE)
        val isPasscodeEnabledFast = secPrefs.getBoolean("fast_passcode_enabled", false)

        val sharedPrefs = getSharedPreferences("fast_theme_prefs", MODE_PRIVATE)
        val cachedThemeMode = sharedPrefs.getInt("key_fast_theme_mode", 0) // 0: System, 1: Light, 2: Dark

        setContent {
            val habayebViewModel: HabayebFinanceViewModel = viewModel()

            val context = LocalContext.current

            LaunchedEffect(securityViewModel) {
                securityViewModel.startRealtimeMonitoring(this@MainActivity)
                securityViewModel.kickoutEvent.collect { reason ->
                    android.widget.Toast.makeText(
                        this@MainActivity,
                        reason,
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }

            LaunchedEffect(habayebViewModel) {
                withContext(Dispatchers.IO) {
                    // Check and execute any recurring transactions on startup safely on background thread
                    HabayebRecurringManager.checkAndExecuteRecurring(context, habayebViewModel) { count ->
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.toast_recurring_txs_success, count),
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

                viewModel.uiEventFlow.collect { event ->
                    when (event) {
                        is com.example.ui.viewmodel.UiEvent.ShowToast -> {
                            android.widget.Toast.makeText(
                                context,
                                context.getString(event.messageRes),
                                if (event.isLong) android.widget.Toast.LENGTH_LONG else android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                        is com.example.ui.viewmodel.UiEvent.ShowActivationDialog -> {
                            securityViewModel.showActivationRequired.value = true
                        }
                    }
                }
            }

            val settings by viewModel.settingsState.collectAsStateWithLifecycle()
            val isSettingsLoaded by viewModel.isSettingsLoaded.collectAsStateWithLifecycle()
            
            var isUnlocked by rememberSaveable { mutableStateOf(!isPasscodeEnabledFast) }

            // Ensure fast preference is strictly synchronized with database settings
            LaunchedEffect(settings.isPasscodeEnabled) {
                if (isSettingsLoaded) {
                    secPrefs.edit().putBoolean("fast_passcode_enabled", settings.isPasscodeEnabled).apply()
                }
            }

            var showOnboardingDialog by remember { mutableStateOf(false) }
            var shouldRequestPermissions by remember { mutableStateOf(false) }

            val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val allGranted = permissions.values.all { it }
                android.util.Log.d("MainActivity", "Permissions completed: allGranted=$allGranted")
            }

            LaunchedEffect(shouldRequestPermissions) {
                if (shouldRequestPermissions) {
                    val permissions = mutableListOf<String>()
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                    if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.S_V2) {
                        permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                        permissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    } else {
                        permissions.add(android.Manifest.permission.READ_MEDIA_IMAGES)
                    }
                    permissionLauncher.launch(permissions.toTypedArray())
                    shouldRequestPermissions = false
                }
            }

            // Strictly check first launch status on start, merging database settings and highly-persistent SharedPreferences lock
            val isReallyFirstLaunch = settings.isFirstLaunch && !viewModel.hasShownOnboarding()
            LaunchedEffect(isReallyFirstLaunch) {
                if (isReallyFirstLaunch) {
                    // Let the user breathe, see and experience the app interface behind first (3500ms elegant delay)
                    kotlinx.coroutines.delay(3500)
                    showOnboardingDialog = true
                }
            }

            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val darkTheme = remember(isSettingsLoaded, settings.themeMode, cachedThemeMode, isSystemDark) {
                if (isSettingsLoaded) {
                    when (settings.themeMode) {
                        1 -> false
                        2 -> true
                        else -> isSystemDark
                    }
                } else {
                    when (cachedThemeMode) {
                        1 -> false
                        2 -> true
                        else -> isSystemDark
                    }
                }
            }

            AppTheme(darkTheme = darkTheme) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    if (isReallyFirstLaunch && showOnboardingDialog) {
                        WelcomeOnboardingDialog(
                            onDismiss = {
                                viewModel.markOnboardingShown() // Persist in SharedPreferences first
                                val updated = settings.copy(isFirstLaunch = false)
                                viewModel.saveSettings(updated)
                                showOnboardingDialog = false
                                shouldRequestPermissions = true // Request storage/post permissions immediately after welcome greeting!
                            }
                        )
                    }

                    if (settings.isPasscodeEnabled && !isUnlocked) {
                        AppLockScreen(
                            viewModel = securityViewModel,
                            onUnlockSuccess = { isUnlocked = true },
                            onUnlockBypassedAndDisabled = {
                                val updated = settings.copy(
                                    isPasscodeEnabled = false,
                                    passcodeHash = null,
                                    recoveryPhraseHash = null
                                )
                                securityViewModel.saveSettings(updated)
                                isUnlocked = true
                            }
                        )
                    } else {
                        MainAppLayout(
                            viewModel = viewModel,
                            habayebViewModel = habayebViewModel,
                            securityViewModel = securityViewModel,
                            backupSyncViewModel = backupSyncViewModel,
                            settings = settings,
                            onExit = { 
                                finishAffinity() 
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            if (::backupSyncViewModel.isInitialized) {
                backupSyncViewModel.triggerSilentLocalBackup()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun logAppSignatureSHA1(context: android.content.Context) {
        try {
            val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                val signingInfo = info.signingInfo
                if (signingInfo != null) {
                    if (signingInfo.hasMultipleSigners()) {
                        signingInfo.apkContentsSigners
                    } else {
                        signingInfo.signingCertificateHistory
                    }
                } else null
            } else {
                @Suppress("DEPRECATION")
                val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                info.signatures
            }
            if (signatures != null) {
                for (signature in signatures) {
                    val md = MessageDigest.getInstance("SHA1")
                    val publicKey = md.digest(signature.toByteArray())
                    val hexString = publicKey.joinToString(":") { String.format("%02X", it) }
                    android.util.Log.d("GOOGLE_AUTH_DEBUG", "SHA-1 ACTUAL SIGNATURE: $hexString")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("GOOGLE_AUTH_DEBUG", "Error getting signature", e)
        }
    }
}
