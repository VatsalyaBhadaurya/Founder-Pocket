package com.vatsalya.founderpocket.ui.capture

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vatsalya.founderpocket.data.location.LocationRepository
import com.vatsalya.founderpocket.data.model.Capture
import com.vatsalya.founderpocket.data.model.CaptureType
import com.vatsalya.founderpocket.data.model.LocationData
import com.vatsalya.founderpocket.domain.usecase.SaveCaptureUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CaptureUiState(
    val body: String = "",
    val selectedType: CaptureType = CaptureType.NOTE,
    val showTypePicker: Boolean = false,
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
        _state.value = _state.value.copy(selectedType = type, showTypePicker = false)
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
        _state.value = _state.value.copy(
            tags = _state.value.tags + tag,
            tagInput = ""
        )
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

    fun save() {
        val s = _state.value
        if (s.body.isBlank() || s.isSaving) return
        viewModelScope.launch {
            _state.value = s.copy(isSaving = true)
            saveCapture(
                Capture(
                    createdAt  = System.currentTimeMillis(),
                    type       = s.selectedType,
                    body       = s.body,
                    lat        = s.location?.lat,
                    lng        = s.location?.lng,
                    placeLabel = s.location?.label,
                    photoUri   = s.photoUri?.toString(),
                    tags       = s.tags.joinToString(",").let { if (it.isBlank()) "[]" else "[\"${it.replace(",", "\",\"")}\"]" },
                    ftsText    = buildFts(s)
                )
            )
            _state.value = _state.value.copy(isSaving = false, saved = true)
        }
    }

    private fun buildFts(s: CaptureUiState): String =
        listOfNotNull(
            s.body,
            s.location?.label,
            s.tags.joinToString(" ").ifBlank { null }
        ).joinToString(" ")
}
