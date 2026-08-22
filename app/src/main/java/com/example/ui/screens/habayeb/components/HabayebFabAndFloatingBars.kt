package com.example.ui.screens.habayeb.components

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.data.local.entities.HabayebCustomer
import com.example.ui.helper.VibrationHelper
import kotlin.math.roundToInt

/**
 * مفاتيح حفظ تموضع وحالة زر الإدخال / الإضافة العائم
 */
object FloatingAddPrefsKeys {
    const val PREFS_NAME = "floating_add_fab_prefs"
    const val KEY_SIZE_LEVEL = "fab_size_level"
    const val KEY_RATIO_X = "fab_ratio_x"
    const val KEY_RATIO_Y = "fab_ratio_y"
    const val KEY_LOCKED = "KEY_ADD_FAB_LOCKED"
}

/**
 * زر الإدخال والإضافة العائم فائق الانسيابية والاحترافية (Free-Floating Add FAB)
 * - يتم قفله بشكل افتراضي بدون أي رموز قفل مزعجة.
 * - يتحرك بالضغط المطول مع السحب، وعند ترك الضغط يتقفل دائماً في موقعه الجديد.
 * - يتبع ثيم التطبيق الأساسي بالكامل ويدعم الوضعين النهاري والليلي بوضوح ممتاز.
 */
@Composable
fun HabayebFab(
    targetCustomer: HabayebCustomer?,
    contentPadding: PaddingValues,
    primaryColor: Color,
    containerColor: Color,
    haptic: HapticFeedback,
    onAddCustomerClick: () -> Unit,
    onAddTransactionForCustomer: (HabayebCustomer) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(FloatingAddPrefsKeys.PREFS_NAME, Context.MODE_PRIVATE)
    }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    // قراءة الإعدادات المحفوظة مع قيم افتراضية أنيقة ومريحة ليد المستخدم (افتراضي: الحجم المتوسط 1)
    val initialSizeLevel = remember(prefs) {
        prefs.getInt(FloatingAddPrefsKeys.KEY_SIZE_LEVEL, 1)
    }
    val hasSavedRatio = remember(prefs) {
        prefs.contains(FloatingAddPrefsKeys.KEY_RATIO_X) && prefs.contains(FloatingAddPrefsKeys.KEY_RATIO_Y)
    }
    val initialRatioX = remember(prefs, hasSavedRatio) {
        if (hasSavedRatio) prefs.getFloat(FloatingAddPrefsKeys.KEY_RATIO_X, -1f) else -1f
    }
    val initialRatioY = remember(prefs, hasSavedRatio) {
        if (hasSavedRatio) prefs.getFloat(FloatingAddPrefsKeys.KEY_RATIO_Y, -1f) else -1f
    }

    // إدارة مستوى الحجم (0: مجهري 40dp، 1: قياسي 52dp، 2: كبير ومريح 62dp)
    var sizeLevel by remember { mutableStateOf(initialSizeLevel) }
    val bubbleSize = when (sizeLevel) {
        0 -> 40.dp
        2 -> 62.dp
        else -> 52.dp
    }
    val iconSize = when (sizeLevel) {
        0 -> 20.dp
        2 -> 30.dp
        else -> 26.dp
    }

    val bubbleSizePx = with(density) { bubbleSize.toPx() }

    var ratioX by remember { mutableStateOf(initialRatioX) }
    var ratioY by remember { mutableStateOf(initialRatioY) }

    var isInteracting by remember { mutableStateOf(false) }

    // تحريكات انسيابية للتفاعل
    val scaleAnim by animateFloatAsState(
        targetValue = if (isInteracting) 1.10f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "FabScaleAnim"
    )

    // استخدام لون الثيم الرئيسي النقاطي للتطابق التام مع ثيم التطبيق
    val effectivePrimary = if (primaryColor != Color.Unspecified) primaryColor else MaterialTheme.colorScheme.primary

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .zIndex(25f)
        ) {
            val maxX = remember(screenWidthPx, bubbleSizePx) {
                (screenWidthPx - bubbleSizePx).coerceAtLeast(0f)
            }
            val maxY = remember(screenHeightPx, bubbleSizePx) {
                (screenHeightPx - bubbleSizePx).coerceAtLeast(0f)
            }

            // Default Center-Dock Position: Center X = 50%, Center Y = Resting over top edge of bottom navigation capsule
            val defaultDockX = remember(screenWidthPx, bubbleSizePx) {
                if (screenWidthPx > 0) (screenWidthPx - bubbleSizePx) / 2f else 0f
            }
            val defaultDockY = remember(screenHeightPx, bubbleSizePx) {
                if (screenHeightPx > 0) {
                    val capsuleBottomOffset = with(density) { 72.dp.toPx() }
                    (screenHeightPx - capsuleBottomOffset - (bubbleSizePx / 2f)).coerceIn(0f, maxY)
                } else 0f
            }

            val clampedX = remember(ratioX, screenWidthPx, bubbleSizePx, defaultDockX) {
                if (ratioX < 0f) defaultDockX else (ratioX * screenWidthPx).coerceIn(0f, maxX)
            }
            val clampedY = remember(ratioY, screenHeightPx, bubbleSizePx, defaultDockY) {
                if (ratioY < 0f) defaultDockY else (ratioY * screenHeightPx).coerceIn(0f, maxY)
            }

            Box(
                modifier = Modifier
                    .absoluteOffset { IntOffset(clampedX.roundToInt(), clampedY.roundToInt()) }
                    .size(bubbleSize)
                    .scale(scaleAnim)
                    .alpha(1.0f)
                    .shadow(
                        elevation = if (isInteracting) 14.dp else 6.dp,
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
                    .pointerInput(screenWidthPx, screenHeightPx, maxX, maxY, defaultDockX, defaultDockY) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                isInteracting = true
                                VibrationHelper.triggerSuccessVibration(context)
                            },
                            onDragEnd = {
                                isInteracting = false
                                if (ratioX >= 0f && ratioY >= 0f) {
                                    prefs.edit().apply {
                                        putFloat(FloatingAddPrefsKeys.KEY_RATIO_X, ratioX)
                                        putFloat(FloatingAddPrefsKeys.KEY_RATIO_Y, ratioY)
                                        putBoolean(FloatingAddPrefsKeys.KEY_LOCKED, true)
                                        apply()
                                    }
                                }
                                VibrationHelper.triggerSuccessVibration(context)
                            },
                            onDragCancel = {
                                isInteracting = false
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                isInteracting = true

                                val currentX = if (ratioX < 0f) defaultDockX else ratioX * screenWidthPx
                                val currentY = if (ratioY < 0f) defaultDockY else ratioY * screenHeightPx
                                val newX = (currentX + dragAmount.x).coerceIn(0f, maxX)
                                val newY = (currentY + dragAmount.y).coerceIn(0f, maxY)

                                ratioX = if (screenWidthPx > 0) newX / screenWidthPx else ratioX
                                ratioY = if (screenHeightPx > 0) newY / screenHeightPx else ratioY
                            }
                        )
                    }
                    .pointerInput(sizeLevel, targetCustomer) {
                        detectTapGestures(
                            onTap = {
                                VibrationHelper.triggerSuccessVibration(context)
                                if (targetCustomer != null) {
                                    onAddTransactionForCustomer(targetCustomer)
                                } else {
                                    onAddCustomerClick()
                                }
                            },
                            onDoubleTap = {
                                val newSizeLevel = (sizeLevel + 1) % 3
                                sizeLevel = newSizeLevel
                                prefs.edit().putInt(FloatingAddPrefsKeys.KEY_SIZE_LEVEL, newSizeLevel).apply()
                                VibrationHelper.triggerSuccessVibration(context)
                            }
                        )
                    }
                    .testTag("floating_add_fab"),
                contentAlignment = Alignment.Center
            ) {
                // أيقونة الإضافة المركزية الفاتنة
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(
                        id = if (targetCustomer != null) R.string.habayeb_add_tx_desc else R.string.habayeb_add_customer_fab
                    ),
                    modifier = Modifier.size(iconSize),
                    tint = Color.White
                )
            }
        }
    }
}
