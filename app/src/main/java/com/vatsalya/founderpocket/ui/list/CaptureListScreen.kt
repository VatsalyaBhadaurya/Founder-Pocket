package com.vatsalya.founderpocket.ui.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vatsalya.founderpocket.ui.shared.CaptureCard
import com.vatsalya.founderpocket.ui.shared.EmptyState

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
                    when {
                        uiState.mode == SearchMode.SEMANTIC && uiState.query.isNotBlank() ->
                            EmptyState(
                                icon = Icons.Default.Search,
                                title = "No semantic matches",
                                subtitle = "Try different wording, or check that the ONNX model is in assets."
                            )
                        uiState.query.isNotBlank() ->
                            EmptyState(
                                icon = Icons.Default.Search,
                                title = "No results",
                                subtitle = "Nothing matched \"${uiState.query}\""
                            )
                        else ->
                            EmptyState(
                                icon = Icons.Default.Search,
                                title = "No captures yet",
                                subtitle = "Tap + to capture your first note, meeting, or idea."
                            )
                    }
                }
            }

            items(displayList, key = { it.id }) { capture ->
                CaptureCard(capture)
            }
        }
    }
}
