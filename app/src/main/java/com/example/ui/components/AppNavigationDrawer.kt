package com.example.ui.components

import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entities.AppSettings
import com.example.ui.navigation.Screen
import com.example.ui.helper.dialPhoneNumber
import com.example.ui.helper.openWhatsAppChat
import com.example.ui.screens.BusinessProfileDialog
import com.example.ui.screens.SecurityDialog
import com.example.ui.viewmodel.SecurityAndLicenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigationDrawer(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
    onBackupClick: () -> Unit,
    isActivated: Boolean,
    onActivateProClick: () -> Unit,
    settings: AppSettings,
    securityViewModel: SecurityAndLicenseViewModel,
    onSaveSettings: (AppSettings, String, Double, Boolean) -> Unit,
    versionName: String,
    onComprehensiveReportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val supportPhoneNumber = stringResource(id = R.string.support_phone_number)
    
    // Smooth state management to show the beautiful compact pop-up dialog
    var isShowingCurrencySettings by remember { mutableStateOf(false) }
    var isShowingBusinessProfile by remember { mutableStateOf(false) }
    var isShowingSecuritySettings by remember { mutableStateOf(false) }

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth(0.85f)
            .widthIn(max = 310.dp)
            .fillMaxHeight(),
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        // Shared Drawer Header Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
                val isDark = remember(settings.themeMode, systemDark) {
                    when (settings.themeMode) {
                        1 -> false
                        2 -> true
                        else -> systemDark
                    }
                }

                IconButton(
                    onClick = {
                        val newMode = if (isDark) 1 else 2
                        val sharedPrefs = context.getSharedPreferences("fast_theme_prefs", android.content.Context.MODE_PRIVATE)
                        sharedPrefs.edit().putInt("key_fast_theme_mode", newMode).apply()
                        onSaveSettings(settings.copy(themeMode = newMode), "", 0.0, false)
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = if (isDark) Icons.Default.WbSunny else Icons.Default.NightsStay,
                        contentDescription = stringResource(id = R.string.desc_toggle_dark_mode),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Text(
                        text = stringResource(id = R.string.app_name_main),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Single unified scrollable column of primary items
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DrawerItem(
                selected = false,
                icon = Icons.Default.People,
                label = stringResource(id = R.string.drawer_business_profile_label),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isShowingBusinessProfile = true
                }
            )

            DrawerItem(
                selected = false,
                icon = Icons.Default.Assessment,
                label = stringResource(id = R.string.drawer_comprehensive_report_label),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onComprehensiveReportClick()
                }
            )
            
            DrawerItem(
                selected = false,
                icon = Icons.Default.Settings,
                label = stringResource(id = R.string.drawer_currency_label),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isShowingCurrencySettings = true
                }
            )
            
            DrawerItem(
                selected = false,
                icon = Icons.Default.Lock,
                label = stringResource(id = R.string.drawer_security_label),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isShowingSecuritySettings = true
                }
            )
            
            DrawerItem(
                selected = currentScreen == Screen.TRASH,
                icon = Icons.Default.Delete,
                label = stringResource(id = R.string.drawer_trash_label),
                onClick = { onScreenSelected(Screen.TRASH) }
            )

            DrawerItem(
                selected = false,
                icon = if (isActivated) Icons.Default.Verified else Icons.Default.Star,
                label = if (isActivated) stringResource(id = R.string.drawer_activate_pro_success) else stringResource(id = R.string.drawer_activate_pro),
                onClick = onActivateProClick
            )

            DrawerItem(
                selected = false,
                icon = Icons.Default.Refresh,
                label = stringResource(id = R.string.drawer_backup_label1),
                onClick = onBackupClick
            )
        }
        
        // Shared Footer / Developer info (always beautifully positioned)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = stringResource(id = R.string.drawer_app_version, versionName),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = stringResource(id = R.string.developer_credit),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ContactIcon(
                    icon = Icons.Default.Call,
                    onClick = {
                        dialPhoneNumber(context, supportPhoneNumber)
                    }
                )
                
                ContactIcon(
                    icon = Icons.Default.Share,
                    onClick = {
                        val msg = context.getString(R.string.whatsapp_contact_msg)
                        openWhatsAppChat(context, supportPhoneNumber, msg)
                    }
                )
            }
        }
    }

    if (isShowingCurrencySettings) {
        CurrencySettingsDialog(
            settings = settings,
            onSaveSettings = onSaveSettings,
            onDismiss = { isShowingCurrencySettings = false }
        )
    }

    if (isShowingBusinessProfile) {
        BusinessProfileDialog(
            onDismiss = { isShowingBusinessProfile = false }
        )
    }

    if (isShowingSecuritySettings) {
        SecurityDialog(
            settings = settings,
            viewModel = securityViewModel,
            onDismiss = { isShowingSecuritySettings = false }
        )
    }
}
