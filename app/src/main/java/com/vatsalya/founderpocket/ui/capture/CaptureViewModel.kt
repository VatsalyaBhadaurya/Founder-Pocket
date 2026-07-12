package com.vatsalya.founderpocket.ui.capture

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vatsalya.founderpocket.data.location.LocationRepository
import com.vatsalya.founderpocket.data.model.Capture
import com.vatsalya.founderpocket.data.model.CaptureType
import com.vatsalya.founderpocket.data.model.LocationData
import com.vatsalya.founderpocket.data.model.payload.*
import com.vatsalya.founderpocket.domain.usecase.SaveCaptureUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

// Sealed class holding per-type form state
sealed class PayloadFormState {
    object None : PayloadFormState()
    data class Meeting(
        val with: String = "",
        val keyPoints: String = "",
        val actionItems: List<String> = emptyList(),
        val actionItemInput: String = "",
        val deadline: String = ""
    ) : PayloadFormState()
    data class Idea(
        val problem: String = "",
        val whoHasIt: String = "",
        val solution: String = ""
    ) : PayloadFormState()
    data class Task(val due: String = "") : PayloadFormState()
    data class Followup(
        val subject: String = "",
        val remindDate: String = "",
        val remindHour: Int = 9,
        val remindAt: Long = 0L
    ) : PayloadFormState()
    data class Contact(
        val name: String = "",
        val metAt: String = "",
        val org: String = "",
        val note: String = ""
    ) : PayloadFormState()
    data class Expense(
        val amountStr: String = "",
        val category: String = "other"
    ) : PayloadFormState()
    data class Parking(
        val lat: Double? = null,
        val lng: Double? = null,
        val label: String? = null,
        val savedAt: Long = 0L,
        val isLoading: Boolean = false
    ) : PayloadFormState()
    data class Link(val url: String = "", val category: String = "web") : PayloadFormState()
}

data class CaptureUiState(
    val body: String = "",
    val selectedType: CaptureType = CaptureType.NOTE,
    val showTypePicker: Boolean = false,
    val payloadState: PayloadFormState = PayloadFormState.None,
    // context envelope
    val tags: List<String> = emptyList(),
    val tagInput: String = "",
    val photoUri: Uri? = null,
    val location: LocationData? = null,
    val isLocationFetching: Boolean = false,
    // save lifecycle
    val isSaving: Boolean = false,
    val saved: Boolean = false
)

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val saveCapture: SaveCaptureUseCase,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CaptureUiState())
    val state: StateFlow<CaptureUiState> = _state

    fun onBodyChange(text: String) {
        _state.value = _state.value.copy(body = text, showTypePicker = text.isNotBlank())
    }

    fun onTypeSelect(type: CaptureType) {
        val payload = when (type) {
            CaptureType.MEETING  -> PayloadFormState.Meeting()
            CaptureType.IDEA     -> PayloadFormState.Idea()
            CaptureType.TASK     -> PayloadFormState.Task()
            CaptureType.FOLLOWUP -> PayloadFormState.Followup()
            CaptureType.CONTACT  -> PayloadFormState.Contact()
            CaptureType.EXPENSE  -> PayloadFormState.Expense()
            CaptureType.PARKING  -> {
                fetchParkingLocation()
                PayloadFormState.Parking(isLoading = true)
            }
            CaptureType.LINK     -> PayloadFormState.Link()
            else                 -> PayloadFormState.None
        }
        _state.value = _state.value.copy(selectedType = type, showTypePicker = false, payloadState = payload)
    }

    fun onPayloadUpdate(payload: PayloadFormState) {
        _state.value = _state.value.copy(payloadState = payload)
    }

    fun onTranscript(text: String) {
        _state.value = _state.value.copy(body = text, showTypePicker = text.isNotBlank(), selectedType = CaptureType.VOICE)
    }

    fun prefill(text: String) {
        _state.value = _state.value.copy(body = text, showTypePicker = text.isNotBlank())
    }

    fun onTagInputChange(input: String) {
        _state.value = _state.value.copy(tagInput = input)
    }

    fun addTag() {
        val tag = _state.value.tagInput.trim()
        if (tag.isBlank() || _state.value.tags.contains(tag)) {
            _state.value = _state.value.copy(tagInput = "")
            return
        }
        _state.value = _state.value.copy(tags = _state.value.tags + tag, tagInput = "")
    }

    fun removeTag(tag: String) {
        _state.value = _state.value.copy(tags = _state.value.tags - tag)
    }

    fun onPhotoSelected(uri: Uri?) {
        _state.value = _state.value.copy(photoUri = uri)
    }

    fun fetchLocation() {
        if (_state.value.isLocationFetching) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLocationFetching = true)
            val loc = locationRepository.getCurrent()
            _state.value = _state.value.copy(location = loc, isLocationFetching = false)
        }
    }

    fun clearLocation() {
        _state.value = _state.value.copy(location = null)
    }

    private fun fetchParkingLocation() {
        viewModelScope.launch {
            val loc = locationRepository.getCurrent()
            val parkingState = PayloadFormState.Parking(
                lat = loc?.lat,
                lng = loc?.lng,
                label = loc?.label,
                savedAt = System.currentTimeMillis(),
                isLoading = false
            )
            _state.value = _state.value.copy(payloadState = parkingState)
        }
    }

    fun save() {
        val s = _state.value
        val canSave = when (s.selectedType) {
            CaptureType.VOICE -> s.body.isNotBlank()
            else -> s.body.isNotBlank()
        }
        if (!canSave || s.isSaving) return
        viewModelScope.launch {
            _state.value = s.copy(isSaving = true)
            val payloadJson = encodePayload(s.payloadState, s.selectedType, s.location)
            val ftsExtra = buildPayloadFtsExtra(s.payloadState)
            saveCapture(
                Capture(
                    createdAt  = System.currentTimeMillis(),
                    type       = s.selectedType,
                    body       = s.body,
                    payload    = payloadJson,
                    lat        = s.location?.lat ?: (s.payloadState as? PayloadFormState.Parking)?.lat,
                    lng        = s.location?.lng ?: (s.payloadState as? PayloadFormState.Parking)?.lng,
                    placeLabel = s.location?.label ?: (s.payloadState as? PayloadFormState.Parking)?.label,
                    photoUri   = s.photoUri?.toString(),
                    tags       = encodeTags(s.tags),
                    ftsText    = buildFts(s, ftsExtra)
                ),
                remindAt = (s.payloadState as? PayloadFormState.Followup)?.remindAt
                    ?: if (s.selectedType == CaptureType.TASK) {
                        val due = (s.payloadState as? PayloadFormState.Task)?.due
                        if (due != null && due.isNotBlank()) com.vatsalya.founderpocket.ui.capture.forms.dateAtHourMillis(due, 9) else null
                    } else null
            )
            _state.value = _state.value.copy(isSaving = false, saved = true)
        }
    }

    private fun encodePayload(
        payload: PayloadFormState,
        type: CaptureType,
        location: LocationData?
    ): String = when (payload) {
        is PayloadFormState.Meeting  -> Json.encodeToString(
            MeetingPayload(payload.with, payload.keyPoints, payload.actionItems, payload.deadline)
        )
        is PayloadFormState.Idea     -> Json.encodeToString(
            IdeaPayload(payload.problem, payload.whoHasIt, payload.solution)
        )
        is PayloadFormState.Task     -> Json.encodeToString(TaskPayload(payload.due))
        is PayloadFormState.Followup -> Json.encodeToString(FollowupPayload(payload.subject, payload.remindAt))
        is PayloadFormState.Contact  -> Json.encodeToString(
            ContactPayload(payload.name, payload.metAt, payload.org, payload.note)
        )
        is PayloadFormState.Expense  -> Json.encodeToString(
            ExpensePayload(payload.amountStr.toDoubleOrNull() ?: 0.0, payload.category)
        )
        is PayloadFormState.Parking  -> Json.encodeToString(
            ParkingPayload(
                lat = payload.lat ?: location?.lat ?: 0.0,
                lng = payload.lng ?: location?.lng ?: 0.0,
                label = payload.label ?: location?.label ?: "",
                savedAt = payload.savedAt.takeIf { it > 0 } ?: System.currentTimeMillis()
            )
        )
        is PayloadFormState.Link     -> Json.encodeToString(LinkPayload(payload.url, payload.category))
        PayloadFormState.None        -> "{}"
    }

    private fun buildPayloadFtsExtra(payload: PayloadFormState): String = when (payload) {
        is PayloadFormState.Meeting  -> listOf(payload.with, payload.keyPoints, payload.actionItems.joinToString(" ")).joinToString(" ")
        is PayloadFormState.Idea     -> listOf(payload.problem, payload.whoHasIt, payload.solution).joinToString(" ")
        is PayloadFormState.Followup -> payload.subject
        is PayloadFormState.Contact  -> listOf(payload.name, payload.metAt, payload.org, payload.note).joinToString(" ")
        is PayloadFormState.Link     -> payload.url
        else                         -> ""
    }

    private fun buildFts(s: CaptureUiState, payloadExtra: String): String =
        listOfNotNull(
            s.body,
            s.location?.label,
            s.tags.joinToString(" ").ifBlank { null },
            payloadExtra.ifBlank { null }
        ).joinToString(" ")

    private fun encodeTags(tags: List<String>): String =
        if (tags.isEmpty()) "[]"
        else "[\"${tags.joinToString("\",\"")}\"]"
}
