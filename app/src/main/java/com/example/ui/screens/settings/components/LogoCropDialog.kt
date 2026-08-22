package com.example.ui.screens.settings.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.gestures.detectTransformGestures
import com.example.R

@Composable
fun LogoCropDialog(
    editingBitmap: Bitmap,
    cropShapeIsCircle: Boolean,
    onCropShapeChange: (Boolean) -> Unit,
    activeThemeColor: Color,
    onRotate: () -> Unit,
    onDismiss: () -> Unit,
    onApply: (scale: Float, offsetX: Float, offsetY: Float) -> Unit
) {
    val density = LocalDensity.current.density
    val kPx = 200f * density
    val centerPx = kPx / 2f

    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val w = editingBitmap.width.toFloat()
    val h = editingBitmap.height.toFloat()
    val s0 = kPx / Math.min(w, h)
    val wDraw = w * s0
    val hDraw = h * s0
    val x0 = (kPx - wDraw) / 2f
    val y0 = (kPx - hDraw) / 2f

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.biz_crop_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant, if (cropShapeIsCircle) CircleShape else RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, if (cropShapeIsCircle) CircleShape else RoundedCornerShape(8.dp))
                        .clip(if (cropShapeIsCircle) CircleShape else RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = editingBitmap.asImageBitmap(),
                        contentDescription = stringResource(id = R.string.biz_crop_preview_desc),
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 5f)
                                    val maxTx = (centerPx - x0) * scale - centerPx
                                    val minTx = centerPx - (x0 + wDraw - centerPx) * scale
                                    val maxTy = (centerPx - y0) * scale - centerPx
                                    val minTy = centerPx - (y0 + hDraw - centerPx) * scale
                                    offsetX = (offsetX + pan.x).coerceIn(minTx, maxTx)
                                    offsetY = (offsetY + pan.y).coerceIn(minTy, maxTy)
                                }
                            },
                        contentScale = ContentScale.Crop
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.biz_zoom_and_pan),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(java.util.Locale.ENGLISH, "%.1fx", scale),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeThemeColor
                        )
                    }
                    Slider(
                        value = scale,
                        onValueChange = { newScale ->
                            scale = newScale
                            val maxTx = (centerPx - x0) * scale - centerPx
                            val minTx = centerPx - (x0 + wDraw - centerPx) * scale
                            val maxTy = (centerPx - y0) * scale - centerPx
                            val minTy = centerPx - (y0 + hDraw - centerPx) * scale
                            offsetX = offsetX.coerceIn(minTx, maxTx)
                            offsetY = offsetY.coerceIn(minTy, maxTy)
                        },
                        valueRange = 1f..5f,
                        colors = SliderDefaults.colors(
                            thumbColor = activeThemeColor,
                            activeTrackColor = activeThemeColor,
                            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onCropShapeChange(false) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!cropShapeIsCircle) activeThemeColor else MaterialTheme.colorScheme.outlineVariant,
                            contentColor = if (!cropShapeIsCircle) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = stringResource(id = R.string.biz_crop_square), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onCropShapeChange(true) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (cropShapeIsCircle) activeThemeColor else MaterialTheme.colorScheme.outlineVariant,
                            contentColor = if (cropShapeIsCircle) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = stringResource(id = R.string.biz_crop_circle), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            onRotate()
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.outlineVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(id = R.string.biz_rotate), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = stringResource(id = R.string.biz_btn_cancel), fontSize = 13.sp)
                    }

                    Button(
                        onClick = { onApply(scale, offsetX, offsetY) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = activeThemeColor)
                    ) {
                        Text(text = stringResource(id = R.string.biz_btn_apply), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
