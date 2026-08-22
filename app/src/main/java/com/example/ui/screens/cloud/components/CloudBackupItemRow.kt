package com.example.ui.screens.cloud.components

import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CloudBackupFile

import androidx.compose.ui.graphics.luminance

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CloudBackupItemRow(
    backup: CloudBackupFile,
    menuExpanded: Boolean,
    onMenuToggle: (Boolean) -> Unit,
    onRestoreClick: () -> Unit,
    onDeleteClick: () -> Unit,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectedChange: (Boolean) -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val (dateStr, timeStr) = remember(backup.name, backup.createdTime) {
        formatBackupDateTime(context, backup.name, backup.createdTime)
    }

    val unknownSize = stringResource(R.string.cloud_size_unknown)
    val sizeKbPattern = stringResource(R.string.cloud_size_kb_pattern)

    val displaySize = remember(backup.size, unknownSize, sizeKbPattern) {
        if (backup.size <= 0L) {
            unknownSize
        } else {
            String.format(java.util.Locale.US, sizeKbPattern, backup.size / 1024.0)
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                com.example.ui.theme.InfoBlue.copy(alpha = if (isDark) 0.22f else 0.12f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 0.8.dp,
            color = if (isSelected) com.example.ui.theme.InfoBlue else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onSelectedChange(!isSelected)
                    } else {
                        onMenuToggle(true)
                    }
                },
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Far Left: Size and 3-dots Menu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = displaySize,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!isSelectionMode) {
                    Box {
                        IconButton(
                            onClick = { onMenuToggle(true) },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("backup_menu_${backup.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.cloud_desc_file_options),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { onMenuToggle(false) },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = { 
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Restore, contentDescription = null, tint = com.example.ui.theme.CreditGreen, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = stringResource(R.string.cloud_menu_restore_this),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                },
                                onClick = onRestoreClick
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = stringResource(R.string.cloud_menu_delete_this),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                },
                                onClick = onDeleteClick
                            )
                        }
                    }
                } else {
                    // Checkbox indicator for selection mode
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isSelected) com.example.ui.theme.InfoBlue else MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(start = 2.dp)
                    )
                }
            }

            // Right: Date & Time details
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = dateStr,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = timeStr,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
