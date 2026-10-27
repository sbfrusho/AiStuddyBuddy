package com.example.aistuddybuddy.ui.screens.questions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.aistuddybuddy.domain.model.AiProvider
import com.example.aistuddybuddy.domain.model.QuestionType
import com.example.aistuddybuddy.domain.model.optionLabel
import com.example.aistuddybuddy.ui.components.ProviderBadge
import com.example.aistuddybuddy.ui.components.brandColor

@Composable
fun QuestionCard(
    item: QuestionItem,
    onAsk: (AiProvider, Boolean) -> Unit,
    onToggleEditing: () -> Unit,
    onDelete: () -> Unit,
    onQuestionTextChange: (String) -> Unit,
    onOptionChange: (Int, String) -> Unit,
    onAddOption: () -> Unit,
    onRemoveOption: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = {},
                    label = { Text(if (item.question.type == QuestionType.MCQ) "MCQ" else "Descriptive") }
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onToggleEditing) {
                    Icon(
                        imageVector = if (item.isEditing) Icons.Filled.Done else Icons.Filled.Edit,
                        contentDescription = if (item.isEditing) "Done editing" else "Edit question"
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete question")
                }
            }

            Column(modifier = Modifier.padding(end = 8.dp)) {
                if (item.isEditing) {
                    QuestionEditor(item, onQuestionTextChange, onOptionChange, onAddOption, onRemoveOption)
                } else {
                    QuestionText(item)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                ProviderSelector(
                    selected = item.selectedProvider,
                    onSelect = { onAsk(it, false) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                AnswerSection(
                    item = item,
                    onRetry = { provider -> onAsk(provider, true) },
                    onOpenSettings = onOpenSettings
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuestionText(item: QuestionItem) {
    val question = item.question
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Q. ") }
            append(question.text.ifBlank { "(empty — tap edit to type the question)" })
        },
        style = MaterialTheme.typography.bodyLarge
    )
    val options = question.options.filter { it.isNotBlank() }
    if (options.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEachIndexed { index, option ->
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append("${optionLabel(index)}. ") }
                        append(option)
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun QuestionEditor(
    item: QuestionItem,
    onQuestionTextChange: (String) -> Unit,
    onOptionChange: (Int, String) -> Unit,
    onAddOption: () -> Unit,
    onRemoveOption: (Int) -> Unit
) {
    OutlinedTextField(
        value = item.question.text,
        onValueChange = onQuestionTextChange,
        label = { Text("Question") },
        modifier = Modifier.fillMaxWidth()
    )
    item.question.options.forEachIndexed { index, option ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = option,
                onValueChange = { onOptionChange(index, it) },
                label = { Text("Option ${optionLabel(index)}") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 4.dp)
            )
            IconButton(onClick = { onRemoveOption(index) }) {
                Icon(Icons.Filled.Close, contentDescription = "Remove option ${optionLabel(index)}")
            }
        }
    }
    TextButton(onClick = onAddOption) {
        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(if (item.question.options.isEmpty()) "Add options (make it an MCQ)" else "Add option")
    }
}

@Composable
private fun ProviderSelector(selected: AiProvider?, onSelect: (AiProvider) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        AiProvider.entries.forEach { provider ->
            val isSelected = provider == selected
            OutlinedButton(
                onClick = { onSelect(provider) },
                modifier = Modifier.weight(1f),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) provider.brandColor else MaterialTheme.colorScheme.outlineVariant
                ),
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ProviderBadge(provider, size = 22.dp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = provider.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun AnswerSection(
    item: QuestionItem,
    onRetry: (AiProvider) -> Unit,
    onOpenSettings: () -> Unit
) {
    val provider = item.selectedProvider
    if (provider == null) {
        Text(
            text = "Choose an AI above to get the answer.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    when (val state = item.answers[provider]) {
        null, AnswerState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(12.dp))
            Text("Asking ${provider.displayName}…", style = MaterialTheme.typography.bodyMedium)
        }

        is AnswerState.Error -> Column {
            Text(
                text = state.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            Row {
                if (state.needsApiKey) {
                    TextButton(onClick = onOpenSettings) { Text("Open Settings") }
                }
                TextButton(onClick = { onRetry(provider) }) { Text("Try again") }
            }
        }

        is AnswerState.Success -> {
            val clipboard = LocalClipboardManager.current
            Column {
                SelectionContainer {
                    Text(text = formatAnswer(state.text), style = MaterialTheme.typography.bodyLarge)
                }
                if (state.question != item.question) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "You edited the question. Tap refresh for an updated answer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProviderBadge(provider, size = 14.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Answered by ${provider.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { clipboard.setText(AnnotatedString(state.text)) }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy answer", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { onRetry(provider) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Ask again", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

/** Bolds the "Ans:" and "Why:" labels the prompt asks the AI to use. */
private fun formatAnswer(text: String): AnnotatedString = buildAnnotatedString {
    val labels = listOf("Ans:", "Answer:", "Why:")
    text.lines().forEachIndexed { index, line ->
        if (index > 0) append("\n")
        val label = labels.firstOrNull { line.trimStart().startsWith(it, ignoreCase = true) }
        if (label != null) {
            val start = line.indexOf(line.trimStart()) + label.length
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(line.substring(0, start)) }
            append(line.substring(start))
        } else {
            append(line)
        }
    }
}
