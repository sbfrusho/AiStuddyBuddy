package com.example.aistuddybuddy.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.example.aistuddybuddy.util.scaleDownTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Decodes gallery images into software bitmaps small enough to crop and OCR comfortably. */
class BitmapLoader(private val context: Context) {

    suspend fun load(uri: Uri): Bitmap = withContext(Dispatchers.IO) {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            // ImageDecoder applies EXIF rotation for us.
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val longSide = maxOf(info.size.width, info.size.height)
                if (longSide > MAX_SIDE * 2) decoder.setTargetSampleSize(longSide / MAX_SIDE)
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
            val longSide = maxOf(bounds.outWidth, bounds.outHeight)
            val options = BitmapFactory.Options().apply {
                inSampleSize = if (longSide > MAX_SIDE * 2) longSide / MAX_SIDE else 1
            }
            context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
                ?: error("Could not open this image.")
        }
        bitmap.scaleDownTo(MAX_SIDE)
    }

    companion object {
        const val MAX_SIDE = 2560
    }
}
