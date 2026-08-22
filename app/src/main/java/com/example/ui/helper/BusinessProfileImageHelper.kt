package com.example.ui.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object BusinessProfileImageHelper {

    fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            val maxDim = maxOf(options.outWidth, options.outHeight)
            var sampleSize = 1
            while (maxDim / sampleSize > 1200) {
                sampleSize *= 2
            }
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            var bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            val rotationDegrees = getExifOrientationDegrees(context, uri)
            if (rotationDegrees != 0f) {
                val rotated = rotateBitmap(bitmap, rotationDegrees)
                if (rotated != bitmap && !bitmap.isRecycled) {
                    bitmap.recycle()
                }
                bitmap = rotated
            }
            bitmap
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    private fun getExifOrientationDegrees(context: Context, uri: Uri): Float {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = android.media.ExifInterface(inputStream)
                when (exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL)) {
                    android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (t: Throwable) {
            0f
        }
    }

    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        if (bitmap.isRecycled || degrees % 360f == 0f) return bitmap
        return try {
            val matrix = Matrix().apply { postRotate(degrees) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (t: Throwable) {
            t.printStackTrace()
            bitmap
        }
    }

    fun cropWithTransform(
        bitmap: Bitmap,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        density: Float,
        isCircle: Boolean
    ): Bitmap {
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return bitmap
        return try {
            val kPx = 200f * density
            val centerPx = kPx / 2f

            val w = bitmap.width.toFloat()
            val h = bitmap.height.toFloat()
            val s0 = kPx / minOf(w, h).coerceAtLeast(1f)
            val wDraw = w * s0
            val hDraw = h * s0
            val x0 = (kPx - wDraw) / 2f
            val y0 = (kPx - hDraw) / 2f

            val safeScale = scale.coerceAtLeast(1f)

            val pTLx = (-offsetX - centerPx) / safeScale + centerPx
            val pTLy = (-offsetY - centerPx) / safeScale + centerPx
            val pBRx = (kPx - offsetX - centerPx) / safeScale + centerPx
            val pBRy = (kPx - offsetY - centerPx) / safeScale + centerPx

            val maxRight = (bitmap.width - 1).coerceAtLeast(0)
            val maxBottom = (bitmap.height - 1).coerceAtLeast(0)

            val leftPx = ((pTLx - x0) / s0).toInt().coerceIn(0, maxRight)
            val topPx = ((pTLy - y0) / s0).toInt().coerceIn(0, maxBottom)
            val rightPx = ((pBRx - x0) / s0).toInt().coerceIn(leftPx + 1, bitmap.width)
            val bottomPx = ((pBRy - y0) / s0).toInt().coerceIn(topPx + 1, bitmap.height)

            val cropW = (rightPx - leftPx).coerceIn(10, (bitmap.width - leftPx).coerceAtLeast(10))
            val cropH = (bottomPx - topPx).coerceIn(10, (bitmap.height - topPx).coerceAtLeast(10))

            val squareBitmap = Bitmap.createBitmap(bitmap, leftPx, topPx, cropW, cropH)

            if (!isCircle) {
                return squareBitmap
            }

            val size = minOf(squareBitmap.width, squareBitmap.height).coerceAtLeast(10)
            val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint().apply {
                isAntiAlias = true
                color = 0xff424242.toInt()
            }
            val rect = Rect(0, 0, size, size)
            canvas.drawARGB(0, 0, 0, 0)
            canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(squareBitmap, rect, rect, paint)
            if (squareBitmap != output && !squareBitmap.isRecycled) {
                squareBitmap.recycle()
            }
            output
        } catch (t: Throwable) {
            t.printStackTrace()
            bitmap
        }
    }

    fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return bitmap
        if (bitmap.width <= maxDimension && bitmap.height <= maxDimension) return bitmap
        return try {
            val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val newWidth: Int
            val newHeight: Int
            if (ratio > 1f) {
                newWidth = maxDimension
                newHeight = (maxDimension / ratio).toInt().coerceAtLeast(1)
            } else {
                newHeight = maxDimension
                newWidth = (maxDimension * ratio).toInt().coerceAtLeast(1)
            }
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } catch (t: Throwable) {
            t.printStackTrace()
            bitmap
        }
    }

    fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String? {
        if (bitmap.isRecycled) return null
        return try {
            val file = File(context.filesDir, "business_logo.png")
            if (file.exists()) {
                file.delete()
            }
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file.absolutePath
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }
}
