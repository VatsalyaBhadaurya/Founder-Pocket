package com.vatsalya.founderpocket.domain.usecase

import com.vatsalya.founderpocket.data.model.AssistantSuggestion
import com.vatsalya.founderpocket.data.model.CaptureType
import com.vatsalya.founderpocket.data.model.SuggestionType
import com.vatsalya.founderpocket.data.model.payload.FollowupPayload
import com.vatsalya.founderpocket.data.model.payload.TaskPayload
import com.vatsalya.founderpocket.data.repository.CaptureRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetSuggestionsUseCase @Inject constructor(
    private val repository: CaptureRepository
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend operator fun invoke(): List<AssistantSuggestion> {
        val all = repository.getAll().first()
        val now = System.currentTimeMillis()
        val suggestions = mutableListOf<AssistantSuggestion>()

        // ── Open tasks sorted overdue → due soon → no date ──────────────────
        all.filter { it.type == CaptureType.TASK }
            .mapNotNull { capture ->
                val payload = runCatching { json.decodeFromString<TaskPayload>(capture.payload) }
                    .getOrNull() ?: return@mapNotNull null
                if (payload.done) return@mapNotNull null
                val dueMillis = parseDueMillis(payload.due)
                Triple(capture, payload, dueMillis)
            }
            .sortedWith(compareBy { it.third })
            .forEach { (capture, payload, dueMillis) ->
                val type = when {
                    dueMillis < now               -> SuggestionType.OVERDUE_TASK
                    dueMillis < now + SEVEN_DAYS  -> SuggestionType.DUE_SOON
                    else                          -> SuggestionType.NO_DUE_TASK
                }
                val subtitle = when (type) {
                    SuggestionType.OVERDUE_TASK -> "Overdue"
                    SuggestionType.DUE_SOON     -> "Due ${payload.due}"
                    else                        -> "Task"
                }
                suggestions.add(AssistantSuggestion(capture.id, type, capture.body, subtitle))
            }

        // ── Pending follow-ups sorted by remindAt ────────────────────────────
        all.filter { it.type == CaptureType.FOLLOWUP }
            .mapNotNull { capture ->
                val payload = runCatching { json.decodeFromString<FollowupPayload>(capture.payload) }
                    .getOrNull() ?: return@mapNotNull null
                if (payload.remindAt <= 0) return@mapNotNull null
                Pair(capture, payload)
            }
            .sortedBy { it.second.remindAt }
            .forEach { (capture, payload) ->
                suggestions.add(
                    AssistantSuggestion(
                        captureId = capture.id,
                        type      = SuggestionType.FOLLOWUP_PENDING,
                        title     = payload.subject.ifBlank { capture.body },
                        subtitle  = "Follow-up"
                    )
                )
            }

        // ── Recent wins (encouragement) ──────────────────────────────────────
        all.filter { it.type == CaptureType.WIN }
            .sortedByDescending { it.createdAt }
            .take(1)
            .forEach { capture ->
                suggestions.add(
                    AssistantSuggestion(capture.id, SuggestionType.RECENT_WIN, capture.body, "Recent win")
                )
            }

        return suggestions.take(7)
    }

    private fun parseDueMillis(due: String): Long {
        if (due.isBlank()) return Long.MAX_VALUE
        return runCatching {
            LocalDate.parse(due).atTime(9, 0)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.getOrDefault(Long.MAX_VALUE)
    }

    companion object {
        private const val SEVEN_DAYS = 7 * 24 * 60 * 60 * 1000L
    }
}
