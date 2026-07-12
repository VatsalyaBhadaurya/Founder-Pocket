package com.vatsalya.founderpocket.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vatsalya.founderpocket.data.model.Capture
import com.vatsalya.founderpocket.domain.usecase.SearchCapturesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SearchMode { KEYWORD, SEMANTIC }

data class SearchUiState(
    val query: String = "",
    val mode: SearchMode = SearchMode.KEYWORD,
    val semanticResults: List<Capture> = emptyList(),
    val isSearching: Boolean = false,
    val modelReady: Boolean = false
)

@HiltViewModel
class CaptureListViewModel @Inject constructor(
    private val searchCaptures: SearchCapturesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState

    private val _query = MutableStateFlow("")
    private var semanticJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val keywordResults: StateFlow<List<Capture>> = _query
        .flatMapLatest { searchCaptures.keyword(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(q: String) {
        _query.value = q
        _uiState.value = _uiState.value.copy(query = q)
        if (_uiState.value.mode == SearchMode.SEMANTIC) launchSemantic(q)
    }

    fun onModeChange(mode: SearchMode) {
        _uiState.value = _uiState.value.copy(mode = mode)
        if (mode == SearchMode.SEMANTIC) launchSemantic(_uiState.value.query)
    }

    private fun launchSemantic(query: String) {
        semanticJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(semanticResults = emptyList(), isSearching = false)
            return
        }
        semanticJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true)
            val results = searchCaptures.semantic(query)
            _uiState.value = _uiState.value.copy(
                semanticResults = results,
                isSearching = false,
                modelReady = results.isNotEmpty() || query.isNotBlank()
            )
        }
    }
}
