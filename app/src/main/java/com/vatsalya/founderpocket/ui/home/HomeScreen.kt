package com.vatsalya.founderpocket.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vatsalya.founderpocket.ui.shared.CaptureCard
import com.vatsalya.founderpocket.ui.shared.greeting

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNewCapture: () -> Unit,
    onViewAll: () -> Unit,
    onFocusTimer: () -> Unit,
    onAssistant: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val todayFocus by viewModel.todayFocus.collectAsState(initial = emptyList())
    val recent by viewModel.recentCaptures.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(greeting()) },
                actions = {
                    IconButton(onClick = onAssistant) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Assistant")
                    }
                    IconButton(onClick = onFocusTimer) {
                        Icon(Icons.Default.Timer, contentDescription = "Focus Timer")
                    }
                }
            )
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
            if (todayFocus.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Today's Focus", style = MaterialTheme.typography.titleMedium)
                        Row {
                            TextButton(onClick = onAssistant) { Text("Ask AI") }
                            TextButton(onClick = onFocusTimer) { Text("Timer") }
                        }
                    }
                }
                items(todayFocus) { capture -> CaptureCard(capture) }
                item { Spacer(Modifier.height(12.dp)) }
            }

            item {
                Column {
                    Text("Recent", style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 4.dp))
                    if (recent.isEmpty()) {
                        Text(
                            "Nothing captured yet. Tap + to start.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(recent.take(5)) { capture -> CaptureCard(capture) }

            if (recent.size > 5) {
                item {
                    TextButton(onClick = onViewAll, modifier = Modifier.fillMaxWidth()) {
                        Text("View all ${recent.size} captures")
                    }
                }
            }
        }
    }
}
