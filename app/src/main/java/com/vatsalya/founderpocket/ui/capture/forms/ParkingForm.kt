package com.vatsalya.founderpocket.ui.capture.forms

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vatsalya.founderpocket.ui.capture.PayloadFormState

@Composable
fun ParkingForm(state: PayloadFormState.Parking, onUpdate: (PayloadFormState.Parking) -> Unit) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (state.isLoading) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text("Fetching location…", style = MaterialTheme.typography.bodyMedium)
            }
        } else if (state.lat != null && state.lng != null) {
            Text(
                text = state.label ?: "%.5f, %.5f".format(state.lat, state.lng),
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedButton(
                onClick = {
                    val uri = Uri.parse("geo:${state.lat},${state.lng}?q=${state.lat},${state.lng}(Parked here)")
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Navigate back in Maps") }
        } else {
            Text("Location will be captured automatically on save.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
