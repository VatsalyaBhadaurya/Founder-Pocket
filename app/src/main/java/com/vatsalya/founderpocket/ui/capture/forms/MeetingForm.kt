package com.vatsalya.founderpocket.ui.capture.forms

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
