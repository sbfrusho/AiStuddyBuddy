package com.example.aistuddybuddy.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.aistuddybuddy.domain.model.AnswerLanguage
import com.example.aistuddybuddy.domain.model.AppPreferences
import com.example.aistuddybuddy.ui.components.SecureScreen
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    SecureScreen()
    val providers by viewModel.providers.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val (paid, free) = providers.partition { it.provider.isPaidOnly }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                PreferencesCard(
                    preferences = preferences,
                    onAnswerLanguageChange = viewModel::updateAnswerLanguage,
                    onReadWithAiChange = viewModel::updateReadWithAi
                )
            }

            item {
                SectionHeader(
                    title = "Free AIs",
                    subtitle = "No card needed. Connect two or three, and the app switches automatically when one is busy."
                )
            }
            items(free, key = { it.provider.name }) { state ->
                ProviderSetupCard(
                    state = state,
                    onDraftKeyChange = { viewModel.updateDraftKey(state.provider, it) },
                    onTestAndSave = { viewModel.testAndSave(state.provider) },
                    onSaveWithoutTesting = { viewModel.saveWithoutTesting(state.provider) },
                    onRemoveKey = { viewModel.removeKey(state.provider) },
                    onModelChange = { viewModel.updateModel(state.provider, it) }
                )
            }

            item {
                SectionHeader(
                    title = "Paid AIs",
                    subtitle = "Need API credit from the provider's developer site. Chat subscriptions such as " +
                        "ChatGPT Plus or Claude Pro don't include API use."
                )
            }
            items(paid, key = { it.provider.name }) { state ->
                ProviderSetupCard(
                    state = state,
                    onDraftKeyChange = { viewModel.updateDraftKey(state.provider, it) },
                    onTestAndSave = { viewModel.testAndSave(state.provider) },
                    onSaveWithoutTesting = { viewModel.saveWithoutTesting(state.provider) },
                    onRemoveKey = { viewModel.removeKey(state.provider) },
                    onModelChange = { viewModel.updateModel(state.provider, it) }
                )
            }

            item { PrivacyNote() }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(top = 16.dp, bottom = 2.dp, start = 4.dp, end = 4.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PrivacyNote() {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.padding(top = 16.dp, start = 4.dp, end = 4.dp)
    ) {
        Icon(
            Icons.Filled.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Your keys are stored encrypted with the Android Keystore and are left out of backups. " +
                "Each key is only ever sent to the AI it belongs to, over a secure connection, along with " +
                "the questions you ask it. Screenshots are blocked on this screen.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreferencesCard(
    preferences: AppPreferences,
    onAnswerLanguageChange: (AnswerLanguage) -> Unit,
    onReadWithAiChange: (Boolean) -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Answer language", style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                AnswerLanguage.entries.forEachIndexed { index, language ->
                    SegmentedButton(
                        selected = preferences.answerLanguage == language,
                        onClick = { onAnswerLanguageChange(language) },
                        shape = SegmentedButtonDefaults.itemShape(index, AnswerLanguage.entries.size)
                    ) {
                        Text(language.label)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Read photos with AI", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Clear English text is read instantly on the device. Bangla, math, figures and " +
                            "hard-to-read photos go to your AI. Off: on-device only, English only.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = preferences.readWithAi, onCheckedChange = onReadWithAiChange)
            }
        }
    }
}
