package com.vatsalya.founderpocket.ui.capture.forms

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.vatsalya.founderpocket.ui.capture.PayloadFormState

private val expenseCategories = listOf("food", "travel", "software", "marketing", "office", "other")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpenseForm(state: PayloadFormState.Expense, onUpdate: (PayloadFormState.Expense) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = state.amountStr,
            onValueChange = { onUpdate(state.copy(amountStr = it)) },
            label = { Text("Amount") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            prefix = { Text("$") }
        )
        Text("Category", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            expenseCategories.forEach { cat ->
                FilterChip(
                    selected = state.category == cat,
                    onClick = { onUpdate(state.copy(category = cat)) },
                    label = { Text(cat) }
                )
            }
        }
    }
}
