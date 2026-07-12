package com.vatsalya.founderpocket.domain.usecase

import com.vatsalya.founderpocket.data.model.Capture
import com.vatsalya.founderpocket.data.repository.CaptureRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class SearchCapturesUseCase @Inject constructor(
    private val repository: CaptureRepository
) {
    operator fun invoke(query: String): Flow<List<Capture>> {
        if (query.isBlank()) return repository.getAll()
        return repository.searchKeyword(query)
    }
}
