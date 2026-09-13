package com.example.aistuddybuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.aistuddybuddy.ui.screens.main.components.CameraPreview
import com.example.aistuddybuddy.ui.theme.AiStuddyBuddyTheme
import org.koin.androidx.compose.KoinAndroidContext
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KoinAndroidContext {
                MaterialTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        val viewModel: MainViewModel = koinViewModel()
                        val uiState by viewModel.uiState.collectAsState()
                        DiStatusScreen(uiState = uiState)
                    }
                }
            }
        }

    }
}

@Composable
fun DiStatusScreen(uiState: DiStatusUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "StudyBuddy AI",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Phase 1 — Core DI Bootstrap Check",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (uiState.isSuccess) {
                    Color(0xFFDFF7DF)
                } else {
                    Color(0xFFEA8B8B)
                }
            ),
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (uiState.isSuccess) {
                        Icons.Filled.CheckCircle
                    } else {
                        Icons.Filled.Warning
                    },
                    contentDescription = null,
                    tint = if (uiState.isSuccess) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.statusMessage,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (uiState.isSuccess) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }
        }
    }
}