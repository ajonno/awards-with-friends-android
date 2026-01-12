package com.aamsco.awardswithfriends.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Vote(
    @DocumentId
    val id: String = "",
    val odUserId: String = "",
    val categoryId: String = "",
    val nomineeId: String = "",
    val votedAt: Timestamp? = null,
    val isCorrect: Boolean? = null
)
