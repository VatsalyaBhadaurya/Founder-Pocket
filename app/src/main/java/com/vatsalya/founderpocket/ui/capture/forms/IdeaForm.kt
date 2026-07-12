package com.vatsalya.founderpocket.ui.capture.forms

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vatsalya.founderpocket.ui.capture.PayloadFormState

@Composable
fun IdeaForm(state: PayloadFormState.Idea, onUpdate: (PayloadFormState.Idea) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = state.problem,
            onValueChange = { onUpdate(state.copy(problem = it)) },
            label = { Text("Problem") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = state.whoHasIt,
            onValueChange = { onUpdate(state.copy(whoHasIt = it)) },
            label = { Text("Who has this problem?") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = state.solution,
            onValueChange = { onUpdate(state.copy(solution = it)) },
            label = { Text("Solution") },
            modifier = Modifier.fillMaxWidth().height(100.dp)
        )
    }
}
