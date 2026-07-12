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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

enum class SpeechLang(val bcp47: String, val label: String) {
    ENGLISH("en-IN", "English"),
    HINDI("hi-IN", "हिंदी"),
    HINGLISH("hi-IN", "Hinglish")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceForm(onTranscript: (String) -> Unit) {
    val context = LocalContext.current
    val isAvailable = remember { SpeechRecognizer.isRecognitionAvailable(context) }
    var selectedLang by remember { mutableStateOf(SpeechLang.ENGLISH) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
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

        // Language selector
        Text("Language", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SpeechLang.entries.forEach { lang ->
                FilterChip(
                    selected = selectedLang == lang,
                    onClick = { selectedLang = lang },
                    label = { Text(lang.label) }
                )
            }
        }

        FilledTonalButton(
            onClick = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, selectedLang.bcp47)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, selectedLang.bcp47)

                    putExtra(RecognizerIntent.EXTRA_PROMPT,
                        when (selectedLang) {
                            SpeechLang.HINDI    -> "बोलिए…"
                            SpeechLang.HINGLISH -> "Boliye / Speak…"
                            else                -> "Speak your note…"
                        }
                    )
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                }
                launcher.launch(intent)
            },
            modifier = Modifier.size(72.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(Icons.Default.Mic, contentDescription = "Record voice", modifier = Modifier.size(36.dp))
        }
        Text(
            "Tap to record (${selectedLang.label})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
