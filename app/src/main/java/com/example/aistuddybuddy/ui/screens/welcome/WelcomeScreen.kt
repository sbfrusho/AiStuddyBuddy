package com.example.aistuddybuddy.ui.screens.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.aistuddybuddy.ui.components.SecureScreen
import com.example.aistuddybuddy.ui.screens.settings.ProviderSetupCard
import com.example.aistuddybuddy.ui.screens.settings.RecommendedProviders
import com.example.aistuddybuddy.ui.screens.settings.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * First-run flow: what the app does, then connecting one free AI. Students can skip and connect
 * later from Settings; either way [onDone] is called once and the welcome isn't shown again.
 */
@Composable
fun WelcomeScreen(
    onDone: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val finish = {
        viewModel.finishOnboarding()
        onDone()
    }
    when (page) {
        0 -> IntroPage(onNext = { page = 1 }, onSkip = finish)
        else -> ConnectPage(viewModel = viewModel, onDone = finish)
    }
}

@Composable
private fun IntroPage(onNext: () -> Unit, onSkip: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surface)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp)
        ) {
            Spacer(Modifier.weight(0.6f))
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(72.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("StudyBuddy AI", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Stuck on a question in your book? Get a clear, short answer in seconds.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(Modifier.height(32.dp))
            Step(Icons.Filled.PhotoCamera, "Snap the page", "Take a photo or pick one from your gallery.")
            Step(Icons.Filled.CropFree, "Select the question", "Drag a box around just the part you need.")
            Step(Icons.Filled.AutoAwesome, "Get the answer", "In English or বাংলা, with follow-ups like “Simpler”.")
            Spacer(Modifier.weight(1f))
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Get started")
            }
            TextButton(onClick = onSkip, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Skip for now")
            }
        }
    }
}

@Composable
private fun Step(icon: ImageVector, title: String, body: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerLowest, modifier = Modifier.size(44.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ConnectPage(viewModel: SettingsViewModel, onDone: () -> Unit) {
    SecureScreen()
    val providers by viewModel.providers.collectAsStateWithLifecycle()
    val anyConnected by viewModel.anyConnected.collectAsStateWithLifecycle()
    val recommended = providers.filter { it.provider in RecommendedProviders }

    Scaffold(
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Button(onClick = onDone, enabled = anyConnected, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text(if (anyConnected) "Start studying" else "Connect one AI to continue")
                }
                TextButton(onClick = onDone, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("I'll do this later")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 24.dp,
                bottom = padding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                    Text("Connect a free AI", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = "StudyBuddy uses your own free AI key, so there's nothing to pay. It takes about a " +
                            "minute. Pick any one; you can add more later in Settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
            items(recommended, key = { it.provider.name }) { state ->
                ProviderSetupCard(
                    state = state,
                    onDraftKeyChange = { viewModel.updateDraftKey(state.provider, it) },
                    onTestAndSave = { viewModel.testAndSave(state.provider) },
                    onSaveWithoutTesting = { viewModel.saveWithoutTesting(state.provider) },
                    onRemoveKey = { viewModel.removeKey(state.provider) },
                    onModelChange = { viewModel.updateModel(state.provider, it) },
                    initiallyExpanded = state.provider == RecommendedProviders.first(),
                    showModel = false
                )
            }
            item {
                Text(
                    text = "Your key is stored encrypted on this phone and only sent to that AI.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }
    }
}
