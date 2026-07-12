package com.vatsalya.founderpocket.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vatsalya.founderpocket.data.model.Capture
import com.vatsalya.founderpocket.data.model.CaptureType
import com.vatsalya.founderpocket.data.model.AssistantSuggestion
import com.vatsalya.founderpocket.data.model.payload.TaskPayload
import com.vatsalya.founderpocket.data.repository.CaptureRepository
import com.vatsalya.founderpocket.domain.usecase.GetSuggestionsUseCase
import com.vatsalya.founderpocket.domain.usecase.SaveCaptureUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val repository: CaptureRepository,
    private val getSuggestions: GetSuggestionsUseCase,
    private val saveCapture: SaveCaptureUseCase
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    val focusItems: StateFlow<List<Capture>> = repository.getTodayFocus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _suggestions = MutableStateFlow<List<AssistantSuggestion>>(emptyList())
    val suggestions: StateFlow<List<AssistantSuggestion>> = _suggestions.asStateFlow()

    private val _shippedText = MutableStateFlow("")
    val shippedText: StateFlow<String> = _shippedText.asStateFlow()

    init { loadSuggestions() }

    fun onShippedChange(text: String) { _shippedText.value = text }

    fun saveShipped() {
        val text = _shippedText.value.trim()
        if (text.isBlank()) return
        viewModelScope.launch {
            saveCapture(
                Capture(
                    createdAt = System.currentTimeMillis(),
                    type      = CaptureType.WIN,
                    body      = text,
                    ftsText   = text
                )
            )
            _shippedText.value = ""
        }
    }

    fun toggleTaskDone(capture: Capture) {
        viewModelScope.launch {
            runCatching {
                val payload = json.decodeFromString<TaskPayload>(capture.payload)
                val updated = capture.copy(
                    payload = json.encodeToString(payload.copy(done = !payload.done))
                )
                repository.update(updated)
            }
        }
    }

    private fun loadSuggestions() {
        viewModelScope.launch { _suggestions.value = getSuggestions() }
    }
}
