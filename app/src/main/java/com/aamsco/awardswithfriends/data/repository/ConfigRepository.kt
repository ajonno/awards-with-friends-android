package com.aamsco.awardswithfriends.data.repository

import com.aamsco.awardswithfriends.data.source.FirestoreDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigRepository @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource
) {
    fun requiresPaymentFlow(): Flow<Boolean> {
        return firestoreDataSource.configFlow().map { config ->
            config["requiresPaymentForCompetitions"] as? Boolean ?: true
        }
    }
}
