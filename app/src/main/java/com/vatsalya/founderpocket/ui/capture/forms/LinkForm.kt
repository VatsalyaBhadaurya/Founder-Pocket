package com.vatsalya.founderpocket.ui.capture.forms

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.vatsalya.founderpocket.data.util.LinkCategorizer
import com.vatsalya.founderpocket.ui.capture.PayloadFormState

@Composable
fun LinkForm(state: PayloadFormState.Link, onUpdate: (PayloadFormState.Link) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = state.url,
            onValueChange = { url ->
                val cat = if (url.isBlank()) "web" else LinkCategorizer.categorize(url)
                onUpdate(state.copy(url = url, category = cat))
            },
            label = { Text("URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )
        if (state.category.isNotBlank()) {
            AssistChip(
                onClick = {},
                label = { Text(state.category) }
            )
        }
    }
}
