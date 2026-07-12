package com.vatsalya.founderpocket.ui.focus

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

private val durations = listOf(25, 45, 60)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusTimerScreen(onBack: () -> Unit, viewModel: FocusTimerViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Focus Timer") },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
        ) {
            // Duration selector
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                durations.forEach { min ->
                    FilterChip(
                        selected = state.durationMinutes == min,
                        onClick = { viewModel.setDuration(min) },
                        enabled = state.timerState != TimerState.RUNNING,
                        label = { Text("${min}m") }
                    )
                }
            }

            // Timer display
            val mins = state.remainingSeconds / 60
            val secs = state.remainingSeconds % 60
            Text(
                text = "%02d:%02d".format(mins, secs),
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = if (state.timerState == TimerState.DONE)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface
            )

            if (state.timerState == TimerState.DONE) {
                Text("Session complete!", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary)
            }

            // Controls
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                when (state.timerState) {
                    TimerState.IDLE, TimerState.PAUSED -> {
                        Button(onClick = { viewModel.start() }) { Text("Start") }
                    }
                    TimerState.RUNNING -> {
                        OutlinedButton(onClick = { viewModel.pause() }) { Text("Pause") }
                    }
                    TimerState.DONE -> {}
                }
                if (state.timerState != TimerState.IDLE) {
                    OutlinedButton(onClick = { viewModel.reset() }) { Text("Reset") }
                }
            }
        }
    }
}
