package com.vatsalya.founderpocket.ui.capture

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.vatsalya.founderpocket.data.model.CaptureType
import com.vatsalya.founderpocket.ui.capture.forms.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CaptureScreen(
    onSaved: () -> Unit,
    viewModel: CaptureViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val bodyFocus = remember { FocusRequester() }

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    // Auto-open keyboard for immediate capture (one-minute constitution)
    LaunchedEffect(Unit) {
        runCatching { bodyFocus.requestFocus() }
    }

    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> viewModel.onPhotoSelected(uri) }

    val locationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) viewModel.fetchLocation() }

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
            // Body field (not shown for VOICE — transcript fills it; not for DOC — auto-filled on encryption)
            if (state.selectedType != CaptureType.VOICE && state.selectedType != CaptureType.DOC) {
                OutlinedTextField(
                    value = state.body,
                    onValueChange = viewModel::onBodyChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .focusRequester(bodyFocus),
                    placeholder = { Text("What's on your mind?") },
                    label = { Text(state.selectedType.name) }
                )
            }

            // Quick actions row
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

            // Location chip
            state.location?.let { loc ->
                val label = loc.label ?: "%.4f, %.4f".format(loc.lat, loc.lng)
                AssistChip(
                    onClick = viewModel::clearLocation,
                    label = { Text(label) },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp)) }
                )
            }

            // Source app chip (from share intent)
            state.sourceApp?.let { pkg ->
                val label = pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
                AssistChip(
                    onClick = {},
                    label = { Text("Shared from $label") },
                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            // Photo thumbnail
            state.photoUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = "Attached photo",
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentScale = ContentScale.Crop
                )
            }

            // Type picker
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

            // Payload form section
            if (state.selectedType == CaptureType.VOICE) {
                VoiceForm(onTranscript = viewModel::onTranscript)
                if (state.body.isNotBlank()) {
                    OutlinedTextField(
                        value = state.body,
                        onValueChange = viewModel::onBodyChange,
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        label = { Text("Transcript") }
                    )
                }
            } else {
                PayloadSection(
                    state = state.payloadState,
                    onUpdate = viewModel::onPayloadUpdate,
                    onDocFilePicked = viewModel::onDocFilePicked
                )
            }

            // Tags
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
                Button(onClick = viewModel::addTag, enabled = state.tagInput.isNotBlank()) { Text("Add") }
            }
            if (state.tags.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.save()
                },
                enabled = when (state.selectedType) {
                    CaptureType.DOC -> {
                        val docState = state.payloadState as? PayloadFormState.Doc
                        docState?.encryptedRef?.isNotBlank() == true && !state.isSaving
                    }
                    else -> state.body.isNotBlank() && !state.isSaving
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.isSaving) "Saving…" else "Save")
            }
        }
    }
}

@Composable
private fun PayloadSection(
    state: PayloadFormState,
    onUpdate: (PayloadFormState) -> Unit,
    onDocFilePicked: (android.net.Uri, String) -> Unit
) {
    when (state) {
        is PayloadFormState.Meeting  -> MeetingForm(state) { onUpdate(it) }
        is PayloadFormState.Idea     -> IdeaForm(state) { onUpdate(it) }
        is PayloadFormState.Task     -> TaskForm(state) { onUpdate(it) }
        is PayloadFormState.Followup -> FollowupForm(state) { onUpdate(it) }
        is PayloadFormState.Contact  -> ContactForm(state) { onUpdate(it) }
        is PayloadFormState.Expense  -> ExpenseForm(state) { onUpdate(it) }
        is PayloadFormState.Parking  -> ParkingForm(state) { onUpdate(it) }
        is PayloadFormState.Link     -> LinkForm(state) { onUpdate(it) }
        is PayloadFormState.Doc      -> DocForm(state, onFilePicked = onDocFilePicked)
        PayloadFormState.None        -> {}
    }
}
