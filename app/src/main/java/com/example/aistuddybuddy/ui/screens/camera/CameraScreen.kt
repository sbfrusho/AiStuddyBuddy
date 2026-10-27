package com.example.aistuddybuddy.ui.screens.camera

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.aistuddybuddy.data.image.BitmapLoader
import com.example.aistuddybuddy.ui.screens.questions.ScanViewModel
import com.example.aistuddybuddy.util.takePictureAsBitmap
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    viewModel: ScanViewModel,
    onImageReady: () -> Unit,
    onOpenQuestions: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var isCapturing by remember { mutableStateOf(false) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) viewModel.loadImage(uri, onLoaded = onImageReady)
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("StudyBuddy AI") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    cameraPermissionState.status.isGranted -> {
                        CameraPreview(imageCapture = imageCapture, modifier = Modifier.fillMaxSize())
                        Text(
                            text = "Fit the page in view. You'll select the question next.",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(16.dp)
                                .background(Color.Black.copy(alpha = 0.5f), MaterialTheme.shapes.medium)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }

                    else -> PermissionContent(
                        showRationale = cameraPermissionState.status.shouldShowRationale,
                        onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
                    )
                }

                if (uiState.isBusy || isCapturing) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }

            CaptureControls(
                questionCount = uiState.questions.size,
                captureEnabled = cameraPermissionState.status.isGranted && !isCapturing && !uiState.isBusy,
                onPickFromGallery = {
                    pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onCapture = {
                    scope.launch {
                        isCapturing = true
                        try {
                            val bitmap = imageCapture.takePictureAsBitmap(BitmapLoader.MAX_SIDE)
                            viewModel.onImageCaptured(bitmap)
                            onImageReady()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Couldn't take the photo. Please try again.")
                        } finally {
                            isCapturing = false
                        }
                    }
                },
                onOpenQuestions = onOpenQuestions
            )
        }
    }
}

@Composable
private fun CaptureControls(
    questionCount: Int,
    captureEnabled: Boolean,
    onPickFromGallery: () -> Unit,
    onCapture: () -> Unit,
    onOpenQuestions: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 32.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPickFromGallery, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Filled.PhotoLibrary, contentDescription = "Pick from gallery", modifier = Modifier.size(32.dp))
        }

        FilledIconButton(
            onClick = onCapture,
            enabled = captureEnabled,
            modifier = Modifier
                .size(76.dp)
                .border(4.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape),
            colors = IconButtonDefaults.filledIconButtonColors()
        ) {
            Icon(Icons.Filled.PhotoCamera, contentDescription = "Take photo", modifier = Modifier.size(36.dp))
        }

        IconButton(onClick = onOpenQuestions, enabled = questionCount > 0, modifier = Modifier.size(56.dp)) {
            BadgedBox(badge = { if (questionCount > 0) Badge { Text("$questionCount") } }) {
                Icon(Icons.Filled.Quiz, contentDescription = "Questions", modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
private fun PermissionContent(showRationale: Boolean, onRequestPermission: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.PhotoCamera,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (showRationale) {
                "StudyBuddy needs the camera to scan questions from your book."
            } else {
                "Camera access is off. Allow it to scan pages, or pick a photo from your gallery below."
            },
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequestPermission) {
            Text("Allow camera")
        }
        if (!showRationale) {
            OutlinedButton(onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                )
            }) {
                Text("Open app settings")
            }
        }
    }
}
