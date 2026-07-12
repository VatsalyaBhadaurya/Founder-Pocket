package com.vatsalya.founderpocket.data.model

enum class CaptureType {
    NOTE, VOICE, LINK, MEETING, IDEA, TASK,
    CONTACT, EXPENSE, WIN, PARKING, FOLLOWUP, DOC;

    val displayName: String get() = when (this) {
        NOTE     -> "Note"
        VOICE    -> "Voice"
        LINK     -> "Link"
        MEETING  -> "Meeting"
        IDEA     -> "Idea"
        TASK     -> "Task"
        CONTACT  -> "Contact"
        EXPENSE  -> "Expense"
        WIN      -> "Win"
        PARKING  -> "Parking"
        FOLLOWUP -> "Follow-up"
        DOC      -> "Doc"
    }
}
