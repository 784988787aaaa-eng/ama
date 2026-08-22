package com.example.ui.screens.settings.components

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entities.AppSettings
import com.example.ui.helper.shareBackupFile
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.BackupSyncViewModel
import java.io.File

@Composable
fun FileTransferManager(
    backupSyncViewModel: BackupSyncViewModel,
    context: Context,
    localBackups: List<File>,
    safRestoreLauncher: ActivityResultLauncher<Array<String>>,
    checkBackupPermissionsGranted: () -> Boolean,
    onShowPermissionExplanation: (() -> Unit) -> Unit,
    onRestoreSuccess: (AppSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. تصدير واستيراد ملفات (.mzd)
        QuadBackupItem(
            title = stringResource(R.string.settings_backup_portable_title),
            description = stringResource(R.string.settings_backup_portable_desc),
            accentColor = MaterialTheme.colorScheme.primary,
            icon = { Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = {
                        val runBackup = {
                            backupSyncViewModel.createLocalBackup(context) { file ->
                                if (file != null) {
                                    shareBackupFile(context, file)
                                }
                            }
                        }
                        if (checkBackupPermissionsGranted()) {
                            runBackup()
                        } else {
                            onShowPermissionExplanation(runBackup)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(36.dp)
                ) {
                    Text(stringResource(R.string.settings_export_mzd), fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        safRestoreLauncher.launch(arrayOf("application/*"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(36.dp)
                ) {
                    Text(stringResource(R.string.settings_import_mzd), fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (localBackups.isNotEmpty()) {
            Text(
                text = stringResource(R.string.settings_discovered_backups_title),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.End).padding(top = 2.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 120.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                localBackups.forEach { file ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                backupSyncViewModel.restoreFromLocalFile(file, context) { success, restoredSettings ->
                                    if (success && restoredSettings != null) {
                                        onRestoreSuccess(restoredSettings)
                                    }
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.settings_desc_restore), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            Text(
                                text = file.name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Right,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
