package com.vatsalya.founderpocket.ui.recall

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
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
fun RecallScreen(
    onNavigateToDetail: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecallViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val results by viewModel.results.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search your captures…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true
        )

        // Filter row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            item {
                FilterChip(
                    selected = state.nearOnly,
                    onClick = viewModel::onNearToggle,
                    label = { Text("Near") },
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null,
                            modifier = Modifier.size(16.dp))
                    }
                )
            }
            items(TimeFilter.entries) { filter ->
                FilterChip(
                    selected = state.timeFilter == filter,
                    onClick = { viewModel.onTimeFilter(filter) },
                    label = {
                        Text(when (filter) {
                            TimeFilter.ALL   -> "Any time"
                            TimeFilter.TODAY -> "Today"
                            TimeFilter.WEEK  -> "This week"
                        })
                    }
                )
            }
            item {
                FilterChip(
                    selected = state.useSemanticSearch,
                    onClick = viewModel::onSemanticToggle,
                    label = { Text("Semantic") }
                )
            }
        }

        if (state.isSearching) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (results.isEmpty() && !state.isSearching) {
                item {
                    val msg = when {
                        state.query.isNotBlank() -> "No results for \"${state.query}\""
                        state.nearOnly           -> "No captures with location attached."
                        else                     -> "No captures yet."
                    }
                    EmptyState(
                        icon = Icons.Default.Search,
                        title = if (state.query.isNotBlank()) "No results" else "Nothing here yet",
                        subtitle = msg
                    )
                }
            }
            items(results, key = { it.id }) { capture ->
                CaptureCard(capture = capture, onClick = { onNavigateToDetail(capture.id) })
            }
        }
    }
}
