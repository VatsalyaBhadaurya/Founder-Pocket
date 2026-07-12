package com.vatsalya.founderpocket.data.model.payload

import kotlinx.serialization.Serializable

@Serializable
data class MeetingPayload(
    val with: String = "",
    val keyPoints: String = "",
    val actionItems: List<String> = emptyList(),
    val deadline: String = ""
)

@Serializable
data class IdeaPayload(
    val problem: String = "",
    val whoHasIt: String = "",
    val solution: String = ""
)

@Serializable
data class TaskPayload(
    val due: String = "",          // ISO date "yyyy-MM-dd"
    val done: Boolean = false
)

@Serializable
data class FollowupPayload(
    val subject: String = "",
    val remindAt: Long = 0L        // epoch millis
)

@Serializable
data class ContactPayload(
    val name: String = "",
    val metAt: String = "",
    val org: String = "",
    val note: String = ""
)

@Serializable
data class ExpensePayload(
    val amount: Double = 0.0,
    val category: String = "other" // food | travel | software | marketing | office | other
)

@Serializable
data class ParkingPayload(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val label: String = "",
    val savedAt: Long = 0L
)

@Serializable
data class LinkPayload(
    val url: String = "",
    val category: String = "web"   // repo | paper | post | video | web
)
