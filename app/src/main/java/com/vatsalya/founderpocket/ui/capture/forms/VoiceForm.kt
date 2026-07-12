package com.vatsalya.founderpocket.ui.capture.forms

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun VoiceForm(onTranscript: (String) -> Unit) {
    val context = LocalContext.current
    val isAvailable = remember { SpeechRecognizer.isRecognitionAvailable(context) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!isAvailable) {
            Text(
                "Speech recognition is not available on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
            return@Column
        }

        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val transcript = result.data
                    ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    ?.firstOrNull() ?: return@rememberLauncherForActivityResult
                onTranscript(transcript)
            }
        }

        Text("Tap the mic to record your voice note.", style = MaterialTheme.typography.bodySmall)
        FilledTonalButton(
            onClick = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your note…")
                    // Spike B: request offline recognition when available
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
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
