package com.aamsco.awardswithfriends.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Nominee(
    val id: String = "",
    val title: String = "",
    val subtitle: String? = null,
    val imageUrl: String = "",
    val tmdbId: String? = null
)
