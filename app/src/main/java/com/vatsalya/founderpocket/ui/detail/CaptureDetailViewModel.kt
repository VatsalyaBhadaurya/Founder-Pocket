package com.vatsalya.founderpocket.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vatsalya.founderpocket.data.model.Capture
import com.vatsalya.founderpocket.data.repository.CaptureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CaptureDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CaptureRepository
) : ViewModel() {

    private val captureId: Long = savedStateHandle["captureId"] ?: 0L

    private val _capture = MutableStateFlow<Capture?>(null)
    val capture: StateFlow<Capture?> = _capture

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog

    init {
        viewModelScope.launch { _capture.value = repository.getById(captureId) }
    }

    fun requestDelete() { _showDeleteDialog.value = true }
    fun cancelDelete()  { _showDeleteDialog.value = false }

    fun confirmDelete() {
        viewModelScope.launch {
            repository.delete(captureId)
            _showDeleteDialog.value = false
            _deleted.value = true
        }
    }
}
