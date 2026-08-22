package com.example.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.hypot

// Circular Reveal Shape for Liquid Morphing Effect
class CircularRevealShape(
    val progress: Float,
    val centerOffset: Offset,
    val isRelative: Boolean = false
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val maxRadius = hypot(size.width, size.height)
        val radius = maxRadius * progress
        val cx = if (isRelative) size.width * centerOffset.x else centerOffset.x
        val cy = centerOffset.y
        val path = Path().apply {
            addOval(Rect(cx - radius, cy - radius, cx + radius, cy + radius))
        }
        return Outline.Generic(path)
    }
}
