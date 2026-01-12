package com.aamsco.awardswithfriends.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class EventTypeData(
    @DocumentId
    val id: String = "",
    val slug: String = "",
    val displayName: String = "",
    val color: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)
