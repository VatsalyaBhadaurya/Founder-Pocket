package com.vatsalya.founderpocket.data.model

enum class SuggestionType {
    OVERDUE_TASK,
    DUE_SOON,
    NO_DUE_TASK,
    FOLLOWUP_PENDING,
    RECENT_WIN
}

data class AssistantSuggestion(
    val captureId: Long,
    val type: SuggestionType,
    val title: String,
    val subtitle: String
)
