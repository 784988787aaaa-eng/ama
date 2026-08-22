package com.example.ui.screens.cloud.components

import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.EmeraldPrimary

@Composable
fun CloudStatsHeader(
    email: String,
    backupsCount: Int,
    isFetching: Boolean,
    onRefresh: () -> Unit,
    isSelectionMode: Boolean = false,
    isAllSelected: Boolean = false,
    onToggleSelectAll: () -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Right Section: Icon + Email + Stats Summary
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (isSelectionMode) com.example.ui.theme.InfoBlueBgLight else com.example.ui.theme.CreditContainerLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSelectionMode) Icons.Default.Checklist else Icons.Default.Backup,
                        contentDescription = null,
                        tint = if (isSelectionMode) com.example.ui.theme.InfoBlue else com.example.ui.theme.CreditGreen,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = email,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(com.example.ui.theme.CreditGreen)
                        )
                    }
                    Text(
                        text = stringResource(R.string.cloud_stat_count_pattern, backupsCount) + " | " + stringResource(R.string.cloud_stat_taken_space),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp
                    )
                }
            }

            // Left Section: Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    TextButton(
                        onClick = onToggleSelectAll,
                        colors = ButtonDefaults.textButtonColors(contentColor = com.example.ui.theme.InfoBlue),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text(
                            text = if (isAllSelected) stringResource(R.string.cloud_btn_cancel_selection) else stringResource(R.string.cloud_btn_select_all),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    IconButton(
                        onClick = onRefresh,
                        enabled = !isFetching,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("refresh_cloud_backups_stats_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.cloud_desc_refresh),
                            tint = if (isFetching) Color.Gray else EmeraldPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}
