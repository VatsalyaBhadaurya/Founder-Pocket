package com.vatsalya.founderpocket.ui.capture.forms

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vatsalya.founderpocket.ui.capture.PayloadFormState

private val timePresets = listOf(
    "Morning (9 AM)"   to 9,
    "Afternoon (2 PM)" to 14,
    "Evening (6 PM)"   to 18
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowupForm(state: PayloadFormState.Followup, onUpdate: (PayloadFormState.Followup) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = state.subject,
            onValueChange = { onUpdate(state.copy(subject = it)) },
            label = { Text("Subject") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Date picker
        OutlinedTextField(
            value = if (state.remindDate.isBlank()) "Pick reminder date" else displayDate(state.remindDate),
            onValueChange = {},
            label = { Text("Remind on") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = { TextButton(onClick = { showDatePicker = true }) { Text("Pick") } }
        )

        // Time preset chips
        if (state.remindDate.isNotBlank()) {
            Text("Time", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                timePresets.forEach { (label, hour) ->
                    FilterChip(
                        selected = state.remindHour == hour,
                        onClick = {
                            val millis = dateAtHourMillis(state.remindDate, hour)
                            onUpdate(state.copy(remindHour = hour, remindAt = millis))
                        },
                        label = { Text(label) }
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val isoDate = formatDateMillis(millis)
                        val remindMillis = dateAtHourMillis(isoDate, state.remindHour)
                        onUpdate(state.copy(remindDate = isoDate, remindAt = remindMillis))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }
}
