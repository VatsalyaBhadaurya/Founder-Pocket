package com.vatsalya.founderpocket.ui.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vatsalya.founderpocket.data.model.Capture
import com.vatsalya.founderpocket.data.model.CaptureType
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
    val isSaving: Boolean = false,
    val saved: Boolean = false
)

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val saveCapture: SaveCaptureUseCase
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

    fun save() {
        val state = _state.value
        if (state.body.isBlank() || state.isSaving) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            saveCapture(
                Capture(
                    createdAt = System.currentTimeMillis(),
                    type = state.selectedType,
                    body = state.body
                )
            )
            _state.value = _state.value.copy(isSaving = false, saved = true)
        }
    }
}
