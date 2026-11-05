package com.example.aistuddybuddy.ui.screens.questions

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

/** A scan in progress, or one that failed with the choices the student has. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PendingScanCard(
    scan: PendingScan,
    onRetry: () -> Unit,
    onUseOcr: () -> Unit,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val thumbnail = remember(scan.image) { scan.image.asImageBitmap() }
                Image(
                    bitmap = thumbnail,
                    contentDescription = "Selected part of the page",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.small)
                )
                Spacer(Modifier.width(12.dp))
                when (val status = scan.status) {
                    is PendingStatus.Reading -> {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(status.message, style = MaterialTheme.typography.bodyLarge)
                            // Only AI reading is slow; the on-device step finishes before this would matter.
                            if (status.message != PendingStatus.Reading.INITIAL) {
                                Text(
                                    text = AI_WAIT_NOTICE,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                    }
                    is PendingStatus.Failed -> {
                        Text(
                            text = status.message.lineSequence().first(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Dismiss")
                }
            }

            val failed = scan.status as? PendingStatus.Failed ?: return@Column
            // Per-provider reasons, e.g. "Gemini: Network error…", in smaller text.
            val details = failed.message.lines().drop(1).filter { it.isNotBlank() }
            if (details.isNotEmpty()) {
                Text(
                    text = details.joinToString("\n"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, end = 8.dp)
                )
            }
            FlowRow(modifier = Modifier.padding(top = 8.dp, end = 8.dp)) {
                if (failed.needsApiKey) {
                    FilledTonalButton(onClick = onOpenSettings, modifier = Modifier.padding(end = 8.dp)) {
                        Text("Open Settings")
                    }
                } else {
                    FilledTonalButton(onClick = onRetry, modifier = Modifier.padding(end = 8.dp)) {
                        Text("Try again")
                    }
                }
                if (failed.ocrQuestions.isNotEmpty()) {
                    OutlinedButton(onClick = onUseOcr) {
                        Text("Use OCR anyway")
                    }
                }
            }
        }
    }
}
