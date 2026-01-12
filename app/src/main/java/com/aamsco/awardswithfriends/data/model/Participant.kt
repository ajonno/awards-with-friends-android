package com.aamsco.awardswithfriends.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Participant(
    @DocumentId
    val id: String = "",
    val odCompetitionId: String = "",
    val odUserId: String = "",
    val displayName: String = "",
    val photoURL: String? = null,
    val score: Int = 0,
    val totalVotes: Int = 0,
    @ServerTimestamp
    val joinedAt: Timestamp? = null,
    val blocked: Boolean = false
)
