package com.example.ui.screens.habayeb.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entities.CustomCategory

@Composable
fun CategoryOptionsPanel(
    categoryKey: String,
    closedCategoryName: String,
    customCategories: List<CustomCategory>,
    activeThemeColor: Color,
    onDismiss: () -> Unit,
    onRename: (CustomCategory, String) -> Unit,
    onRenameClosed: (String) -> Unit,
    onDelete: (CustomCategory) -> Unit,
    onMoveLeft: (String) -> Unit,
    onMoveRight: (String) -> Unit
) {
    val isSystem = categoryKey == "CLOSED"
    val customCat = remember(customCategories, categoryKey) {
        customCategories.find { it.name == categoryKey }
    }
    val displayName = remember(isSystem, closedCategoryName, categoryKey) {
        if (isSystem) closedCategoryName else categoryKey
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(activeThemeColor, CircleShape)
                )
                Text(
                    text = stringResource(R.string.habayeb_category_edit_prefix, displayName),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Move Right (Icon > moves category physically to the RIGHT)
                OptionCircularIconButton(
                    onClick = { onMoveRight(categoryKey) },
                    icon = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Move Right",
                    tint = MaterialTheme.colorScheme.onSurface
                )

                // Move Left (Icon < moves category physically to the LEFT)
                OptionCircularIconButton(
                    onClick = { onMoveLeft(categoryKey) },
                    icon = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Move Left",
                    tint = MaterialTheme.colorScheme.onSurface
                )

                // Edit Name
                var showRenameDialog by remember { mutableStateOf(false) }
                OptionCircularIconButton(
                    onClick = { showRenameDialog = true },
                    icon = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )

                if (showRenameDialog) {
                    MicroRenameCategoryDialog(
                        initialName = displayName,
                        activeThemeColor = activeThemeColor,
                        onDismiss = { showRenameDialog = false },
                        onSave = { newName ->
                            if (newName.isNotBlank()) {
                                if (isSystem) {
                                    onRenameClosed(newName.trim())
                                } else if (customCat != null) {
                                    onRename(customCat, newName.trim())
                                }
                            }
                            showRenameDialog = false
                            onDismiss()
                        }
                    )
                }

                if (!isSystem && customCat != null) {
                    // Delete Category
                    OptionCircularIconButton(
                        onClick = { onDelete(customCat) },
                        icon = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        backgroundColor = Color.Transparent
                    )
                }

                // Close Options
                OptionCircularIconButton(
                    onClick = onDismiss,
                    icon = Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun OptionCircularIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    tint: Color,
    backgroundColor: Color = MaterialTheme.colorScheme.surface
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    IconButton(
        onClick = {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = Modifier
            .size(24.dp)
            .background(backgroundColor, CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(14.dp)
        )
    }
}
