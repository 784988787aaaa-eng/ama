package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.data.local.entities.AppSettings
import com.example.ui.navigation.Screen
import com.example.ui.screens.*
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.HabayebFinanceViewModel
import com.example.ui.viewmodel.SecurityAndLicenseViewModel
import com.example.ui.viewmodel.BackupSyncViewModel

@Composable
fun MainAppContent(
    currentScreen: Screen,
    viewModel: FinanceViewModel,
    habayebViewModel: HabayebFinanceViewModel,
    securityViewModel: SecurityAndLicenseViewModel,
    backupSyncViewModel: BackupSyncViewModel,
    settings: AppSettings,
    contentPadding: PaddingValues = PaddingValues(),
    onNavigate: (Screen) -> Unit,
    onMenuClick: () -> Unit,
    onExit: () -> Unit,
    isDrawerOpen: Boolean = false,
    onHeaderDoubleClick: () -> Unit = {},
    isFloatingSearchActive: Boolean = false,
    onFloatingSearchActiveChanged: (Boolean) -> Unit = {},
    isSearchActive: Boolean = false,
    onSearchActiveChanged: (Boolean) -> Unit = {},
    isHistoryOverlayActive: Boolean = false,
    onHistoryOverlayActiveChanged: (Boolean) -> Unit = {},
    isHistorySearchActive: Boolean = false,
    onHistorySearchActiveChanged: (Boolean) -> Unit = {},
    onFabOverlayChanged: (((@Composable () -> Unit)?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        val premiumSpring = remember {
            spring<Float>(
                dampingRatio = 0.85f,
                stiffness = 350f
            )
        }
        val premiumOffsetSpring = remember {
            spring<IntOffset>(
                dampingRatio = 0.85f,
                stiffness = 350f
            )
        }

        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                val isInitialSub = initialState == Screen.SETTINGS || initialState == Screen.TRASH || initialState == Screen.BUSINESS_PROFILE || initialState == Screen.SECURITY
                val isTargetSub = targetState == Screen.SETTINGS || targetState == Screen.TRASH || targetState == Screen.BUSINESS_PROFILE || targetState == Screen.SECURITY

                if (isTargetSub && !isInitialSub) {
                    // Entering sub-screen: Slide UP from bottom + Fade in + Scale in
                    val slideIn = slideInVertically(animationSpec = premiumOffsetSpring) { it } +
                            fadeIn(animationSpec = premiumSpring) +
                            scaleIn(initialScale = 0.95f, animationSpec = premiumSpring)
                    val slideOut = fadeOut(animationSpec = premiumSpring) +
                            scaleOut(targetScale = 0.95f, animationSpec = premiumSpring)
                    slideIn togetherWith slideOut
                } else if (isInitialSub && !isTargetSub) {
                    // Exiting sub-screen: Slide DOWN to bottom + Fade out + Scale out
                    val slideIn = fadeIn(animationSpec = premiumSpring) +
                            scaleIn(initialScale = 0.95f, animationSpec = premiumSpring)
                    val slideOut = slideOutVertically(animationSpec = premiumOffsetSpring) { it } +
                            fadeOut(animationSpec = premiumSpring) +
                            scaleOut(targetScale = 0.95f, animationSpec = premiumSpring)
                    slideIn togetherWith slideOut
                } else {
                    // Lateral or sub-to-sub: Horizontal slide
                    val isForward = targetState.ordinal > initialState.ordinal
                    val slideIn = if (isForward) {
                        slideInHorizontally(animationSpec = premiumOffsetSpring) { width -> width } +
                        fadeIn(animationSpec = premiumSpring) +
                        scaleIn(initialScale = 0.95f, animationSpec = premiumSpring)
                    } else {
                        slideInHorizontally(animationSpec = premiumOffsetSpring) { width -> -width } +
                        fadeIn(animationSpec = premiumSpring) +
                        scaleIn(initialScale = 1.05f, animationSpec = premiumSpring)
                    }
                    val slideOut = if (isForward) {
                        slideOutHorizontally(animationSpec = premiumOffsetSpring) { width -> -width } +
                        fadeOut(animationSpec = premiumSpring) +
                        scaleOut(targetScale = 1.05f, animationSpec = premiumSpring)
                    } else {
                        slideOutHorizontally(animationSpec = premiumOffsetSpring) { width -> width } +
                        fadeOut(animationSpec = premiumSpring) +
                        scaleOut(targetScale = 0.95f, animationSpec = premiumSpring)
                    }
                    slideIn togetherWith slideOut
                }
            },
            label = "ScreenSwitch"
        ) { screen ->
            when (screen) {
                Screen.HABAYEB -> {
                    HabayebScreen(
                        viewModel = habayebViewModel,
                        securityViewModel = securityViewModel,
                        onMenuClick = onMenuClick,
                        onClose = onExit,
                        contentPadding = contentPadding,
                        isDrawerOpen = isDrawerOpen,
                        onHeaderDoubleClick = onHeaderDoubleClick,
                        isFloatingSearchActive = isFloatingSearchActive,
                        onFloatingSearchActiveChanged = onFloatingSearchActiveChanged,
                        isSearchActive = isSearchActive,
                        onSearchActiveChanged = onSearchActiveChanged,
                        isHistoryOverlayActive = isHistoryOverlayActive,
                        onHistoryOverlayActiveChanged = onHistoryOverlayActiveChanged,
                        isHistorySearchActive = isHistorySearchActive,
                        onHistorySearchActiveChanged = onHistorySearchActiveChanged,
                        onFabOverlayChanged = onFabOverlayChanged
                    )
                }
                Screen.LEDGER -> {
                    MainLedgerView(
                        viewModel = viewModel,
                        habayebViewModel = habayebViewModel,
                        securityViewModel = securityViewModel,
                        settings = settings,
                        onBackIntercept = {},
                        onMenuClick = onMenuClick,
                        isDrawerOpen = isDrawerOpen,
                        isFloatingSearchActive = isFloatingSearchActive,
                        onFloatingSearchActiveChanged = onFloatingSearchActiveChanged,
                        isSearchActive = isSearchActive,
                        onSearchActiveChanged = onSearchActiveChanged,
                        contentPadding = contentPadding
                    )
                }
                Screen.SETTINGS -> {
                    SettingsView(
                        viewModel = viewModel,
                        habayebViewModel = habayebViewModel,
                        backupSyncViewModel = backupSyncViewModel,
                        settings = settings,
                        onNavigateToSecurity = { onNavigate(Screen.SECURITY) },
                        contentPadding = contentPadding
                    )
                }
                Screen.TRASH -> {
                    TrashScreen(
                        viewModel = viewModel,
                        habayebViewModel = habayebViewModel,
                        onBack = { onNavigate(Screen.HABAYEB) },
                        contentPadding = contentPadding
                    )
                }
                Screen.BUSINESS_PROFILE -> {
                    BusinessProfileScreen(
                        onBack = { onNavigate(Screen.HABAYEB) },
                        contentPadding = contentPadding
                    )
                }
                Screen.SECURITY -> {
                    SecurityScreen(
                        settings = settings,
                        viewModel = securityViewModel,
                        onBack = { onNavigate(Screen.LEDGER) },
                        contentPadding = contentPadding
                    )
                }
            }
        }
    }
}
