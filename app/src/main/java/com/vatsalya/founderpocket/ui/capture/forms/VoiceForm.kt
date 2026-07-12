package com.vatsalya.founderpocket.ui.capture.forms

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun VoiceForm(onTranscript: (String) -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val transcript = matches?.firstOrNull() ?: return@rememberLauncherForActivityResult
            onTranscript(transcript)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Tap the mic to record your voice note.", style = MaterialTheme.typography.bodySmall)
        FilledTonalButton(
            onClick = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your note…")
                }
                launcher.launch(intent)
            },
            modifier = Modifier.size(64.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(Icons.Default.Mic, contentDescription = "Record voice", modifier = Modifier.size(32.dp))
        }
    }
}
