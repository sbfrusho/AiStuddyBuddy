package com.example.aistuddybuddy.util

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Base64
import androidx.compose.ui.geometry.Rect
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

fun Bitmap.rotate(degrees: Int): Bitmap {
    if (degrees % 360 == 0) return this
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

fun Bitmap.scaleDownTo(maxSide: Int): Bitmap {
    val longSide = maxOf(width, height)
    if (longSide <= maxSide) return this
    val scale = maxSide.toFloat() / longSide
    return Bitmap.createScaledBitmap(this, (width * scale).roundToInt(), (height * scale).roundToInt(), true)
}

/** Crops using a rect whose edges are fractions (0..1) of the bitmap's width and height. */
fun Bitmap.cropNormalized(rect: Rect): Bitmap {
    val left = (rect.left * width).roundToInt().coerceIn(0, width - 1)
    val top = (rect.top * height).roundToInt().coerceIn(0, height - 1)
    val right = (rect.right * width).roundToInt().coerceIn(left + 1, width)
    val bottom = (rect.bottom * height).roundToInt().coerceIn(top + 1, height)
    return Bitmap.createBitmap(this, left, top, right - left, bottom - top)
}

/** Scales the image down to a size vision models read well and encodes it as base64 JPEG. */
fun Bitmap.toJpegBase64(maxSide: Int = 1600, quality: Int = 85): String {
    val output = ByteArrayOutputStream()
    scaleDownTo(maxSide).compress(Bitmap.CompressFormat.JPEG, quality, output)
    return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
}
