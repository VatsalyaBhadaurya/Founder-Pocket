package com.vatsalya.founderpocket.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vatsalya.founderpocket.ui.shared.CaptureCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureListScreen(
    onNewCapture: () -> Unit,
    viewModel: CaptureListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val keywordResults by viewModel.keywordResults.collectAsState()

    val displayList = when (uiState.mode) {
        SearchMode.KEYWORD -> keywordResults
        SearchMode.SEMANTIC -> uiState.semanticResults
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("All captures") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewCapture) {
                Icon(Icons.Default.Add, contentDescription = "New capture")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search bar
            item {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search captures…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )
            }

            // Mode toggle
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = uiState.mode == SearchMode.KEYWORD,
                        onClick = { viewModel.onModeChange(SearchMode.KEYWORD) },
                        label = { Text("Keyword") }
                    )
                    FilterChip(
                        selected = uiState.mode == SearchMode.SEMANTIC,
                        onClick = { viewModel.onModeChange(SearchMode.SEMANTIC) },
                        label = { Text("Semantic") }
                    )
                }
            }

            // Semantic loading indicator
            if (uiState.mode == SearchMode.SEMANTIC && uiState.isSearching) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            // Empty state
            if (displayList.isEmpty() && !uiState.isSearching) {
                item {
                    val msg = when {
                        uiState.mode == SearchMode.SEMANTIC && uiState.query.isNotBlank() ->
                            "No semantic matches — model may not be loaded yet (complete Spike A)."
                        uiState.query.isBlank() -> "No captures yet. Tap + to start."
                        else -> "No results for \"${uiState.query}\""
                    }
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }
            }

            items(displayList, key = { it.id }) { capture ->
                CaptureCard(capture)
            }
        }
    }
}
