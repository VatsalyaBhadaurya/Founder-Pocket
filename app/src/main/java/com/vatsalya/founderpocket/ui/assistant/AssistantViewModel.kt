package com.vatsalya.founderpocket.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vatsalya.founderpocket.data.ml.LlmManager
import com.vatsalya.founderpocket.data.model.AssistantSuggestion
import com.vatsalya.founderpocket.data.model.SuggestionType
import com.vatsalya.founderpocket.domain.usecase.GetSuggestionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class AssistantUiState(
    val suggestions: List<AssistantSuggestion> = emptyList(),
    val isLoading: Boolean = false,
    val llmAvailable: Boolean = false,
    val modelPath: String = "",
    val aiQuery: String = "What should I work on today?",
    val aiResponse: String = "",
    val isGenerating: Boolean = false,
    // True on first generate() call while the 529 MB copy from assets is in progress
    val isCopyingModel: Boolean = false
)

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val getSuggestions: GetSuggestionsUseCase,
    private val llmManager: LlmManager
) : ViewModel() {

    private val _state = MutableStateFlow(AssistantUiState())
    val state: StateFlow<AssistantUiState> = _state

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val suggestions = getSuggestions()
            _state.value = _state.value.copy(
                suggestions  = suggestions,
                isLoading    = false,
                llmAvailable = llmManager.isAvailable,
                modelPath    = llmManager.modelFile().absolutePath
            )
        }
    }

    fun onQueryChange(text: String) {
        _state.value = _state.value.copy(aiQuery = text)
    }

    fun generate() {
        val s = _state.value
        if (s.isGenerating || s.aiQuery.isBlank()) return
        viewModelScope.launch {
            val firstRun = !llmManager.modelFile().exists() && llmManager.isAvailable
            _state.value = s.copy(
                isGenerating   = true,
                isCopyingModel = firstRun,
                aiResponse     = if (firstRun) "Copying model from assets (first run, ~30 s)…" else ""
            )
            val prompt = buildPrompt(s)
            val buffer = StringBuilder()
            llmManager.generate(prompt) { token ->
                buffer.append(token)
                _state.value = _state.value.copy(aiResponse = buffer.toString(), isCopyingModel = false)
            }
            _state.value = _state.value.copy(isGenerating = false, isCopyingModel = false)
        }
    }

    private fun buildPrompt(s: AssistantUiState): String {
        val today = LocalDate.now().toString()
        val taskLines = s.suggestions
            .filter { it.type in listOf(SuggestionType.OVERDUE_TASK, SuggestionType.DUE_SOON, SuggestionType.NO_DUE_TASK) }
            .joinToString("\n") { "- ${it.title} (${it.subtitle})" }
            .ifBlank { "  None" }
        val followupLines = s.suggestions
            .filter { it.type == SuggestionType.FOLLOWUP_PENDING }
            .joinToString("\n") { "- ${it.title}" }
            .ifBlank { "  None" }

        return """
Today: $today

Open tasks:
$taskLines

Pending follow-ups:
$followupLines

${s.aiQuery}

Answer briefly and practically. Under 100 words.
""".trimIndent()
    }
}
