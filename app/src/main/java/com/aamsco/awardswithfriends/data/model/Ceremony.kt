package com.aamsco.awardswithfriends.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Ceremony(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val year: String = "",
    val event: String? = null, // Event type document ID
    val date: Timestamp? = null,
    val status: String = "upcoming",
    val hidden: Boolean = false,
    val categoryCount: Int? = null
) {
    val ceremonyStatus: CeremonyStatus
        get() = CeremonyStatus.fromString(status)
}

enum class CeremonyStatus(val value: String) {
    UPCOMING("upcoming"),
    LIVE("live"),
    COMPLETED("completed");

    companion object {
        fun fromString(value: String): CeremonyStatus {
            // Handle both "complete" and "completed" like iOS
            if (value == "complete" || value == "completed") return COMPLETED
            return entries.find { it.value == value } ?: UPCOMING
        }
    }
}
