package com.example.aistuddybuddy.ui.screens.crop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.aistuddybuddy.ui.screens.questions.ScanViewModel

private val DefaultSelection = Rect(0.08f, 0.3f, 0.92f, 0.6f)
private val FullSelection = Rect(0f, 0f, 1f, 1f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropScreen(
    viewModel: ScanViewModel,
    onBack: () -> Unit,
    onQuestionsExtracted: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val image = uiState.image
    val snackbarHostState = remember { SnackbarHostState() }
    var cropRect by remember { mutableStateOf(DefaultSelection) }

    LaunchedEffect(image) {
        // Nothing to crop (e.g. the process was recreated); go back to the camera.
        if (image == null && !uiState.isBusy) onBack()
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
                title = { Text("Select the question") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { cropRect = FullSelection }) {
                        Icon(Icons.Filled.Fullscreen, contentDescription = "Select whole page")
                    }
                    IconButton(onClick = viewModel::rotateImage, enabled = !uiState.isBusy) {
                        Icon(Icons.Filled.Rotate90DegreesCw, contentDescription = "Rotate")
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
                    .background(Color.Black)
            ) {
                if (image != null) {
                    CropImage(
                        bitmap = image,
                        cropRect = cropRect,
                        onCropRectChange = { cropRect = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Drag the box or its edges around the question and its options. " +
                        "You can include several questions at once.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(12.dp))
                Button(
                    onClick = { viewModel.extractQuestions(cropRect, onSuccess = onQuestionsExtracted) },
                    enabled = image != null && !uiState.isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (uiState.isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Reading text…")
                        } else {
                            Icon(Icons.Filled.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Extract questions")
                        }
                    }
                }
            }
        }
    }
}
