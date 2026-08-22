package com.example.ui.screens.habayeb.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.helper.HabayebMathHelper
import java.math.BigDecimal
import kotlinx.coroutines.android.awaitFrame

private const val PRIVACY_MASK = "*****"

@Composable
fun HabayebHeaderTopBar(
    isSearchActive: Boolean,
    onSearchActiveChanged: (Boolean) -> Unit,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onMenuClick: () -> Unit,
    haptic: HapticFeedback,
    netDebt: BigDecimal,
    isPrivacyMode: Boolean,
    onTogglePrivacy: () -> Unit,
    currencySymbol: String,
    onHeaderDoubleClick: () -> Unit = {},
    isFloatingActive: Boolean = false,
    onToggleFloatingClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(bottom = 8.dp)
    ) {
        if (isSearchActive) {
            HabayebSearchHeaderBar(
                searchQuery = searchQuery,
                onSearchQueryChanged = onSearchQueryChanged,
                onCloseSearch = {
                    onSearchQueryChanged("")
                    onSearchActiveChanged(false)
                },
                haptic = haptic
            )
        } else {
            HabayebNormalHeaderBar(
                netDebt = netDebt,
                isPrivacyMode = isPrivacyMode,
                currencySymbol = currencySymbol,
                isFloatingActive = isFloatingActive,
                onMenuClick = onMenuClick,
                onOpenSearch = { onSearchActiveChanged(true) },
                onTogglePrivacy = onTogglePrivacy,
                onHeaderDoubleClick = onHeaderDoubleClick,
                onToggleFloatingClick = onToggleFloatingClick,
                haptic = haptic
            )
        }
    }
}

@Composable
private fun HabayebSearchHeaderBar(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onCloseSearch: () -> Unit,
    haptic: HapticFeedback
) {
    val focusRequester = remember { FocusRequester() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(46.dp)
            .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(23.dp))
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Close Search Icon Button
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCloseSearch()
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(id = R.string.habayeb_close_search),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        // Search Input field in center
        BasicTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Right
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onPrimary),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
                .focusRequester(focusRequester),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = stringResource(id = R.string.habayeb_search_hint),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    innerTextField()
                }
            }
        )

        // Passive Search Icon
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
            modifier = Modifier.size(20.dp)
        )
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        try {
            awaitFrame()
            focusRequester.requestFocus()
            keyboardController?.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
private fun HabayebNormalHeaderBar(
    netDebt: BigDecimal,
    isPrivacyMode: Boolean,
    currencySymbol: String,
    isFloatingActive: Boolean,
    onMenuClick: () -> Unit,
    onOpenSearch: () -> Unit,
    onTogglePrivacy: () -> Unit,
    onHeaderDoubleClick: () -> Unit,
    onToggleFloatingClick: () -> Unit,
    haptic: HapticFeedback
) {
    val currentOnHeaderDoubleClick = rememberUpdatedState(onHeaderDoubleClick)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Right/Start Element: Menu icon button
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onMenuClick()
            },
            modifier = Modifier
                .size(38.dp)
                .background(Color.White.copy(alpha = 0.15f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = stringResource(id = R.string.ledger_nav_menu_desc),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        // Centered head title
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentOnHeaderDoubleClick.value()
                        },
                        onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentOnHeaderDoubleClick.value()
                        }
                    )
                }
        ) {
            val isPositiveOrZero = remember(netDebt) { netDebt.compareTo(BigDecimal.ZERO) >= 0 }
            val titleText = if (isPositiveOrZero) {
                stringResource(R.string.habayeb_net_total_for_you)
            } else {
                stringResource(R.string.habayeb_net_total_on_you)
            }

            val formattedBalanceText = remember(netDebt, isPrivacyMode, currencySymbol) {
                if (isPrivacyMode) {
                    PRIVACY_MASK
                } else {
                    val sign = if (netDebt.compareTo(BigDecimal.ZERO) < 0) "-" else ""
                    val bdNetDebt = netDebt.abs()
                    val formatted = HabayebMathHelper.formatSmart(bdNetDebt)
                    "$sign$formatted $currencySymbol"
                }
            }

            AnimatedContent(
                targetState = titleText,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220)) + slideInVertically(animationSpec = tween(220)) { height -> -height / 3 })
                        .togetherWith(fadeOut(animationSpec = tween(180)) + slideOutVertically(animationSpec = tween(180)) { height -> height / 3 })
                },
                label = "header_title_anim"
            ) { animatedTitle ->
                Text(
                    text = animatedTitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = onTogglePrivacy,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isPrivacyMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = stringResource(id = R.string.ledger_visibility_desc),
                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                AnimatedContent(
                    targetState = formattedBalanceText,
                    transitionSpec = {
                        if (targetState == PRIVACY_MASK || initialState == PRIVACY_MASK) {
                            fadeIn(animationSpec = tween(90)).togetherWith(fadeOut(animationSpec = tween(60)))
                        } else {
                            (fadeIn(animationSpec = tween(150)) + slideInVertically(animationSpec = tween(150)) { height -> height / 3 })
                                .togetherWith(fadeOut(animationSpec = tween(100)) + slideOutVertically(animationSpec = tween(100)) { height -> -height / 3 })
                        }
                    },
                    label = "header_balance_anim",
                    modifier = Modifier.weight(1f, fill = false)
                ) { animatedBalance ->
                    AutoSizeText(
                        text = animatedBalance,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1
                    )
                }
            }
        }

        // Left/End Element: Search and Floating Bubble Toggle row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TinyFloatingSearchToggle(
                isFloatingActive = isFloatingActive,
                activeThemeColor = MaterialTheme.colorScheme.onPrimary,
                onToggleClick = onToggleFloatingClick
            )
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenSearch()
                },
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(id = R.string.habayeb_search_label),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

