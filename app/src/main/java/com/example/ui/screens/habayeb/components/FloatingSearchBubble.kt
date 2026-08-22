package com.example.ui.screens.habayeb.components

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.ui.helper.VibrationHelper
import kotlin.math.roundToInt

private object BubblePrefsKeys {
    const val PREFS_NAME = "floating_search_prefs"
    const val KEY_SIZE_LEVEL = "bubble_size_level"
    const val KEY_RATIO_X = "bubble_ratio_x"
    const val KEY_RATIO_Y = "bubble_ratio_y"
    const val KEY_LOCKED = "KEY_SEARCH_BUTTON_LOCKED"
}

/**
 * زر التنشيط والتفعيل المجهري في شريط العنوان العلوي (The Tiny Toggle Button)
 * - حجم مجهري وأنيق للغاية لمنع تشويه الواجهة.
 */
@Composable
fun TinyFloatingSearchToggle(
    isFloatingActive: Boolean,
    activeThemeColor: Color,
    onToggleClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(
                color = if (isFloatingActive) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.15f),
                shape = CircleShape
            )
            .border(
                width = 0.5.dp,
                color = if (isFloatingActive) Color.White.copy(alpha = 0.45f) else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onToggleClick),
        contentAlignment = Alignment.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(id = R.string.floating_search_toggle),
                tint = if (isFloatingActive) Color.White else Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(13.dp)
            )
            if (isFloatingActive) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .align(Alignment.TopEnd)
                        .background(Color.White, CircleShape)
                )
            }
        }
    }
}

/**
 * فقاعة البحث العائمة فائقة الانسيابية (Absolute Free Floating Search Bubble)
 * - مقفلة بشكل افتراضي وتتحرك حصراً بالضغط المطول مع السحب.
 * - تنظيف تام لرموز وأكواد القفل المزعجة لتطابق ثيم التطبيق بالكامل.
 */
@Composable
fun FloatingSearchBubble(
    activeThemeColor: Color,
    onSearchClick: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(BubblePrefsKeys.PREFS_NAME, Context.MODE_PRIVATE) }
    
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val initialSizeLevel = remember(prefs) { prefs.getInt(BubblePrefsKeys.KEY_SIZE_LEVEL, 1) }
    val initialRatioX = remember(prefs) { prefs.getFloat(BubblePrefsKeys.KEY_RATIO_X, 0.80f).coerceIn(0f, 1f) }
    val initialRatioY = remember(prefs) { prefs.getFloat(BubblePrefsKeys.KEY_RATIO_Y, 0.70f).coerceIn(0f, 1f) }

    var sizeLevel by remember { mutableStateOf(initialSizeLevel) }
    val bubbleSize = when (sizeLevel) {
        0 -> 38.dp
        2 -> 58.dp
        else -> 48.dp
    }
    val searchIconSize = when (sizeLevel) {
        0 -> 18.dp
        2 -> 26.dp
        else -> 22.dp
    }
    val bubbleSizePx = with(density) { bubbleSize.toPx() }

    var ratioX by remember { mutableStateOf(initialRatioX) }
    var ratioY by remember { mutableStateOf(initialRatioY) }

    var isInteracting by remember { mutableStateOf(false) }

    val scaleAnim by animateFloatAsState(
        targetValue = if (isInteracting) 1.10f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "SearchBubbleScaleAnim"
    )

    val effectivePrimary = if (activeThemeColor != Color.Unspecified) activeThemeColor else MaterialTheme.colorScheme.primary

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(25f)
        ) {
            val maxX = remember(screenWidthPx, bubbleSizePx) { (screenWidthPx - bubbleSizePx).coerceAtLeast(0f) }
            val maxY = remember(screenHeightPx, bubbleSizePx) { (screenHeightPx - bubbleSizePx).coerceAtLeast(0f) }
            
            val clampedX = (ratioX * screenWidthPx).coerceIn(0f, maxX)
            val clampedY = (ratioY * screenHeightPx).coerceIn(0f, maxY)

            Box(
                modifier = Modifier
                    .absoluteOffset { IntOffset(clampedX.roundToInt(), clampedY.roundToInt()) }
                    .size(bubbleSize)
                    .scale(scaleAnim)
                    .shadow(
                        elevation = if (isInteracting) 12.dp else 6.dp,
                        shape = CircleShape,
                        spotColor = effectivePrimary.copy(alpha = 0.5f)
                    )
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                effectivePrimary,
                                effectivePrimary.copy(alpha = 0.88f)
                            )
                        )
                    )
                    .border(
                        width = 1.2.dp,
                        color = Color.White.copy(alpha = if (isInteracting) 0.9f else 0.45f),
                        shape = CircleShape
                    )
                    .pointerInput(screenWidthPx, screenHeightPx, maxX, maxY) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                isInteracting = true
                                VibrationHelper.triggerSuccessVibration(context)
                            },
                            onDragEnd = {
                                isInteracting = false
                                prefs.edit().apply {
                                    putFloat(BubblePrefsKeys.KEY_RATIO_X, ratioX)
                                    putFloat(BubblePrefsKeys.KEY_RATIO_Y, ratioY)
                                    putBoolean(BubblePrefsKeys.KEY_LOCKED, true)
                                    apply()
                                }
                                VibrationHelper.triggerSuccessVibration(context)
                            },
                            onDragCancel = {
                                isInteracting = false
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                isInteracting = true
                                
                                val currentX = ratioX * screenWidthPx
                                val currentY = ratioY * screenHeightPx
                                val newX = (currentX + dragAmount.x).coerceIn(0f, maxX)
                                val newY = (currentY + dragAmount.y).coerceIn(0f, maxY)
                                
                                ratioX = if (screenWidthPx > 0) newX / screenWidthPx else ratioX
                                ratioY = if (screenHeightPx > 0) newY / screenHeightPx else ratioY
                            }
                        )
                    }
                    .pointerInput(sizeLevel) {
                        detectTapGestures(
                            onTap = {
                                VibrationHelper.triggerSuccessVibration(context)
                                onSearchClick()
                            },
                            onDoubleTap = {
                                val newSizeLevel = (sizeLevel + 1) % 3
                                sizeLevel = newSizeLevel
                                prefs.edit().putInt(BubblePrefsKeys.KEY_SIZE_LEVEL, newSizeLevel).apply()
                                VibrationHelper.triggerSuccessVibration(context)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(id = R.string.floating_search_icon),
                    tint = Color.White,
                    modifier = Modifier.size(searchIconSize)
                )
            }
        }
    }
}
