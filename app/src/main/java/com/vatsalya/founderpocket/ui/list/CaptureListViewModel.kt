package com.vatsalya.founderpocket.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vatsalya.founderpocket.data.model.Capture
import com.vatsalya.founderpocket.domain.usecase.SearchCapturesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CaptureListViewModel @Inject constructor(
    private val searchCaptures: SearchCapturesUseCase
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    @OptIn(ExperimentalCoroutinesApi::class)
    val captures: StateFlow<List<Capture>> = _query
        .flatMapLatest { searchCaptures(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(q: String) {
        _query.value = q
    }
}
