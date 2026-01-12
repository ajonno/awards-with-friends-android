package com.aamsco.awardswithfriends.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

data class Competition(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val ceremonyId: String = "",
    val ceremonyYear: String = "",
    val event: String? = null,
    val inviteCode: String = "",
    val createdBy: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val status: String = "open",
    val participantCount: Int = 0,
    val participantIds: List<String> = emptyList(),
    // Denormalized ceremony info
    val ceremonyName: String = "",
    val eventType: String = ""
) {
    val competitionStatus: CompetitionStatus
        get() = CompetitionStatus.fromString(status)

    val eventDisplayName: String
        get() = ceremonyName.ifEmpty { "Unknown Event" }
}

enum class CompetitionStatus(val value: String) {
    OPEN("open"),
    LOCKED("locked"),
    COMPLETED("completed"),
    INACTIVE("inactive");

    companion object {
        fun fromString(value: String): CompetitionStatus {
            return entries.find { it.value == value } ?: OPEN
        }
    }
}
