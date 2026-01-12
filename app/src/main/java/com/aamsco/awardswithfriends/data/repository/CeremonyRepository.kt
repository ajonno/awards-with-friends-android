package com.aamsco.awardswithfriends.data.repository

import com.aamsco.awardswithfriends.data.model.Category
import com.aamsco.awardswithfriends.data.model.Ceremony
import com.aamsco.awardswithfriends.data.model.EventTypeData
import com.aamsco.awardswithfriends.data.model.Nominee
import com.aamsco.awardswithfriends.data.model.Vote
import com.aamsco.awardswithfriends.data.source.CloudFunctionsDataSource
import com.aamsco.awardswithfriends.data.source.FirestoreDataSource
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CeremonyRepository @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val cloudFunctionsDataSource: CloudFunctionsDataSource,
    private val auth: FirebaseAuth
) {
    private val currentUserId: String
        get() = auth.currentUser?.uid ?: ""
    // ==================== Event Types ====================

    fun eventTypesFlow(): Flow<List<EventTypeData>> {
        return firestoreDataSource.eventTypesFlow()
    }

    // ==================== Ceremonies ====================

    fun ceremoniesFlow(): Flow<List<Ceremony>> {
        return firestoreDataSource.ceremoniesFlow()
    }

    fun ceremonyFlow(ceremonyId: String): Flow<Ceremony?> {
        return firestoreDataSource.ceremonyFlow(ceremonyId)
    }

    // ==================== Categories ====================

    fun categoriesFlow(ceremonyYear: String, event: String?): Flow<List<Category>> {
        return firestoreDataSource.categoriesFlow(ceremonyYear, event)
    }

    fun categoryFlow(ceremonyYear: String, event: String?, categoryId: String): Flow<Category?> {
        return firestoreDataSource.categoryFlow(ceremonyYear, event, categoryId)
    }

    // Legacy method for older competition data that uses ceremonyId
    fun categoriesFlowByCeremonyId(ceremonyId: String): Flow<List<Category>> {
        return firestoreDataSource.categoriesFlowByCeremonyId(ceremonyId)
    }

    // ==================== Nominees ====================

    fun nomineesFlow(ceremonyId: String, categoryId: String): Flow<List<Nominee>> {
        return firestoreDataSource.nomineesFlow(ceremonyId, categoryId)
    }

    // ==================== Voting ====================

    fun ceremonyVotesFlow(ceremonyYear: String, event: String?): Flow<Map<String, Vote>> {
        return firestoreDataSource.ceremonyVotesFlow(currentUserId, ceremonyYear, event)
    }

    suspend fun castCeremonyVote(
        ceremonyYear: String,
        categoryId: String,
        nomineeId: String
    ) {
        cloudFunctionsDataSource.castCeremonyVote(ceremonyYear, categoryId, nomineeId)
    }
}
