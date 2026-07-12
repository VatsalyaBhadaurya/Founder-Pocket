package com.vatsalya.founderpocket.ui.capture.forms

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vatsalya.founderpocket.ui.capture.PayloadFormState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingForm(state: PayloadFormState.Meeting, onUpdate: (PayloadFormState.Meeting) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    var selectedLang by remember { mutableStateOf(SpeechLang.ENGLISH) }

    val sttLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val transcript = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull() ?: return@rememberLauncherForActivityResult
            // append transcript to key points with a newline separator
            val newKeyPoints = if (state.keyPoints.isBlank()) transcript
                               else "${state.keyPoints}\n$transcript"
            onUpdate(state.copy(keyPoints = newKeyPoints))
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = state.with,
            onValueChange = { onUpdate(state.copy(with = it)) },
            label = { Text("Meeting with") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = state.keyPoints,
            onValueChange = { onUpdate(state.copy(keyPoints = it)) },
            label = { Text("Key points") },
            modifier = Modifier.fillMaxWidth().height(100.dp)
        )

        // STT language selector + Transcribe button
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpeechLang.entries.forEach { lang ->
                FilterChip(
                    selected = selectedLang == lang,
                    onClick = { selectedLang = lang },
                    label = { Text(lang.label) }
                )
            }
            Spacer(Modifier.weight(1f))
            OutlinedButton(
                onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, selectedLang.bcp47)
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak key points…")
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    }
                    sttLauncher.launch(intent)
                }
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Transcribe")
            }
        }

        // Action items
        Text("Action items", style = MaterialTheme.typography.labelMedium)
        state.actionItems.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("• $item", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = { onUpdate(state.copy(actionItems = state.actionItems - item)) }) {
                    Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.actionItemInput,
                onValueChange = { onUpdate(state.copy(actionItemInput = it)) },
                label = { Text("Add action item") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Button(
                onClick = {
                    val item = state.actionItemInput.trim()
                    if (item.isNotBlank()) onUpdate(state.copy(actionItems = state.actionItems + item, actionItemInput = ""))
                },
                enabled = state.actionItemInput.isNotBlank()
            ) { Text("Add") }
        }

        // Deadline
        OutlinedTextField(
            value = state.deadline.ifBlank { "No deadline" },
            onValueChange = {},
            label = { Text("Deadline") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                TextButton(onClick = { showDatePicker = true }) { Text("Pick") }
            }
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onUpdate(state.copy(deadline = formatDateMillis(millis)))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }
}
