package com.vatsalya.founderpocket.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TimerState { IDLE, RUNNING, PAUSED, DONE }

data class FocusTimerUiState(
    val durationMinutes: Int = 25,
    val remainingSeconds: Int = 25 * 60,
    val timerState: TimerState = TimerState.IDLE
)

@HiltViewModel
class FocusTimerViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(FocusTimerUiState())
    val uiState: StateFlow<FocusTimerUiState> = _uiState

    private var timerJob: Job? = null

    fun setDuration(minutes: Int) {
        if (_uiState.value.timerState == TimerState.RUNNING) return
        _uiState.value = FocusTimerUiState(durationMinutes = minutes, remainingSeconds = minutes * 60)
    }

    fun start() {
        val state = _uiState.value
        if (state.timerState == TimerState.RUNNING) return
        _uiState.value = state.copy(timerState = TimerState.RUNNING)
        timerJob = viewModelScope.launch {
            while (_uiState.value.remainingSeconds > 0) {
                delay(1_000L)
                val remaining = _uiState.value.remainingSeconds - 1
                _uiState.value = _uiState.value.copy(
                    remainingSeconds = remaining,
                    timerState = if (remaining == 0) TimerState.DONE else TimerState.RUNNING
                )
            }
        }
    }

    fun pause() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(timerState = TimerState.PAUSED)
    }

    fun reset() {
        timerJob?.cancel()
        val dur = _uiState.value.durationMinutes
        _uiState.value = FocusTimerUiState(durationMinutes = dur, remainingSeconds = dur * 60)
    }
}
