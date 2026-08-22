package com.example.ui.screens.habayeb.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.local.entities.HabayebCustomer
import com.example.domain.StringUtils
import com.example.ui.helper.rememberContactPicker
import com.example.ui.theme.financialDebtColor

@Composable
fun CustomerDeleteConfirmationDialog(
    customer: HabayebCustomer? = null,
    selectedCustomerIds: List<String> = emptyList(),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val isSingle = customer != null || selectedCustomerIds.size == 1
    val singleCustomerName = customer?.name ?: ""

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val debtRed = financialDebtColor(isDark)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isSingle) {
                    stringResource(id = R.string.habayeb_delete_account_title)
                } else {
                    stringResource(id = R.string.habayeb_bulk_delete_title)
                },
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Text(
                text = if (isSingle) {
                    stringResource(id = R.string.habayeb_delete_account_confirm, singleCustomerName)
                } else {
                    stringResource(id = R.string.habayeb_bulk_delete_confirm, selectedCustomerIds.size)
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = debtRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.habayeb_delete_yes),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(id = R.string.habayeb_cancel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun CustomerEditDialog(
    customer: HabayebCustomer,
    activeThemeColor: Color,
    existingCustomers: List<HabayebCustomer> = emptyList(),
    onConfirm: (name: String, phone: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var editedNameTfv by remember(customer.name) {
        mutableStateOf(TextFieldValue(text = customer.name, selection = TextRange(customer.name.length)))
    }
    var editedPhoneTfv by remember(customer.phone) {
        mutableStateOf(TextFieldValue(text = customer.phone, selection = TextRange(customer.phone.length)))
    }

    var isSaving by remember { mutableStateOf(false) }

    val editedNameStr = editedNameTfv.text
    val editedPhoneStr = editedPhoneTfv.text

    val normalizedExistingNames = remember(existingCustomers, customer.id) {
        existingCustomers
            .filter { it.id != customer.id }
            .map { StringUtils.normalizeArabic(it.name.trim()) }
            .filter { it.isNotBlank() }
            .toSet()
    }

    val normalizedEditedName = remember(editedNameStr) { StringUtils.normalizeArabic(editedNameStr.trim()) }
    val isDuplicateName = remember(editedNameStr, normalizedExistingNames, isSaving) {
        !isSaving && editedNameStr.trim().isNotBlank() && normalizedEditedName.isNotBlank() && normalizedExistingNames.contains(normalizedEditedName)
    }

    val editNameFocusRequester = remember { FocusRequester() }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    val view = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(view) {
        val window = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        onDispose {}
    }

    val launchContactPicker = rememberContactPicker { name, phone ->
        if (name.isNotBlank() && editedNameTfv.text.isBlank()) {
            editedNameTfv = TextFieldValue(text = name, selection = TextRange(name.length))
        }
        if (phone.isNotBlank()) {
            editedPhoneTfv = TextFieldValue(text = phone, selection = TextRange(phone.length))
        }
    }

    LaunchedEffect(Unit) {
        try {
            kotlinx.coroutines.delay(150)
            editNameFocusRequester.requestFocus()
            keyboardController?.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Card(
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .fillMaxWidth(0.92f)
                    .imePadding(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(activeThemeColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = activeThemeColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = stringResource(id = R.string.habayeb_edit_name_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Input Fields
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Account Name Field
                        Column {
                            OutlinedTextField(
                                value = editedNameTfv,
                                onValueChange = { editedNameTfv = it },
                                label = { Text(stringResource(id = R.string.habayeb_account_name)) },
                                singleLine = true,
                                isError = isDuplicateName && editedNameStr.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = activeThemeColor.copy(alpha = 0.8f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(editNameFocusRequester),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedBorderColor = activeThemeColor,
                                    focusedLabelColor = activeThemeColor,
                                    cursorColor = activeThemeColor,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    errorBorderColor = MaterialTheme.colorScheme.error
                                )
                            )
                            if (isDuplicateName && editedNameStr.isNotBlank()) {
                                Text(
                                    text = stringResource(id = R.string.habayeb_error_duplicate_name),
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                )
                            }
                        }

                        // Phone Number Field with Contacts Pick Button
                        OutlinedTextField(
                            value = editedPhoneTfv,
                            onValueChange = { editedPhoneTfv = it },
                            label = { Text(stringResource(id = R.string.habayeb_phone_label)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = activeThemeColor.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { launchContactPicker() },
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .size(36.dp)
                                        .background(
                                            color = activeThemeColor.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Contacts,
                                        contentDescription = stringResource(id = R.string.habayeb_contact_picker),
                                        tint = activeThemeColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedBorderColor = activeThemeColor,
                                focusedLabelColor = activeThemeColor,
                                cursorColor = activeThemeColor,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }

                    // Action Buttons Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.habayeb_cancel),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = {
                                if (editedNameStr.trim().isBlank()) return@Button
                                if (isDuplicateName) {
                                    Toast.makeText(context, context.getString(R.string.habayeb_error_duplicate_name), Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isSaving = true
                                onConfirm(editedNameStr.trim(), editedPhoneStr.trim())
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = activeThemeColor),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 9.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.habayeb_save_edit),
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
