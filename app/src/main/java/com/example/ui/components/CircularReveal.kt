package com.example.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import kotlin.math.hypot

/**
 * A highly optimized circular reveal modifier that caches the Path object to minimize
 * memory allocation and GC churn during animation frames.
 */
fun Modifier.circularReveal(
    progress: Float,
    centerOffset: Offset,
    isRelative: Boolean = false
): Modifier = this.drawWithCache {
    val path = Path()
    onDrawWithContent {
        val maxRadius = hypot(size.width, size.height)
        val radius = maxRadius * progress
        val cx = if (isRelative) size.width * centerOffset.x else centerOffset.x
        val cy = centerOffset.y
        path.reset()
        path.addOval(Rect(cx - radius, cy - radius, cx + radius, cy + radius))
        clipPath(path) {
            this@onDrawWithContent.drawContent()
        }
    }
}
