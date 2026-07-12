package com.vatsalya.founderpocket.ui.today

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vatsalya.founderpocket.data.model.AssistantSuggestion
import com.vatsalya.founderpocket.data.model.Capture
import com.vatsalya.founderpocket.data.model.SuggestionType
import com.vatsalya.founderpocket.data.model.payload.TaskPayload
import kotlinx.serialization.json.Json

@Composable
fun TodayScreen(
    onNavigateToDetail: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val focusItems by viewModel.focusItems.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val shippedText by viewModel.shippedText.collectAsState()
    val json = remember { Json { ignoreUnknownKeys = true } }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Text("Today's focus", style = MaterialTheme.typography.titleLarge)
        }

        if (focusItems.isEmpty()) {
            item {
                Text(
                    "No open tasks or follow-ups.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(focusItems, key = { it.id }) { capture ->
            val isDone = runCatching {
                json.decodeFromString<TaskPayload>(capture.payload).done
            }.getOrDefault(false)

            TaskRow(
                capture = capture,
                isDone = isDone,
                onToggle = { viewModel.toggleTaskDone(capture) },
                onClick = { onNavigateToDetail(capture.id) }
            )
        }

        // Top suggestion card
        suggestions.firstOrNull()?.let { suggestion ->
            item {
                SuggestionCard(suggestion = suggestion, onClick = { onNavigateToDetail(suggestion.captureId) })
            }
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }

        // Today I shipped
        item {
            Text("Today I shipped…", style = MaterialTheme.typography.titleMedium)
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = shippedText,
                    onValueChange = viewModel::onShippedChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("One sentence") },
                    singleLine = true
                )
                Button(
                    onClick = viewModel::saveShipped,
                    enabled = shippedText.isNotBlank()
                ) { Text("Log") }
            }
        }
    }
}

@Composable
private fun TaskRow(
    capture: Capture,
    isDone: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (isDone) "Mark open" else "Mark done",
                tint = if (isDone) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(
            onClick = onClick,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = capture.body,
                style = MaterialTheme.typography.bodyMedium.copy(
                    textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SuggestionCard(suggestion: AssistantSuggestion, onClick: () -> Unit) {
    val containerColor = when (suggestion.type) {
        SuggestionType.OVERDUE_TASK     -> MaterialTheme.colorScheme.errorContainer
        SuggestionType.DUE_SOON         -> MaterialTheme.colorScheme.tertiaryContainer
        SuggestionType.FOLLOWUP_PENDING -> MaterialTheme.colorScheme.secondaryContainer
        SuggestionType.RECENT_WIN       -> MaterialTheme.colorScheme.primaryContainer
        SuggestionType.NO_DUE_TASK      -> MaterialTheme.colorScheme.surfaceVariant
    }
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(suggestion.title, style = MaterialTheme.typography.bodyMedium)
                Text(suggestion.subtitle, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
