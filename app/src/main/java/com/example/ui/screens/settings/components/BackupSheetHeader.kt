package com.example.ui.screens.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CreditContainerDark
import com.example.ui.theme.CreditContainerLight
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.CreditGreenDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SelectionGreen

@Composable
fun BackupSheetHeader(
    isConnected: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CloudQueue,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = stringResource(R.string.backup_sheet_title),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        val connectedBg = if (isDark) CreditContainerDark else CreditContainerLight
        val connectedText = if (isDark) CreditGreenDark else CreditGreen
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isConnected) connectedBg else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) SelectionGreen else MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Text(
                    text = if (isConnected) stringResource(R.string.backup_status_connected) else stringResource(R.string.backup_status_local),
                    fontSize = 8.5.sp,
                    color = if (isConnected) connectedText else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
