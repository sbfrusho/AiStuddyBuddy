package com.example.aistuddybuddy.ui.screens.main.components

import android.util.Log
import android.widget.FrameLayout
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.aistuddybuddy.getCameraProvider
import com.google.android.gms.common.internal.zzag
import kotlinx.coroutines.flow.MutableStateFlow

private val TAG = "CameraPreview"

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onPreviewReady: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewView = remember { PreviewView(context) }

    LaunchedEffect(previewView) {
        try {
            val cameraProvider = context.getCameraProvider()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview
            )
            Log.d(TAG, "Camera preview started")
            onPreviewReady(true)
        }catch (e: Exception) {
            Log.e(TAG, "Error starting camera preview: ${e.message}")
            onPreviewReady(false)
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { previewView
        }
    )
}