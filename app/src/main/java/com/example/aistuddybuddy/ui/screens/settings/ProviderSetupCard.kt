package com.example.aistuddybuddy.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.aistuddybuddy.domain.model.AiProvider
import com.example.aistuddybuddy.ui.components.ProviderBadge
import com.example.aistuddybuddy.ui.theme.successColor

/**
 * One AI's setup card: status at a glance, and when expanded, the steps to get a key, a paste
 * field, and Test & save. [showModel] hides the model picker on the welcome screen.
 */
@Composable
fun ProviderSetupCard(
    state: ProviderUiState,
    onDraftKeyChange: (String) -> Unit,
    onTestAndSave: () -> Unit,
    onSaveWithoutTesting: () -> Unit,
    onRemoveKey: () -> Unit,
    onModelChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    showModel: Boolean = true
) {
    val provider = state.provider
    var expanded by rememberSaveable(provider) { mutableStateOf(initiallyExpanded) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp)
        ) {
            ProviderBadge(provider, size = 36.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(provider.displayName, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    StatusPill(state)
                }
                Text(
                    text = if (state.isConnected && !expanded) "Key ${state.maskedKey}" else provider.tagline,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse ${provider.displayName}" else "Set up ${provider.displayName}",
                modifier = Modifier.padding(8.dp)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                SetupSteps(provider)
                GetKeyButton(provider)
                KeyField(state, onDraftKeyChange, onTestAndSave)
                StatusMessage(state.status, onSaveWithoutTesting)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Button(
                        onClick = onTestAndSave,
                        enabled = state.status != KeyStatus.Testing &&
                            (state.draftKey.isNotBlank() || state.isConnected)
                    ) {
                        Text(if (state.draftKey.isBlank() && state.isConnected) "Test key" else "Test & save")
                    }
                    Spacer(Modifier.weight(1f))
                    if (state.isConnected) {
                        TextButton(onClick = onRemoveKey) {
                            Text("Remove key", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                if (showModel) {
                    ModelDropdown(
                        provider = provider,
                        selectedModel = state.saved.model,
                        onModelChange = onModelChange,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusPill(state: ProviderUiState) {
    val (text, color) = when {
        state.isConnected -> "Connected" to successColor
        state.provider.isPaidOnly -> "Paid API" to MaterialTheme.colorScheme.tertiary
        else -> "Not set" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        if (state.isConnected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(3.dp))
        }
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun SetupSteps(provider: AiProvider) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        provider.setupSteps.forEachIndexed { index, step ->
            Row {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(20.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(step, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun GetKeyButton(provider: AiProvider) {
    val uriHandler = LocalUriHandler.current
    OutlinedButton(
        onClick = { uriHandler.openUri(provider.apiKeyUrl) },
        modifier = Modifier.padding(top = 12.dp)
    ) {
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(if (provider.isPaidOnly) "Get a key" else "Get a free key")
    }
}

@Composable
private fun KeyField(state: ProviderUiState, onDraftKeyChange: (String) -> Unit, onSubmit: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var showKey by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = state.draftKey,
        onValueChange = onDraftKeyChange,
        label = { Text(if (state.isConnected) "Replace key" else "API key") },
        placeholder = { Text(if (state.isConnected) state.maskedKey else "Paste your key here") },
        singleLine = true,
        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        trailingIcon = {
            Row {
                if (state.draftKey.isNotEmpty()) {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(
                            imageVector = if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (showKey) "Hide key" else "Show key"
                        )
                    }
                }
                IconButton(onClick = { clipboard.getText()?.text?.let(onDraftKeyChange) }) {
                    Icon(Icons.Filled.ContentPaste, contentDescription = "Paste key")
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    )
}

@Composable
private fun StatusMessage(status: KeyStatus, onSaveWithoutTesting: () -> Unit) {
    when (status) {
        KeyStatus.Idle -> Unit
        KeyStatus.Testing -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("Checking the key…", style = MaterialTheme.typography.bodySmall)
        }
        KeyStatus.Works -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = successColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Key works and is saved.", style = MaterialTheme.typography.bodySmall, color = successColor)
        }
        is KeyStatus.Failed -> Column(modifier = Modifier.padding(top = 8.dp)) {
            Row {
                Icon(
                    Icons.Filled.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(status.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            TextButton(onClick = onSaveWithoutTesting) { Text("Save anyway") }
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
