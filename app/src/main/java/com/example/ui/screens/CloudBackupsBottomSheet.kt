package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.CloudSyncState
import com.example.ui.screens.cloud.components.CloudBackupsListSection
import com.example.ui.screens.cloud.components.CloudBottomActionBar
import com.example.ui.screens.cloud.components.CloudDeleteConfirmDialog
import com.example.ui.screens.cloud.components.CloudHeaderBar
import com.example.ui.screens.cloud.components.CloudMultiDeleteConfirmDialog
import com.example.ui.screens.cloud.components.CloudNotConnectedView
import com.example.ui.screens.cloud.components.CloudOngoingActionDialog
import com.example.ui.screens.cloud.components.CloudRestoreConfirmDialog
import com.example.ui.screens.cloud.components.CloudStatsHeader
import com.example.ui.theme.SoftRed
import com.example.ui.viewmodel.BackupSyncViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudBackupsBottomSheet(
    viewModel: BackupSyncViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    // Collect flows safely using safe lifecycle-aware state collectors
    val cloudBackups by viewModel.filteredCloudBackups.collectAsStateWithLifecycle()
    val isFetching by viewModel.isFetchingCloudBackups.collectAsStateWithLifecycle()
    val syncState by viewModel.googleDriveSyncState.collectAsStateWithLifecycle()
    
    val storedEmail = remember(syncState) { viewModel.googleDriveSyncHelper.getStoredEmail() }
    val isConnected = remember(storedEmail, syncState) {
        !storedEmail.isNullOrEmpty() || syncState is CloudSyncState.Authenticated || syncState is CloudSyncState.Success
    }
    
    // UI Local States
    var isSearchActive by remember { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    
    var showRestoreConfirmId by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmId by remember { mutableStateOf<String?>(null) }
    var menuExpandedFileId by remember { mutableStateOf<String?>(null) }
    var ongoingActionMessage by remember { mutableStateOf<String?>(null) }
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedFileIds = remember { mutableStateListOf<String>() }
    var showMultiDeleteConfirm by remember { mutableStateOf(false) }

    // Optimization: O(1) set lookup for selection checks during LazyColumn scroll
    val selectedFileIdsSet = remember(selectedFileIds.toList()) { selectedFileIds.toSet() }

    // Fetch cloud backups list when bottom sheet opens
    LaunchedEffect(Unit) {
        if (isConnected) {
            viewModel.fetchCloudBackupsList()
        }
    }

    LaunchedEffect(syncState) {
        if (syncState is CloudSyncState.Success) {
            Toast.makeText(context, context.getString(R.string.cloud_toast_new_backup_success), Toast.LENGTH_LONG).show()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 80.dp), // space for bottom button
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Row Component
                    CloudHeaderBar(
                        isSearchActive = isSearchActive,
                        searchQuery = searchQuery,
                        isConnected = isConnected,
                        hasBackupsOrSearch = cloudBackups.isNotEmpty() || searchQuery.isNotEmpty(),
                        isSelectionMode = isSelectionMode,
                        onToggleSearch = { active -> isSearchActive = active },
                        onSearchQueryChange = { query -> viewModel.updateSearchQuery(query) },
                        onToggleSelectionMode = {
                            isSelectionMode = !isSelectionMode
                            if (!isSelectionMode) {
                                selectedFileIds.clear()
                            }
                        },
                        onDismiss = onDismiss
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                    if (!isConnected) {
                        CloudNotConnectedView()
                    } else {
                        val isAllSelected = cloudBackups.isNotEmpty() && selectedFileIds.size == cloudBackups.size
                        CloudStatsHeader(
                            email = storedEmail ?: stringResource(R.string.cloud_default_connected_acc),
                            backupsCount = cloudBackups.size,
                            isFetching = isFetching,
                            onRefresh = { viewModel.fetchCloudBackupsList() },
                            isSelectionMode = isSelectionMode,
                            isAllSelected = isAllSelected,
                            onToggleSelectAll = {
                                if (isAllSelected) {
                                    selectedFileIds.clear()
                                } else {
                                    selectedFileIds.clear()
                                    selectedFileIds.addAll(cloudBackups.map { it.id })
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        if (syncState is CloudSyncState.Error) {
                            val errMsg = (syncState as CloudSyncState.Error).message
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.cloud_warn_perm_conn),
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        text = stringResource(R.string.cloud_warn_perm_conn_desc, errMsg),
                                        color = SoftRed,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        // Cloud Backups List Section (Loading / Empty / LazyColumn)
                        CloudBackupsListSection(
                            isFetching = isFetching,
                            cloudBackups = cloudBackups,
                            selectedFileIdsSet = selectedFileIdsSet,
                            isSelectionMode = isSelectionMode,
                            menuExpandedFileId = menuExpandedFileId,
                            onMenuToggle = { menuExpandedFileId = it },
                            onRestoreClick = { showRestoreConfirmId = it },
                            onDeleteClick = { showDeleteConfirmId = it },
                            onItemSelectToggle = { id, selected ->
                                if (selected) selectedFileIds.add(id) else selectedFileIds.remove(id)
                            },
                            onItemLongClick = { id ->
                                if (!isSelectionMode) {
                                    isSelectionMode = true
                                    selectedFileIds.clear()
                                    selectedFileIds.add(id)
                                }
                            },
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }

                // Bottom Floating Action bar
                if (isConnected) {
                    CloudBottomActionBar(
                        isSelectionMode = isSelectionMode,
                        selectedCount = selectedFileIds.size,
                        onMultiDeleteClick = { showMultiDeleteConfirm = true },
                        onInstantBackupClick = {
                            ongoingActionMessage = context.getString(R.string.cloud_progress_uploading_instant)
                            viewModel.uploadBackupToGoogleDrive {
                                ongoingActionMessage = null
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }

    // --- Action Overlay Dialogs ---
    if (ongoingActionMessage != null) {
        CloudOngoingActionDialog(ongoingActionMessage!!)
    }

    if (showRestoreConfirmId != null) {
        val targetId = showRestoreConfirmId!!
        CloudRestoreConfirmDialog(
            context = context,
            targetId = targetId,
            cloudBackups = cloudBackups,
            viewModel = viewModel,
            onDismiss = { showRestoreConfirmId = null },
            onStartAction = { msg -> ongoingActionMessage = msg },
            onCompleteAction = { ongoingActionMessage = null },
            onSheetDismiss = onDismiss
        )
    }

    if (showDeleteConfirmId != null) {
        val targetId = showDeleteConfirmId!!
        CloudDeleteConfirmDialog(
            context = context,
            targetId = targetId,
            cloudBackups = cloudBackups,
            viewModel = viewModel,
            onDismiss = { showDeleteConfirmId = null },
            onStartAction = { msg -> ongoingActionMessage = msg },
            onCompleteAction = { ongoingActionMessage = null }
        )
    }

    if (showMultiDeleteConfirm) {
        CloudMultiDeleteConfirmDialog(
            context = context,
            selectedFileIds = selectedFileIds.toList(),
            viewModel = viewModel,
            onDismiss = { showMultiDeleteConfirm = false },
            onStartAction = { msg -> ongoingActionMessage = msg },
            onCompleteAction = { success -> 
                ongoingActionMessage = null
                if (success) {
                    selectedFileIds.clear()
                    isSelectionMode = false
                }
            }
        )
    }
}
