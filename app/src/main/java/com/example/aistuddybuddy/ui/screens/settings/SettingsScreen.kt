package com.example.aistuddybuddy.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.aistuddybuddy.domain.model.AiProvider
import com.example.aistuddybuddy.domain.model.AnswerLanguage
import com.example.aistuddybuddy.domain.model.AppPreferences
import com.example.aistuddybuddy.domain.model.ProviderConfig
import com.example.aistuddybuddy.ui.components.ProviderBadge
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private val AiProvider.apiKeyUrl: String
    get() = when (this) {
        AiProvider.CHATGPT -> "https://platform.openai.com/api-keys"
        AiProvider.GEMINI -> "https://aistudio.google.com/apikey"
        AiProvider.DEEPSEEK -> "https://platform.deepseek.com/api_keys"
        AiProvider.OPENROUTER -> "https://openrouter.ai/settings/keys"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val drafts by viewModel.drafts.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PreferencesCard(
                preferences = preferences,
                onAnswerLanguageChange = viewModel::updateAnswerLanguage,
                onReadWithAiChange = viewModel::updateReadWithAi
            )

            Text(
                text = "Add an API key for each AI you want to use. Both have free keys. " +
                    "Keys are stored encrypted, only on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AiProvider.enabled.forEach { provider ->
                ProviderSettingsCard(
                    provider = provider,
                    config = drafts[provider] ?: ProviderConfig(),
                    onApiKeyChange = { viewModel.updateApiKey(provider, it) },
                    onModelChange = { viewModel.updateModel(provider, it) }
                )
            }

            Button(
                onClick = {
                    viewModel.save()
                    scope.launch { snackbarHostState.showSnackbar("Settings saved") }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Text("Save")
            }
        }
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
                        text = "Gemini (or a free OpenRouter vision model) reads the photo, including Bangla, " +
                            "math and figures. Off: on-device OCR, offline but English only.",
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

@Composable
private fun ProviderSettingsCard(
    provider: AiProvider,
    config: ProviderConfig,
    onApiKeyChange: (String) -> Unit,
    onModelChange: (String) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var showKey by remember { mutableStateOf(false) }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProviderBadge(provider, size = 28.dp)
                Spacer(Modifier.width(12.dp))
                Text(provider.displayName, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { uriHandler.openUri(provider.apiKeyUrl) }) {
                    Text("Get a key")
                }
            }
            OutlinedTextField(
                value = config.apiKey,
                onValueChange = onApiKeyChange,
                label = { Text("API key") },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(
                            imageVector = if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (showKey) "Hide key" else "Show key"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            ModelDropdown(
                provider = provider,
                selectedModel = config.model,
                onModelChange = onModelChange,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelDropdown(
    provider: AiProvider,
    selectedModel: String,
    onModelChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val knownModel = provider.models.firstOrNull { it.isAvailable && it.id == selectedModel }
    val defaultModel = provider.models.first { it.isAvailable }
    // Models not in the list (e.g. saved before the list changed) open in custom mode.
    var isCustom by remember { mutableStateOf(selectedModel.isNotBlank() && knownModel == null) }

    Column(modifier = modifier) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = when {
                    isCustom -> "Custom model ID"
                    knownModel != null -> knownModel.label
                    else -> defaultModel.label
                },
                onValueChange = {},
                readOnly = true,
                label = { Text("Model") },
                supportingText = if (!isCustom) {
                    { Text((knownModel ?: defaultModel).description) }
                } else {
                    null
                },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                provider.models.forEach { model ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(model.label, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = model.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        enabled = model.isAvailable,
                        trailingIcon = if (model.isAvailable && model.id == selectedModel && !isCustom) {
                            { Icon(Icons.Filled.Check, contentDescription = "Selected") }
                        } else {
                            null
                        },
                        onClick = {
                            isCustom = false
                            onModelChange(model.id)
                            expanded = false
                        }
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Custom model ID…") },
                    onClick = {
                        isCustom = true
                        expanded = false
                    }
                )
            }
        }

        if (isCustom) {
            OutlinedTextField(
                value = selectedModel,
                onValueChange = onModelChange,
                label = { Text("Model ID") },
                placeholder = { Text(provider.defaultModel) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}
