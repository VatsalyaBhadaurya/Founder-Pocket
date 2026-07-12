package com.vatsalya.founderpocket.ui.capture.forms

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vatsalya.founderpocket.ui.capture.PayloadFormState

@Composable
fun ContactForm(state: PayloadFormState.Contact, onUpdate: (PayloadFormState.Contact) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = state.name,
            onValueChange = { onUpdate(state.copy(name = it)) },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = state.metAt,
            onValueChange = { onUpdate(state.copy(metAt = it)) },
            label = { Text("Met at") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = state.org,
            onValueChange = { onUpdate(state.copy(org = it)) },
            label = { Text("Company / Org") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = state.note,
            onValueChange = { onUpdate(state.copy(note = it)) },
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth().height(80.dp)
        )
    }
}
