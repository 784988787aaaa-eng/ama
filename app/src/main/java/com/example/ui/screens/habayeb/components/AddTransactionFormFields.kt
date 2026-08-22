package com.example.ui.screens.habayeb.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.screens.habayeb.utils.HabayebDateFormatter
import java.util.Date

@Composable
fun AddTransactionFormFields(
    amountTfv: TextFieldValue,
    onAmountChange: (TextFieldValue) -> Unit,
    descTfv: TextFieldValue,
    onDescChange: (TextFieldValue) -> Unit,
    selectedTransactionCurrency: String,
    dateMillis: Long,
    dynamicThemeColor: Color,
    amountFocusRequester: FocusRequester,
    descFocusRequester: FocusRequester,
    onOpenCalculator: () -> Unit,
    onOpenDatePicker: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    val fieldShape = remember { RoundedCornerShape(8.dp) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        focusedBorderColor = dynamicThemeColor,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        cursorColor = dynamicThemeColor
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = amountTfv,
            onValueChange = onAmountChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(amountFocusRequester),
            placeholder = {
                Text(
                    text = stringResource(id = R.string.habayeb_amount_required),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { descFocusRequester.requestFocus() }),
            colors = fieldColors,
            singleLine = true,
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            ),
            leadingIcon = {
                IconButton(onClick = onOpenCalculator) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = stringResource(id = R.string.habayeb_calculator),
                        tint = dynamicThemeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            trailingIcon = {
                Text(
                    text = selectedTransactionCurrency,
                    color = dynamicThemeColor,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 12.dp),
                    fontSize = 14.sp
                )
            },
            shape = fieldShape
        )

        Spacer(modifier = Modifier.height(4.dp))

        val formattedSelectedDate = remember(dateMillis) {
            HabayebDateFormatter.formatDateArabic(Date(dateMillis))
        }

        OutlinedTextField(
            value = descTfv,
            onValueChange = onDescChange,
            placeholder = {
                Text(
                    text = stringResource(id = R.string.habayeb_tx_desc_optional),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            colors = fieldColors,
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                fontSize = 13.sp
            ),
            leadingIcon = {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formattedSelectedDate,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(
                        onClick = {
                            focusManager.clearFocus()
                            onOpenDatePicker()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = stringResource(id = R.string.habayeb_tx_date),
                            tint = dynamicThemeColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
            },
            shape = fieldShape,
            singleLine = false,
            maxLines = 2,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp, max = 56.dp)
                .focusRequester(descFocusRequester),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )
    }
}
