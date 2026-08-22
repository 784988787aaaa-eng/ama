package com.example.ui.screens.ledger.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.LicenseCheckResult
import com.example.ui.viewmodel.BackupSyncViewModel
import com.example.ui.viewmodel.SecurityAndLicenseViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

/**
 * Unified Facade for Device Activation & Licensing Dialog.
 * Beautifully refactored into modular subcomponents for maximum maintainability.
 */
@Composable
fun DeviceActivationDialog(
    deviceId: String,
    viewModel: SecurityAndLicenseViewModel,
    backupSyncViewModel: BackupSyncViewModel? = null,
    onDismiss: () -> Unit,
    isAutoTriggered: Boolean = false
) {
    val isActivated by viewModel.isActivatedState.collectAsStateWithLifecycle()
    val activatedEmail by viewModel.activatedEmailState.collectAsStateWithLifecycle()
    val isLicenseLoading by viewModel.isLicenseLoading.collectAsStateWithLifecycle()
    val storedEmail by com.example.domain.GoogleAuthSessionManager.currentEmail.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Google / Cloud, 1: Product Key
    var activationCodeInput by remember { mutableStateOf("") }
    var isCodeError by remember { mutableStateOf(false) }
    var actionFeedbackMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val googleSignInClient = remember(backupSyncViewModel) {
        try {
            backupSyncViewModel?.googleDriveSyncHelper?.getGoogleSignInClient()
        } catch (e: Exception) {
            android.util.Log.e("DeviceActivationDialog", "GoogleSignInClient creation error", e)
            null
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val intent = result.data
        if (result.resultCode == android.app.Activity.RESULT_OK && intent != null) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
            try {
                val account = task.getResult(ApiException::class.java)
                val authCode = account?.serverAuthCode
                val email = account?.email ?: ""
                if (authCode != null && backupSyncViewModel != null) {
                    backupSyncViewModel.handleGoogleOAuthCode(authCode, email) { success ->
                        if (success) {
                            Toast.makeText(context, context.getString(R.string.backup_toast_linked_success, email), Toast.LENGTH_LONG).show()
                            if (email.isNotEmpty()) {
                                viewModel.activateWithFirebaseEmail(email) { res ->
                                    actionFeedbackMessage = when (res) {
                                        is LicenseCheckResult.Success -> null
                                        is LicenseCheckResult.DeviceMismatch -> context.getString(R.string.licensing_fluent_mismatch_error)
                                        is LicenseCheckResult.NotLicensed -> res.message
                                        is LicenseCheckResult.Error -> res.message
                                    }
                                    if (res is LicenseCheckResult.Success) {
                                        Toast.makeText(context, context.getString(R.string.licensing_fluent_toast_active_success), Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(context, context.getString(R.string.backup_toast_connect_failed), Toast.LENGTH_LONG).show()
                        }
                    }
                } else if (email.isNotEmpty()) {
                    backupSyncViewModel?.googleDriveSyncHelper?.storeEmail(email)
                    viewModel.activateWithFirebaseEmail(email) { res ->
                        actionFeedbackMessage = when (res) {
                            is LicenseCheckResult.Success -> null
                            is LicenseCheckResult.DeviceMismatch -> context.getString(R.string.licensing_fluent_mismatch_error)
                            is LicenseCheckResult.NotLicensed -> res.message
                            is LicenseCheckResult.Error -> res.message
                        }
                        if (res is LicenseCheckResult.Success) {
                            Toast.makeText(context, context.getString(R.string.licensing_fluent_toast_active_success), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is ApiException && (e.statusCode == com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.SIGN_IN_CANCELLED || e.statusCode == 12501 || e.statusCode == 16)) {
                    android.util.Log.i("DeviceActivationDialog", "Google sign in was cancelled by user")
                    Toast.makeText(context, context.getString(R.string.backup_toast_cancelled), Toast.LENGTH_SHORT).show()
                } else {
                    android.util.Log.e("DeviceActivationDialog", "Google sign in error", e)
                    Toast.makeText(context, context.getString(R.string.backup_toast_connect_error, e.localizedMessage ?: ""), Toast.LENGTH_LONG).show()
                }
            }
        } else if (result.resultCode == android.app.Activity.RESULT_CANCELED) {
            Toast.makeText(context, context.getString(R.string.backup_toast_cancelled), Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 400.dp)
                .wrapContentHeight()
                .clip(RoundedCornerShape(22.dp))
                .testTag("unified_activation_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                ActivationHeaderSection(
                    isActivated = isActivated,
                    isAutoTriggered = isAutoTriggered,
                    onDismiss = onDismiss
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Status Banner
                ActivationStatusBanner(
                    isActivated = isActivated,
                    isAutoTriggered = isAutoTriggered
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (isActivated) {
                    // Activated State Body
                    ActivationActivatedBody(
                        storedEmail = storedEmail,
                        activatedEmail = activatedEmail,
                        onLogout = {
                            backupSyncViewModel?.googleDriveLogout {
                                viewModel.clearLocalActivationData()
                                actionFeedbackMessage = null
                                Toast.makeText(context, context.getString(R.string.sec_toast_disabled), Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDismiss = onDismiss
                    )
                } else {
                    // Segmented Tabs
                    ActivationSegmentedTabs(
                        selectedTab = selectedTab,
                        onTabSelected = {
                            selectedTab = it
                            actionFeedbackMessage = null
                            isCodeError = false
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                        label = "TabContentTransition"
                    ) { tabIndex ->
                        if (tabIndex == 0) {
                            ActivationGoogleTabContent(
                                storedEmail = storedEmail,
                                isLicenseLoading = isLicenseLoading,
                                onGoogleSignInClick = {
                                    val client = googleSignInClient
                                    if (client != null) {
                                        try {
                                            googleSignInLauncher.launch(client.signInIntent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, context.getString(R.string.licensing_fluent_toast_google_failed), Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.licensing_fluent_toast_google_unavailable), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onGoogleActivateClick = {
                                    if (!storedEmail.isNullOrBlank()) {
                                        viewModel.activateWithFirebaseEmail(storedEmail!!) { res ->
                                            actionFeedbackMessage = when (res) {
                                                is LicenseCheckResult.Success -> null
                                                is LicenseCheckResult.DeviceMismatch -> context.getString(R.string.licensing_fluent_mismatch_error)
                                                is LicenseCheckResult.NotLicensed -> res.message
                                                is LicenseCheckResult.Error -> res.message
                                            }
                                            if (res is LicenseCheckResult.Success) {
                                                Toast.makeText(context, context.getString(R.string.licensing_fluent_toast_active_success), Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                }
                            )
                        } else {
                            ActivationKeyInputSection(
                                activationCodeInput = activationCodeInput,
                                isCodeError = isCodeError,
                                onCodeInputChange = {
                                    activationCodeInput = it
                                    isCodeError = false
                                },
                                onVerifyManualCode = {
                                    val cleanInput = activationCodeInput.trim().uppercase()
                                    val success = viewModel.activateLicense(cleanInput)
                                    if (success) {
                                        Toast.makeText(context, context.getString(R.string.licensing_fluent_toast_active_success), Toast.LENGTH_LONG).show()
                                        onDismiss()
                                    } else {
                                        isCodeError = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Device ID Bar
                    ActivationDeviceIdBar(
                        deviceId = deviceId,
                        onCopyClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("DeviceID", deviceId)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, context.getString(R.string.licensing_fluent_copied_toast), Toast.LENGTH_SHORT).show()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )

                    // Feedback Banner
                    ActivationFeedbackBanner(
                        actionFeedbackMessage = actionFeedbackMessage,
                        onWhatsAppRequestClick = {
                            val googleEmailStr = storedEmail ?: context.getString(R.string.licensing_fluent_unregistered_email)
                            val msg = context.getString(R.string.licensing_whatsapp_email_request_template, googleEmailStr, deviceId)
                            openWhatsAppSupportDirect(context, msg)
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Bottom Actions
                    ActivationActionsFooter(
                        onWhatsAppClick = {
                            val googleEmailStr = storedEmail ?: context.getString(R.string.licensing_fluent_unregistered_email)
                            val msg = context.getString(R.string.licensing_whatsapp_email_request_template, googleEmailStr, deviceId)
                            openWhatsAppSupportDirect(context, msg)
                        },
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}
