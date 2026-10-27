package com.example.aistuddybuddy.ui.screens.camera

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.aistuddybuddy.util.getCameraProvider

private const val TAG = "CameraPreview"

@Composable
fun CameraPreview(
    imageCapture: ImageCapture,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(previewView, imageCapture) {
        try {
            val cameraProvider = context.getCameraProvider()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )
            Log.d(TAG, "Camera preview started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting camera preview: ${e.message}")
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { previewView }
    )
}
