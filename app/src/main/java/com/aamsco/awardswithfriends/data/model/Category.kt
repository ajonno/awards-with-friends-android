package com.aamsco.awardswithfriends.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Category(
    @DocumentId
    val id: String = "",
    val ceremonyYear: String = "",
    val event: String? = null,
    val name: String = "",
    val displayOrder: Int = 0,
    val winnerId: String? = null,
    val correctNomineeIds: List<String>? = null,
    val winnerAnnouncedAt: Timestamp? = null,
    val votingLocked: Boolean? = null,
    val votingLockedAt: Timestamp? = null,
    val hidden: Boolean? = null,
    val nominees: List<Nominee> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val isVotingLocked: Boolean
        get() = votingLocked == true

    @get:Exclude
    val isHidden: Boolean
        get() = hidden == true

    @get:Exclude
    val hasWinner: Boolean
        get() = resolvedCorrectNomineeIds.isNotEmpty()

    @get:Exclude
    val winner: Nominee?
        get() = nominees.find { it.id == winnerId }

    @get:Exclude
    val resolvedCorrectNomineeIds: List<String>
        get() {
            val nomineeIds = correctNomineeIds?.toMutableList() ?: mutableListOf()
            if (winnerId != null && !nomineeIds.contains(winnerId)) {
                nomineeIds.add(0, winnerId)
            }
            return nomineeIds
        }

    fun isCorrectNominee(nomineeId: String): Boolean = resolvedCorrectNomineeIds.contains(nomineeId)
}
