package com.vatsalya.founderpocket.ui.home

import androidx.lifecycle.ViewModel
import com.vatsalya.founderpocket.data.model.Capture
import com.vatsalya.founderpocket.data.repository.CaptureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: CaptureRepository
) : ViewModel() {
    val todayFocus: Flow<List<Capture>> = repository.getTodayFocus()
    val recentCaptures: Flow<List<Capture>> = repository.getAll()
}
