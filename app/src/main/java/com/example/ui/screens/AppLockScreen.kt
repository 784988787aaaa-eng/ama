package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.BiometricAuthHelper
import com.example.domain.DatabaseSecurityGuard
import com.example.domain.HashUtils
import com.example.ui.screens.security.lock.LockHapticHelper
import com.example.ui.screens.security.lock.LockHapticType
import com.example.ui.screens.security.lock.PasscodeKeypadContent
import com.example.ui.screens.security.lock.RecoveryPhraseContent
import com.example.ui.theme.DarkBackground
import com.example.ui.viewmodel.SecurityAndLicenseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * App Lock Screen Facade coordinating authentication (PIN Passcode, Biometrics, Recovery Phrase).
 * Maintains a clean decoupled architecture and strict zero-leakage security lifecycle.
 */
@Composable
fun AppLockScreen(
    viewModel: SecurityAndLicenseViewModel,
    onUnlockSuccess: () -> Unit,
    onUnlockBypassedAndDisabled: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val vibrator = remember(context) { LockHapticHelper.getVibrator(context) }

    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()
    val isBiometricSupported = remember(context) { BiometricAuthHelper.isBiometricAvailable(context) }

    var enteredPasscode by remember { mutableStateOf("") }
    var isCheckingPasscode by remember { mutableStateOf(false) }
    var showRecoveryView by remember { mutableStateOf(false) }
    var recoveryPhraseInput by remember { mutableStateOf("") }
    var showHintText by remember { mutableStateOf(false) }
    val recoveryHint = settings.recoveryHint

    val shakeOffset = remember { Animatable(0f) }

    val currentEnteredPasscode by rememberUpdatedState(enteredPasscode)
    val currentIsCheckingPasscode by rememberUpdatedState(isCheckingPasscode)
    val currentPasscodeHash by rememberUpdatedState(settings.passcodeHash.orEmpty())

    val triggerErrorAnimationAndHaptic = {
        scope.launch {
            LockHapticHelper.performLockHaptic(vibrator, LockHapticType.ERROR)
            shakeOffset.animateTo(24f, tween(40))
            shakeOffset.animateTo(-24f, tween(40))
            shakeOffset.animateTo(16f, tween(35))
            shakeOffset.animateTo(-16f, tween(35))
            shakeOffset.animateTo(8f, tween(30))
            shakeOffset.animateTo(0f, tween(30))
        }
        Unit
    }

    val triggerBiometricPrompt = {
        val activity = context as? FragmentActivity
        if (activity != null && isBiometricSupported) {
            BiometricAuthHelper.authenticate(
                activity = activity,
                title = context.getString(R.string.lock_ledger_locked),
                subtitle = context.getString(R.string.lock_enter_pin_prompt),
                negativeButtonText = context.getString(R.string.lock_cancel_btn),
                onSuccess = {
                    LockHapticHelper.performLockHaptic(vibrator, LockHapticType.SUCCESS)
                    onUnlockSuccess()
                },
                onError = { _, _ -> },
                onFailed = {
                    LockHapticHelper.performLockHaptic(vibrator, LockHapticType.ERROR)
                }
            )
        }
    }

    // Auto-launch Biometric prompt on screen appearance if enabled and supported
    LaunchedEffect(isBiometricSupported, isBiometricEnabled) {
        if (isBiometricSupported && isBiometricEnabled && !showRecoveryView) {
            delay(200)
            triggerBiometricPrompt()
        }
    }

    val onKeyPress = remember(vibrator) {
        { key: String ->
            if (!currentIsCheckingPasscode && currentEnteredPasscode.length < 4) {
                LockHapticHelper.performLockHaptic(vibrator, LockHapticType.KEYPRESS)
                val nextPasscode = currentEnteredPasscode + key
                enteredPasscode = nextPasscode
                if (nextPasscode.length == 4) {
                    isCheckingPasscode = true
                    scope.launch {
                        val passChars = nextPasscode.toCharArray()
                        val isMatch = withContext(Dispatchers.Default) {
                            try {
                                val hashed = HashUtils.hashString(String(passChars))
                                DatabaseSecurityGuard.secureEqual(hashed, currentPasscodeHash)
                            } finally {
                                HashUtils.wipeCharArray(passChars)
                            }
                        }
                        if (isMatch) {
                            LockHapticHelper.performLockHaptic(vibrator, LockHapticType.SUCCESS)
                            onUnlockSuccess()
                        } else {
                            triggerErrorAnimationAndHaptic()
                            val fallbackMsg = context.getString(R.string.lock_incorrect_pin)
                            Toast.makeText(context, fallbackMsg, Toast.LENGTH_SHORT).show()
                            enteredPasscode = ""
                            isCheckingPasscode = false
                        }
                    }
                }
            }
        }
    }

    val onDeleteClick = {
        if (!isCheckingPasscode) {
            LockHapticHelper.performLockHaptic(vibrator, LockHapticType.KEYPRESS)
            if (enteredPasscode.isNotEmpty()) {
                enteredPasscode = enteredPasscode.dropLast(1)
            }
        }
    }

    val onForgotClick = {
        LockHapticHelper.performLockHaptic(vibrator, LockHapticType.KEYPRESS)
        showRecoveryView = true
    }

    val onVerifyRecoveryPhrase = {
        scope.launch {
            val recoveryChars = recoveryPhraseInput.trim().toCharArray()
            val isCorrect = withContext(Dispatchers.Default) {
                try {
                    val hashed = HashUtils.hashString(String(recoveryChars))
                    DatabaseSecurityGuard.secureEqual(hashed, settings.recoveryPhraseHash)
                } finally {
                    HashUtils.wipeCharArray(recoveryChars)
                }
            }
            if (isCorrect) {
                LockHapticHelper.performLockHaptic(vibrator, LockHapticType.SUCCESS)
                keyboardController?.hide()
                focusManager.clearFocus()
                val successMsg = context.getString(R.string.lock_recovery_matched)
                Toast.makeText(context, successMsg, Toast.LENGTH_LONG).show()
                onUnlockBypassedAndDisabled()
            } else {
                LockHapticHelper.performLockHaptic(vibrator, LockHapticType.ERROR)
                val errorMsg = context.getString(R.string.lock_recovery_wrong)
                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
            }
        }
        Unit
    }

    val onReturnToKeypadClick = {
        LockHapticHelper.performLockHaptic(vibrator, LockHapticType.KEYPRESS)
        keyboardController?.hide()
        focusManager.clearFocus()
        recoveryPhraseInput = ""
        showRecoveryView = false
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = DarkBackground
    ) {
        AnimatedContent(
            targetState = showRecoveryView,
            transitionSpec = {
                slideInHorizontally { width -> if (targetState) width else -width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> if (targetState) -width else width } + fadeOut()
            },
            label = "ScreenType"
        ) { isRecovery ->
            if (isRecovery) {
                RecoveryPhraseContent(
                    recoveryPhraseInput = recoveryPhraseInput,
                    onRecoveryPhraseChange = { recoveryPhraseInput = it },
                    recoveryHint = recoveryHint,
                    showHintText = showHintText,
                    onToggleHint = { showHintText = !showHintText },
                    onVerifyClick = onVerifyRecoveryPhrase,
                    onReturnToKeypadClick = onReturnToKeypadClick
                )
            } else {
                PasscodeKeypadContent(
                    enteredPasscode = enteredPasscode,
                    isCheckingPasscode = isCheckingPasscode,
                    shakeOffsetPx = shakeOffset.value,
                    isBiometricSupported = isBiometricSupported,
                    onKeyPress = onKeyPress,
                    onDeleteClick = onDeleteClick,
                    onForgotClick = onForgotClick,
                    onBiometricClick = {
                        LockHapticHelper.performLockHaptic(vibrator, LockHapticType.KEYPRESS)
                        triggerBiometricPrompt()
                    }
                )
            }
        }
    }
}
