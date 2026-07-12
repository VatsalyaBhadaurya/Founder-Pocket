package com.vatsalya.founderpocket.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
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
    val query by viewModel.query.collectAsState()
    val captures by viewModel.captures.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("All captures") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewCapture) {
                Icon(Icons.Default.Add, contentDescription = "New capture")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search captures…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )
            }

            if (captures.isEmpty()) {
                item {
                    Text(
                        text = if (query.isBlank()) "No captures yet." else "No results for \"$query\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }
            } else {
                items(captures, key = { it.id }) { capture ->
                    CaptureCard(capture)
                }
            }
        }
    }
}
