package com.vatsalya.founderpocket.ui.capture

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.vatsalya.founderpocket.data.model.CaptureType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CaptureScreen(
    onSaved: () -> Unit,
    viewModel: CaptureViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    // Photo picker
    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> viewModel.onPhotoSelected(uri) }

    // Location permission launcher
    val locationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.fetchLocation()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New capture") },
                navigationIcon = {
                    IconButton(onClick = onSaved) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Body ────────────────────────────────────────────
            OutlinedTextField(
                value = state.body,
                onValueChange = viewModel::onBodyChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                placeholder = { Text("What's on your mind?") },
                label = { Text(state.selectedType.name) }
            )

            // ── Quick actions row ────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = {
                    clipboard.getText()?.text?.let { viewModel.prefill(it) }
                }) {
                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste from clipboard")
                }
                IconButton(onClick = {
                    photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Attach photo")
                }
                // Location toggle
                if (state.isLocationFetching) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(4.dp))
                } else if (state.location != null) {
                    IconButton(onClick = viewModel::clearLocation) {
                        Icon(Icons.Default.LocationOff, contentDescription = "Remove location",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    IconButton(onClick = {
                        val perm = Manifest.permission.ACCESS_FINE_LOCATION
                        if (ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED) {
                            viewModel.fetchLocation()
                        } else {
                            locationPermLauncher.launch(perm)
                        }
                    }) {
                        Icon(Icons.Default.LocationOn, contentDescription = "Attach location")
                    }
                }
            }

            // ── Location chip ────────────────────────────────────
            state.location?.let { loc ->
                val label = loc.label ?: "%.4f, %.4f".format(loc.lat, loc.lng)
                AssistChip(
                    onClick = viewModel::clearLocation,
                    label = { Text(label) },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp)) }
                )
            }

            // ── Photo thumbnail ──────────────────────────────────
            state.photoUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = "Attached photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentScale = ContentScale.Crop
                )
            }

            // ── Type picker (appears after text is entered) ──────
            if (state.showTypePicker) {
                Text("Type", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CaptureType.entries.forEach { type ->
                        FilterChip(
                            selected = state.selectedType == type,
                            onClick = { viewModel.onTypeSelect(type) },
                            label = { Text(type.name) }
                        )
                    }
                }
            }

            // ── Tags ─────────────────────────────────────────────
            Text("Tags", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.tagInput,
                    onValueChange = viewModel::onTagInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Add tag…") },
                    singleLine = true
                )
                Button(
                    onClick = viewModel::addTag,
                    enabled = state.tagInput.isNotBlank()
                ) { Text("Add") }
            }
            if (state.tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    state.tags.forEach { tag ->
                        InputChip(
                            selected = false,
                            onClick = { viewModel.removeTag(tag) },
                            label = { Text(tag) },
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove tag", modifier = Modifier.size(14.dp)) }
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = viewModel::save,
                enabled = state.body.isNotBlank() && !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.isSaving) "Saving…" else "Save")
            }
        }
    }
}
