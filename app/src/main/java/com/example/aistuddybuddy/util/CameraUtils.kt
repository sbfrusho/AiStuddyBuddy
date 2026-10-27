package com.example.aistuddybuddy.util

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.concurrent.futures.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun Context.getCameraProvider(): ProcessCameraProvider {
    return ProcessCameraProvider.getInstance(this).await()
}

/** Captures a single frame as an upright bitmap, decoding off the main thread. */
suspend fun ImageCapture.takePictureAsBitmap(maxSide: Int): Bitmap = suspendCancellableCoroutine { continuation ->
    takePicture(
        Dispatchers.Default.asExecutor(),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val result = runCatching {
                    image.use {
                        it.toBitmap()
                            .rotate(it.imageInfo.rotationDegrees)
                            .scaleDownTo(maxSide)
                    }
                }
                result.fold(continuation::resume, continuation::resumeWithException)
            }

            override fun onError(exception: ImageCaptureException) {
                continuation.resumeWithException(exception)
            }
        }
    )
}
