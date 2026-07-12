package com.vatsalya.founderpocket.ui.recall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vatsalya.founderpocket.data.model.Capture
import com.vatsalya.founderpocket.data.repository.CaptureRepository
import com.vatsalya.founderpocket.domain.usecase.SearchCapturesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class TimeFilter { ALL, TODAY, WEEK }

data class RecallUiState(
    val query: String = "",
    val timeFilter: TimeFilter = TimeFilter.ALL,
    val nearOnly: Boolean = false,
    val isSearching: Boolean = false,
    val useSemanticSearch: Boolean = false
)

@HiltViewModel
class RecallViewModel @Inject constructor(
    private val repository: CaptureRepository,
    private val searchCaptures: SearchCapturesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecallUiState())
    val uiState: StateFlow<RecallUiState> = _uiState.asStateFlow()

    private val _query = MutableStateFlow("")
    private val _semanticResults = MutableStateFlow<List<Capture>>(emptyList())
    private var semanticJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    private val keywordFlow: Flow<List<Capture>> = _query
        .flatMapLatest { q ->
            if (q.isBlank()) repository.getAll()
            else searchCaptures.keyword(q)
        }

    val results: StateFlow<List<Capture>> = combine(
        keywordFlow,
        _semanticResults,
        _uiState
    ) { keyword, semantic, state ->
        val base = if (state.useSemanticSearch) semantic else keyword
        applyFilters(base, state.timeFilter, state.nearOnly)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(q: String) {
        _query.value = q
        _uiState.value = _uiState.value.copy(query = q)
        if (_uiState.value.useSemanticSearch) launchSemantic(q)
    }

    fun onTimeFilter(filter: TimeFilter) {
        _uiState.value = _uiState.value.copy(timeFilter = filter)
    }

    fun onNearToggle() {
        _uiState.value = _uiState.value.copy(nearOnly = !_uiState.value.nearOnly)
    }

    fun onSemanticToggle() {
        val nowSemantic = !_uiState.value.useSemanticSearch
        _uiState.value = _uiState.value.copy(useSemanticSearch = nowSemantic)
        if (nowSemantic) launchSemantic(_uiState.value.query)
    }

    private fun launchSemantic(query: String) {
        semanticJob?.cancel()
        if (query.isBlank()) { _semanticResults.value = emptyList(); return }
        semanticJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true)
            _semanticResults.value = searchCaptures.semantic(query)
            _uiState.value = _uiState.value.copy(isSearching = false)
        }
    }

    private fun applyFilters(
        list: List<Capture>,
        timeFilter: TimeFilter,
        nearOnly: Boolean
    ): List<Capture> {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
        val todayStart = cal.timeInMillis
        val weekStart  = todayStart - 6L * 24 * 60 * 60 * 1000

        return list.filter { c ->
            val timeOk = when (timeFilter) {
                TimeFilter.ALL   -> true
                TimeFilter.TODAY -> c.createdAt >= todayStart
                TimeFilter.WEEK  -> c.createdAt >= weekStart
            }
            val nearOk = if (nearOnly) c.lat != null else true
            timeOk && nearOk
        }
    }
}
