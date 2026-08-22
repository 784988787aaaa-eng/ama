package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.entities.AppSettings
import com.example.domain.HashUtils
import com.example.ui.screens.security.components.SecurityActivePanel
import com.example.ui.screens.security.components.SecuritySetupForm
import com.example.ui.viewmodel.SecurityAndLicenseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private suspend fun saveSecurityPasscode(
    passcode: String,
    recoveryPhrase: String,
    recoveryHint: String,
    currentSettings: AppSettings,
    viewModel: SecurityAndLicenseViewModel,
    onSuccess: () -> Unit
) {
    val pHash = HashUtils.hashString(passcode)
    val rHash = HashUtils.hashString(recoveryPhrase.trim())
    val updated = currentSettings.copy(
        isPasscodeEnabled = true,
        passcodeHash = pHash,
        recoveryPhraseHash = rHash,
        recoveryHint = recoveryHint.trim().takeIf { it.isNotBlank() }
    )
    viewModel.saveSettings(updated)
    withContext(Dispatchers.Main) {
        onSuccess()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(
    settings: AppSettings,
    viewModel: SecurityAndLicenseViewModel,
    onBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val currentSettings by viewModel.settingsState.collectAsStateWithLifecycle()
    val isAlreadyPasscodeEnabled = currentSettings.isPasscodeEnabled
    var isEditingPasscode by remember { mutableStateOf(false) }

    var passcode by remember { mutableStateOf("") }
    var confirmPasscode by remember { mutableStateOf("") }
    var recoveryPhrase by remember { mutableStateOf("") }
    var recoveryHint by remember { mutableStateOf("") }
    var checkAcknowledged by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.sec_title),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isEditingPasscode && isAlreadyPasscodeEnabled) {
                            isEditingPasscode = false
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.habayeb_back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.border(
                    width = 0.5.dp, 
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            if (!isAlreadyPasscodeEnabled || isEditingPasscode) {
                SecuritySetupForm(
                    passcode = passcode,
                    onPasscodeChange = { passcode = it },
                    confirmPasscode = confirmPasscode,
                    onConfirmPasscodeChange = { confirmPasscode = it },
                    recoveryPhrase = recoveryPhrase,
                    onRecoveryPhraseChange = { recoveryPhrase = it },
                    recoveryHint = recoveryHint,
                    onRecoveryHintChange = { recoveryHint = it },
                    checkAcknowledged = checkAcknowledged,
                    onCheckAcknowledgedChange = { checkAcknowledged = it },
                    isSaving = isSaving,
                    onSave = {
                        val isValid = passcode.length == 4 &&
                                confirmPasscode == passcode &&
                                recoveryPhrase.isNotBlank() &&
                                checkAcknowledged &&
                                !isSaving
                        if (isValid) {
                            isSaving = true
                            coroutineScope.launch(Dispatchers.Default) {
                                saveSecurityPasscode(
                                    passcode = passcode,
                                    recoveryPhrase = recoveryPhrase,
                                    recoveryHint = recoveryHint,
                                    currentSettings = currentSettings,
                                    viewModel = viewModel
                                ) {
                                    isSaving = false
                                    isEditingPasscode = false
                                    Toast.makeText(context, context.getString(R.string.sec_toast_enabled_success), Toast.LENGTH_SHORT).show()
                                    onBack()
                                }
                            }
                        }
                    }
                )
            } else {
                SecurityActivePanel(
                    currentSettings = currentSettings,
                    viewModel = viewModel,
                    onCopyRecoveryPhrase = {
                        if (!currentSettings.recoveryPhraseHash.isNullOrBlank()) {
                            clipboardManager.setText(AnnotatedString(currentSettings.recoveryPhraseHash!!))
                            Toast.makeText(context, context.getString(R.string.sec_toast_copied), Toast.LENGTH_SHORT).show()
                        }
                    },
                    onChangePasscode = {
                        passcode = ""
                        confirmPasscode = ""
                        isEditingPasscode = true
                    },
                    onDeactivateSecurity = {
                        val updated = currentSettings.copy(
                            isPasscodeEnabled = false,
                            passcodeHash = null,
                            recoveryPhraseHash = null,
                            recoveryHint = null
                        )
                        viewModel.saveSettings(updated)
                        isEditingPasscode = false
                        Toast.makeText(context, context.getString(R.string.sec_toast_disabled), Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityDialog(
    settings: AppSettings,
    viewModel: SecurityAndLicenseViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val currentSettings by viewModel.settingsState.collectAsStateWithLifecycle()
    val isAlreadyPasscodeEnabled = currentSettings.isPasscodeEnabled
    var isEditingPasscodeInDialog by remember { mutableStateOf(false) }

    var passcode by remember { mutableStateOf("") }
    var confirmPasscode by remember { mutableStateOf("") }
    var recoveryPhrase by remember { mutableStateOf("") }
    var recoveryHint by remember { mutableStateOf("") }
    var checkAcknowledged by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 340.dp)
                .padding(4.dp)
                .imePadding()
                .animateContentSize(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Title and Close Icon (Aligned nicely)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(id = R.string.sec_title),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(id = R.string.calc_close_desc),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isAlreadyPasscodeEnabled || isEditingPasscodeInDialog) {
                        SecuritySetupForm(
                            passcode = passcode,
                            onPasscodeChange = { passcode = it },
                            confirmPasscode = confirmPasscode,
                            onConfirmPasscodeChange = { confirmPasscode = it },
                            recoveryPhrase = recoveryPhrase,
                            onRecoveryPhraseChange = { recoveryPhrase = it },
                            recoveryHint = recoveryHint,
                            onRecoveryHintChange = { recoveryHint = it },
                            checkAcknowledged = checkAcknowledged,
                            onCheckAcknowledgedChange = { checkAcknowledged = it },
                            isSaving = isSaving,
                            onSave = {
                                val isValid = passcode.length == 4 &&
                                        confirmPasscode == passcode &&
                                        recoveryPhrase.isNotBlank() &&
                                        checkAcknowledged &&
                                        !isSaving
                                if (isValid) {
                                    isSaving = true
                                    coroutineScope.launch(Dispatchers.Default) {
                                        saveSecurityPasscode(
                                            passcode = passcode,
                                            recoveryPhrase = recoveryPhrase,
                                            recoveryHint = recoveryHint,
                                            currentSettings = currentSettings,
                                            viewModel = viewModel
                                        ) {
                                            isSaving = false
                                            isEditingPasscodeInDialog = false
                                            Toast.makeText(context, context.getString(R.string.sec_toast_enabled_success), Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        }
                                    }
                                }
                            }
                        )
                    } else {
                        SecurityActivePanel(
                            currentSettings = currentSettings,
                            viewModel = viewModel,
                            onCopyRecoveryPhrase = {
                                if (!currentSettings.recoveryPhraseHash.isNullOrBlank()) {
                                    clipboardManager.setText(AnnotatedString(currentSettings.recoveryPhraseHash!!))
                                    Toast.makeText(context, context.getString(R.string.sec_toast_copied), Toast.LENGTH_SHORT).show()
                                }
                            },
                            onChangePasscode = {
                                passcode = ""
                                confirmPasscode = ""
                                isEditingPasscodeInDialog = true
                            },
                            onDeactivateSecurity = {
                                val updated = currentSettings.copy(
                                    isPasscodeEnabled = false,
                                    passcodeHash = null,
                                    recoveryPhraseHash = null,
                                    recoveryHint = null
                                )
                                viewModel.saveSettings(updated)
                                isEditingPasscodeInDialog = false
                                Toast.makeText(context, context.getString(R.string.sec_toast_disabled), Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

