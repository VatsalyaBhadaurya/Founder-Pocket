package com.vatsalya.founderpocket.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vatsalya.founderpocket.ui.shared.CaptureCard
import com.vatsalya.founderpocket.ui.shared.greeting
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNewCapture: () -> Unit,
    onViewAll: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val todayFocus by viewModel.todayFocus.collectAsState(initial = emptyList())
    val recent by viewModel.recentCaptures.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(greeting()) })
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
                    Text(
                        "Today's Focus",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(todayFocus) { capture ->
                    CaptureCard(capture)
                }
                item { Spacer(Modifier.height(12.dp)) }
            }

            item {
                Column {
                    Text(
                        "Recent",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    if (recent.isEmpty()) {
                        Text(
                            "Nothing captured yet. Tap + to start.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(recent.take(5)) { capture ->
                CaptureCard(capture)
            }

            if (recent.size > 5) {
                item {
                    TextButton(
                        onClick = onViewAll,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("View all ${recent.size} captures")
                    }
                }
            }
        }
    }
}
