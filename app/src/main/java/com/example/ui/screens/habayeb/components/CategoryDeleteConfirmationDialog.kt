package com.example.ui.screens.habayeb.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R

@Composable
fun CategoryDeleteConfirmationDialog(
    categoryName: String,
    activeThemeColor: Color,
    onDismiss: () -> Unit,
    onConfirmDelete: (deleteLinkedAccounts: Boolean) -> Unit
) {
    val errorColor = MaterialTheme.colorScheme.error
    val buttonShape = remember { RoundedCornerShape(8.dp) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .widthIn(max = 300.dp)
                .fillMaxWidth(0.88f)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.habayeb_category_delete_confirm, categoryName),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                // Cancel button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeThemeColor,
                        contentColor = Color.White
                    ),
                    shape = buttonShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                ) {
                    Text(
                        text = stringResource(R.string.habayeb_category_delete_cancel),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Delete Category Only
                OutlinedButton(
                    onClick = {
                        onConfirmDelete(false)
                        onDismiss()
                    },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = buttonShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                ) {
                    Text(
                        text = stringResource(R.string.habayeb_category_delete_only),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                // Delete Category and Linked Accounts
                OutlinedButton(
                    onClick = {
                        onConfirmDelete(true)
                        onDismiss()
                    },
                    border = BorderStroke(1.dp, errorColor.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = errorColor
                    ),
                    shape = buttonShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                ) {
                    Text(
                        text = stringResource(R.string.habayeb_category_delete_all_accounts),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}
