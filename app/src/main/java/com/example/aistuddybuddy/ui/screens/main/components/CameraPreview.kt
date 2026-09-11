package com.example.aistuddybuddy.ui.screens.main.components

import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.aistuddybuddy.getCameraProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPreview (
    modifier: Modifier = Modifier,
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FIT_CENTER,
    onUseCaseConfigured: (Preview) -> Unit = {}
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    if(cameraPermissionState.status.isGranted) {
        val previewView = remember { PreviewView(context).apply { this.scaleType = scaleType } }
        val previewUseCase = remember { Preview.Builder().build() }

        LaunchedEffect(Unit) {
            val cameraProvider = context.getCameraProvider()
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    previewUseCase
                )
                previewUseCase.setSurfaceProvider(previewView.surfaceProvider)
                onUseCaseConfigured(previewUseCase)
            }catch (e: Exception) {
                Log.e("CameraPreview", "Error binding camera use case", e)
            }

        }

        AndroidView(
            factory = {previewView},
            modifier = modifier.fillMaxSize()
        )
    } else {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if(cameraPermissionState.status.shouldShowRationale){
                Toast.makeText(context, "Camera permission is required to use this feature.", Toast.LENGTH_LONG).show()
            } else {
                LaunchedEffect(Unit) {
                    cameraPermissionState.launchPermissionRequest()
                }
                Toast.makeText(context, "Requesting camera permission.", Toast.LENGTH_LONG).show()
            }
        }
    }

}