package com.vatsalya.founderpocket.ui.detail

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vatsalya.founderpocket.data.model.Capture
import com.vatsalya.founderpocket.data.model.CaptureType
import com.vatsalya.founderpocket.data.model.payload.*
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: CaptureDetailViewModel = hiltViewModel()
) {
    val capture by viewModel.capture.collectAsState()
    val deleted by viewModel.deleted.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(deleted) { if (deleted) onBack() }

    if (capture == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val c = capture!!

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        c.type.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onEdit(c.id) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, buildShareText(c))
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share capture"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = viewModel::requestDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Body
            if (c.body.isNotBlank()) {
                Text(c.body, style = MaterialTheme.typography.bodyLarge)
            }

            // Metadata row
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    formatDate(c.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                c.placeLabel?.let {
                    Text("📍 $it",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                c.sourceApp?.let { pkg ->
                    Text("via ${pkg.substringAfterLast('.')}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalDivider()

            // Payload details
            PayloadDisplay(c.type, c.payload)

            // Tags
            val tags = runCatching {
                Json.decodeFromString<List<String>>(c.tags)
            }.getOrDefault(emptyList())
            if (tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tags.forEach { tag ->
                        SuggestionChip(onClick = {}, label = { Text(tag) })
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("Delete capture?") },
            text  = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDelete) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PayloadDisplay(type: CaptureType, payloadJson: String) {
    val json = remember { Json { ignoreUnknownKeys = true } }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        when (type) {
            CaptureType.MEETING -> {
                val p = runCatching { json.decodeFromString<MeetingPayload>(payloadJson) }.getOrNull() ?: return
                DetailRow("With", p.with)
                DetailRow("Key points", p.keyPoints)
                if (p.actionItems.isNotEmpty()) {
                    Text("Action items", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    p.actionItems.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                }
                DetailRow("Deadline", p.deadline)
            }
            CaptureType.IDEA -> {
                val p = runCatching { json.decodeFromString<IdeaPayload>(payloadJson) }.getOrNull() ?: return
                DetailRow("Problem", p.problem)
                DetailRow("Who has it", p.whoHasIt)
                DetailRow("Solution", p.solution)
            }
            CaptureType.TASK -> {
                val p = runCatching { json.decodeFromString<TaskPayload>(payloadJson) }.getOrNull() ?: return
                DetailRow("Due", p.due)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Status:", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (p.done) "Done" else "Open",
                        style = MaterialTheme.typography.bodySmall,
                        textDecoration = if (p.done) TextDecoration.LineThrough else TextDecoration.None
                    )
                }
            }
            CaptureType.FOLLOWUP -> {
                val p = runCatching { json.decodeFromString<FollowupPayload>(payloadJson) }.getOrNull() ?: return
                DetailRow("Subject", p.subject)
                if (p.remindAt > 0) DetailRow("Remind at", formatDate(p.remindAt))
            }
            CaptureType.CONTACT -> {
                val p = runCatching { json.decodeFromString<ContactPayload>(payloadJson) }.getOrNull() ?: return
                DetailRow("Name", p.name)
                DetailRow("Met at", p.metAt)
                DetailRow("Org", p.org)
                DetailRow("Note", p.note)
            }
            CaptureType.EXPENSE -> {
                val p = runCatching { json.decodeFromString<ExpensePayload>(payloadJson) }.getOrNull() ?: return
                DetailRow("Amount", "₹%.2f".format(p.amount))
                DetailRow("Category", p.category)
            }
            CaptureType.PARKING -> {
                val p = runCatching { json.decodeFromString<ParkingPayload>(payloadJson) }.getOrNull() ?: return
                DetailRow("Location", p.label)
                DetailRow("Coordinates", "%.4f, %.4f".format(p.lat, p.lng))
            }
            CaptureType.LINK -> {
                val p = runCatching { json.decodeFromString<LinkPayload>(payloadJson) }.getOrNull() ?: return
                DetailRow("URL", p.url)
                DetailRow("Type", p.category)
            }
            CaptureType.DOC -> {
                val p = runCatching { json.decodeFromString<DocPayload>(payloadJson) }.getOrNull() ?: return
                DetailRow("Document type", p.docType)
                if (p.encryptedRef.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Encrypted on device",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$label:", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(min = 72.dp))
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

private fun buildShareText(c: Capture) = buildString {
    append(c.body)
    c.placeLabel?.let { append("\n📍 $it") }
    append("\n— ${c.type.displayName} captured ${formatDate(c.createdAt)} via Founder Pocket")
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(millis))
