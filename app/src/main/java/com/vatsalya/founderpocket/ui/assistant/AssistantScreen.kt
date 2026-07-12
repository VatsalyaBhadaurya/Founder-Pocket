package com.vatsalya.founderpocket.ui.assistant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vatsalya.founderpocket.data.model.AssistantSuggestion
import com.vatsalya.founderpocket.data.model.SuggestionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(onBack: () -> Unit, viewModel: AssistantViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assistant") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Rules-based Suggested Focus ──────────────────────────────────
            Text("Suggested Focus", style = MaterialTheme.typography.titleMedium)

            if (state.isLoading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.suggestions.isEmpty()) {
                Text(
                    "No open tasks or follow-ups. Capture something first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                state.suggestions.forEach { suggestion ->
                    SuggestionCard(suggestion)
                }
            }

            HorizontalDivider()

            // ── AI Query section ─────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null,
                    tint = if (state.llmAvailable) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "Ask AI",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (state.llmAvailable) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!state.llmAvailable) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Not set up", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            if (!state.llmAvailable) {
                // Setup banner
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("To enable AI suggestions:", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "1. Run Spike C to benchmark Gemma 3 1B on your device\n" +
                            "2. Download the model from Kaggle\n" +
                            "3. Push to device:\n" +
                            "   adb push model.task\n   ${state.modelPath}\n" +
                            "4. Wire LiteRT-LM in LlmManager.kt",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Query input (always visible — works as a preview even when model absent)
            OutlinedTextField(
                value = state.aiQuery,
                onValueChange = viewModel::onQueryChange,
                label = { Text("Ask") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                enabled = !state.isGenerating
            )

            if (state.isCopyingModel) {
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Copying model from assets (first run, ~30 s)…",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Button(
                onClick = viewModel::generate,
                enabled = state.aiQuery.isNotBlank() && !state.isGenerating,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.isCopyingModel) "Copying model…" else "Generating…")
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.llmAvailable) "Generate" else "Preview prompt")
                }
            }

            // AI response
            if (state.aiResponse.isNotBlank()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = state.aiResponse,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionCard(suggestion: AssistantSuggestion) {
    val containerColor = when (suggestion.type) {
        SuggestionType.OVERDUE_TASK     -> MaterialTheme.colorScheme.errorContainer
        SuggestionType.DUE_SOON         -> MaterialTheme.colorScheme.tertiaryContainer
        SuggestionType.FOLLOWUP_PENDING -> MaterialTheme.colorScheme.secondaryContainer
        SuggestionType.RECENT_WIN       -> MaterialTheme.colorScheme.primaryContainer
        SuggestionType.NO_DUE_TASK      -> MaterialTheme.colorScheme.surfaceVariant
    }
    val badge = when (suggestion.type) {
        SuggestionType.OVERDUE_TASK     -> "⚠ Overdue"
        SuggestionType.DUE_SOON         -> "Due soon"
        SuggestionType.FOLLOWUP_PENDING -> "Follow-up"
        SuggestionType.RECENT_WIN       -> "Win"
        SuggestionType.NO_DUE_TASK      -> "Task"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = suggestion.title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            SuggestionChip(onClick = {}, label = { Text(badge, style = MaterialTheme.typography.labelSmall) })
        }
    }
}
