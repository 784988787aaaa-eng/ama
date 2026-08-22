package com.example.ui.screens.business

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@Composable
fun BusinessProfileInfoSection(
    bizName: String,
    onBizNameChange: (String) -> Unit,
    bizDesc: String,
    onBizDescChange: (String) -> Unit,
    isDialog: Boolean,
    activeThemeColor: Color
) {
    val focusManager = LocalFocusManager.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDialog) 0.dp else 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.biz_details_section),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = bizName,
                onValueChange = { if (it.length <= 40) onBizNameChange(it) },
                label = { Text(text = stringResource(id = R.string.biz_label_name), fontSize = 13.sp) },
                placeholder = { Text(text = stringResource(id = R.string.biz_placeholder_name), fontSize = 13.sp) },
                supportingText = {
                    Text(
                        text = "${bizName.length}/40",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("biz_name_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = activeThemeColor,
                    focusedLabelColor = activeThemeColor,
                    cursorColor = activeThemeColor
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right)
            )

            OutlinedTextField(
                value = bizDesc,
                onValueChange = { if (it.length <= 45) onBizDescChange(it) },
                label = { Text(text = stringResource(id = R.string.biz_label_desc), fontSize = 13.sp) },
                placeholder = { Text(text = stringResource(id = R.string.biz_placeholder_desc), fontSize = 13.sp) },
                supportingText = {
                    Text(
                        text = "${bizDesc.length}/45",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                },
                singleLine = true,
                maxLines = 1,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("biz_desc_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = activeThemeColor,
                    focusedLabelColor = activeThemeColor,
                    cursorColor = activeThemeColor
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right)
            )
        }
    }
}
