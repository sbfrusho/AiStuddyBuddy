package com.example.aistuddybuddy.ui.screens.questions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionsScreen(
    viewModel: ScanViewModel,
    onScanMore: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenBookmarks: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val providers by viewModel.providers.collectAsStateWithLifecycle()
    val hasAnyKey by viewModel.hasAnyKey.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var confirmClear by remember { mutableStateOf(false) }

    // New scans and questions are added to the top; bring them into view.
    val firstId = uiState.pendingScans.firstOrNull()?.id ?: uiState.questions.firstOrNull()?.id
    LaunchedEffect(firstId) {
        if (firstId != null) listState.animateScrollToItem(0)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Questions") },
                actions = {
                    IconButton(onClick = viewModel::addBlankQuestion) {
                        Icon(Icons.Filled.Add, contentDescription = "Type a question")
                    }
                    if (uiState.questions.isNotEmpty()) {
                        IconButton(onClick = { confirmClear = true }) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear all")
                        }
                    }
                    IconButton(onClick = onOpenBookmarks) {
                        Icon(Icons.Filled.Bookmarks, contentDescription = "Saved answers")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },

        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onScanMore,
                icon = { Icon(Icons.Filled.DocumentScanner, contentDescription = null) },
                text = { Text("Scan more") }
            )
        }
    ) { padding ->
        if (uiState.questions.isEmpty() && uiState.pendingScans.isEmpty()) {
            EmptyState(
                hasAnyKey = hasAnyKey,
                onScan = onScanMore,
                onOpenSettings = onOpenSettings,
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!hasAnyKey) {
                    item(key = "connect-banner") { ConnectAiBanner(onOpenSettings) }
                }
                items(uiState.pendingScans, key = { "scan-${it.id}" }) { scan ->
                    PendingScanCard(
                        scan = scan,
                        onRetry = { viewModel.retryScan(scan.id) },
                        onUseOcr = { viewModel.useOcrResult(scan.id) },
                        onDismiss = { viewModel.dismissScan(scan.id) },
                        onOpenSettings = onOpenSettings,
                        modifier = Modifier.animateItem()
                    )
                }
                items(uiState.questions, key = { it.id }) { item ->
                    QuestionCard(
                        item = item,
                        providers = providers,
                        onAsk = { provider, refresh -> viewModel.ask(item.id, provider, refresh) },
                        onSelectStyle = { viewModel.selectStyle(item.id, it) },
                        onToggleBookmark = { viewModel.toggleBookmark(item.id) },
                        onRemoveImage = { viewModel.removeImage(item.id) },
                        onToggleEditing = { viewModel.toggleEditing(item.id) },
                        onDelete = { viewModel.deleteQuestion(item.id) },
                        onQuestionTextChange = { viewModel.updateQuestionText(item.id, it) },
                        onOptionChange = { index, text -> viewModel.updateOption(item.id, index, text) },
                        onAddOption = { viewModel.addOption(item.id) },
                        onRemoveOption = { viewModel.removeOption(item.id, it) },
                        onOpenSettings = onOpenSettings,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear all questions?") },
            text = { Text("This removes every question and answer from the list.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAll()
                    confirmClear = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun EmptyState(
    hasAnyKey: Boolean,
    onScan: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        if (!hasAnyKey) {
            ConnectAiBanner(onOpenSettings)
            Spacer(Modifier.height(24.dp))
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .size(72.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.DocumentScanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No questions yet",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Scan a page from your book, or tap + to type a question yourself.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onScan, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Icon(Icons.Filled.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Scan a page")
        }
    }
}

/** Shown until at least one AI key is saved, since nothing can be answered without one. */
@Composable
private fun ConnectAiBanner(onOpenSettings: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Filled.Key, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Connect a free AI",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    "Takes about a minute, no card needed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            FilledTonalButton(onClick = onOpenSettings) { Text("Connect") }
        }
    }
}
