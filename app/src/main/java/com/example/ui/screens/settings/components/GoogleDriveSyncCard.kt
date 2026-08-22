package com.example.ui.screens.settings.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BackupTable
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CloudSyncState
import com.example.ui.helper.openGoogleDriveApp
import com.example.ui.theme.EmeraldPrimary

@Composable
fun GoogleDriveSyncCard(
    googleSyncState: CloudSyncState,
    storedEmail: String?,
    isConnected: Boolean,
    isDark: Boolean,
    isSyncLoggingOut: Boolean,
    authUrlProvider: () -> String,
    onSignInClick: () -> Unit,
    onUploadBackupClick: () -> Unit,
    onRestoreBackupClick: () -> Unit,
    onBrowseArchivesClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onManualAuthCodeSubmit: (String) -> Unit
) {
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.backup_cloud_linking_title),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            when (val state = googleSyncState) {
                is CloudSyncState.Idle, is CloudSyncState.Error, is CloudSyncState.SessionExpired -> {
                    var showWebFallback by remember { mutableStateOf(false) }
                    var pastedWebCode by remember { mutableStateOf("") }
                    val focusRequester = remember { FocusRequester() }

                    LaunchedEffect(showWebFallback) {
                        if (showWebFallback) {
                            focusRequester.requestFocus()
                        }
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (state is CloudSyncState.Error) {
                            Text(
                                text = "⚠️ " + state.message,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Button(
                            onClick = onSignInClick,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(15.dp))
                                Text(stringResource(R.string.backup_btn_gdrive_quick), color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        TextButton(
                            onClick = { showWebFallback = !showWebFallback },
                            modifier = Modifier.align(Alignment.CenterHorizontally).height(28.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = if (showWebFallback) stringResource(R.string.backup_btn_gdrive_fallback_hide) else stringResource(R.string.backup_btn_gdrive_fallback_show),
                                color = EmeraldPrimary,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (showWebFallback) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.backup_gdrive_fallback_steps),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    
                                    Button(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrlProvider()))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, context.getString(R.string.backup_toast_browser_failed), Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().height(32.dp)
                                    ) {
                                        Text(stringResource(R.string.backup_btn_gdrive_open_browser), color = MaterialTheme.colorScheme.onPrimary, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedTextField(
                                        value = pastedWebCode,
                                        onValueChange = { pastedWebCode = it },
                                        placeholder = { Text(stringResource(R.string.backup_placeholder_oauth_code), fontSize = 10.sp) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().height(44.dp).focusRequester(focusRequester),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = EmeraldPrimary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    )

                                    Button(
                                        onClick = {
                                            val rawCode = pastedWebCode.trim()
                                            if (rawCode.isNotEmpty()) {
                                                onManualAuthCodeSubmit(rawCode)
                                            }
                                        },
                                        enabled = pastedWebCode.trim().isNotEmpty(),
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().height(32.dp)
                                    ) {
                                        Text(stringResource(R.string.backup_btn_oauth_confirm), fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
                is CloudSyncState.Authenticating -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = EmeraldPrimary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.backup_toast_cloud_auth), fontSize = 10.5.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
                is CloudSyncState.Syncing -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = EmeraldPrimary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.backup_toast_cloud_syncing), fontSize = 10.5.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
                is CloudSyncState.Success, is CloudSyncState.Skipped, is CloudSyncState.Authenticated -> {
                    val email = if (state is CloudSyncState.Authenticated) state.email else (storedEmail ?: stringResource(R.string.cloud_google_connected))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val successBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        val successText = MaterialTheme.colorScheme.onPrimaryContainer
                        val warningBg = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                        val warningText = MaterialTheme.colorScheme.onErrorContainer
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(successBg, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                            Text(
                                text = stringResource(R.string.backup_gdrive_linked_pattern, email),
                                color = successText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp
                            )
                        }

                        Text(
                            text = stringResource(R.string.backup_gdrive_linked_warning),
                            fontSize = 9.sp,
                            color = warningText,
                            lineHeight = 12.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(warningBg, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = onUploadBackupClick,
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(34.dp)
                            ) {
                                Text(stringResource(R.string.backup_btn_upload_backup), fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = onRestoreBackupClick,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(34.dp)
                            ) {
                                Text(stringResource(id = R.string.btn_cloud_restore), fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = onBrowseArchivesClick,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(32.dp).testTag("open_cloud_backups_archive_sheet")
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.BackupTable, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(15.dp))
                                Text(stringResource(R.string.backup_btn_browse_archives), fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSecondary, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = onLogoutClick,
                            enabled = !isSyncLoggingOut,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, disabledContainerColor = MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(30.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSyncLoggingOut) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = MaterialTheme.colorScheme.onErrorContainer, strokeWidth = 2.dp)
                                    Text(stringResource(R.string.backup_gdrive_logging_out), fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(14.dp))
                                    Text(stringResource(R.string.backup_btn_gdrive_logout), fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
