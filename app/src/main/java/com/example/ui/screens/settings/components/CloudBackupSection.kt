package com.example.ui.screens.settings.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CloudSyncState
import com.example.ui.theme.SoftRed
import com.example.ui.viewmodel.BackupSyncViewModel
import com.example.ui.screens.CloudBackupsBottomSheet
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudBackupSection(
    backupSyncViewModel: BackupSyncViewModel,
    isDark: Boolean,
    context: Context,
    googleCloudSyncState: CloudSyncState,
    googleSignInClient: GoogleSignInClient,
    googleSignInLauncher: ActivityResultLauncher<Intent>,
    safExportLauncher: ActivityResultLauncher<String>,
    modifier: Modifier = Modifier
) {
    var showCloudBackupsSheet by remember { mutableStateOf(false) }
    var isSyncLoggingOut by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. المزامنة السحابية المباشرة (Google Drive Direct Sync)
        GoogleDriveSyncCard(
            googleSyncState = googleCloudSyncState,
            storedEmail = backupSyncViewModel.googleDriveSyncHelper.getStoredEmail(),
            isConnected = googleCloudSyncState is CloudSyncState.Success || googleCloudSyncState is CloudSyncState.Authenticated,
            isDark = isDark,
            isSyncLoggingOut = isSyncLoggingOut,
            authUrlProvider = { backupSyncViewModel.googleDriveSyncHelper.getAuthUrl() },
            onSignInClick = {
                try {
                    val playServicesAvailable = run {
                        try {
                            val gmsClass = Class.forName("com.google.android.gms.common.GoogleApiAvailability")
                            val instanceMethod = gmsClass.getMethod("getInstance")
                            val gmsInstance = instanceMethod.invoke(null)
                            val isAvailableMethod = gmsClass.getMethod("isGooglePlayServicesAvailable", Context::class.java)
                            val status = isAvailableMethod.invoke(gmsInstance, context) as Int
                            status == 0
                        } catch (e: Exception) {
                            false
                        }
                    }

                    if (!playServicesAvailable) {
                        Toast.makeText(context, context.getString(R.string.settings_toast_google_missing), Toast.LENGTH_SHORT).show()
                        return@GoogleDriveSyncCard
                    }

                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(R.string.settings_toast_no_network), Toast.LENGTH_SHORT).show()
                }
            },
            onUploadBackupClick = {
                backupSyncViewModel.uploadBackupToGoogleDrive { success -> }
            },
            onRestoreBackupClick = {
                backupSyncViewModel.restoreFromGoogleDriveDirect(context) { success ->
                    if (success) {
                        Toast.makeText(context, context.getString(R.string.settings_toast_gdrive_restore_success), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, context.getString(R.string.settings_toast_gdrive_restore_failed), Toast.LENGTH_LONG).show()
                    }
                }
            },
            onBrowseArchivesClick = {
                showCloudBackupsSheet = true
            },
            onLogoutClick = {
                isSyncLoggingOut = true
                backupSyncViewModel.googleDriveLogout {
                    isSyncLoggingOut = false
                    Toast.makeText(context, context.getString(R.string.settings_toast_gdrive_logout_success), Toast.LENGTH_SHORT).show()
                }
            },
            onManualAuthCodeSubmit = { code ->
                backupSyncViewModel.handleRawOAuthCodeOrUrl(code, null, "http://localhost/oauth2callback") { success ->
                    if (success) {
                        Toast.makeText(context, context.getString(R.string.settings_toast_oauth_success), Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, context.getString(R.string.settings_toast_oauth_failed), Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    if (showCloudBackupsSheet) {
        CloudBackupsBottomSheet(
            viewModel = backupSyncViewModel,
            onDismiss = { showCloudBackupsSheet = false }
        )
    }
}
