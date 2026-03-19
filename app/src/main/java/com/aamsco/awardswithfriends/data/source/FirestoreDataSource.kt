package com.aamsco.awardswithfriends.data.source

import com.aamsco.awardswithfriends.data.model.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class FirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun ceremonyKey(ceremonyYear: String, event: String?): String =
        "${event ?: "default"}:$ceremonyYear"

    private fun ceremonyVotesQuery(ceremonyYear: String, event: String?) =
        firestore.collection("ceremonyVotes")
            .whereEqualTo("ceremonyKey", ceremonyKey(ceremonyYear, event))

    // ==================== Users ====================

    fun userFlow(uid: String): Flow<User?> {
        return firestore.collection("users")
            .document(uid)
            .snapshots()
            .map { it.toObject<User>() }
    }

    suspend fun updateUser(uid: String, updates: Map<String, Any>) {
        firestore.collection("users")
            .document(uid)
            .update(updates)
    }

    // ==================== Competitions ====================

    fun competitionsFlow(userId: String): Flow<List<Competition>> {
        // Query participants collectionGroup to find competitions user is in
        return firestore.collectionGroup("participants")
            .whereEqualTo("odUserId", userId)
            .snapshots()
            .map { snapshot ->
                // Get competition IDs from participant documents
                snapshot.documents.mapNotNull { doc ->
                    doc.reference.parent.parent?.id
                }.distinct()
            }
            .flatMapLatest { competitionIds ->
                if (competitionIds.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    // Listen to all competitions
                    combine(
                        competitionIds.map { id ->
                            firestore.collection("competitions")
                                .document(id)
                                .snapshots()
                                .map { it.toObject<Competition>() }
                        }
                    ) { competitions ->
                        competitions.filterNotNull().toList()
                    }
                }
            }
    }

    fun competitionFlow(competitionId: String): Flow<Competition?> {
        return firestore.collection("competitions")
            .document(competitionId)
            .snapshots()
            .map { it.toObject<Competition>() }
    }

    // ==================== Ceremonies ====================

    fun ceremoniesFlow(): Flow<List<Ceremony>> {
        return firestore.collection("ceremonies")
            .orderBy("date", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot -> snapshot.toObjects<Ceremony>() }
    }

    fun ceremonyFlow(ceremonyId: String): Flow<Ceremony?> {
        return firestore.collection("ceremonies")
            .document(ceremonyId)
            .snapshots()
            .map { it.toObject<Ceremony>() }
    }

    // ==================== Categories ====================

    // Categories are stored in top-level "categories" collection, linked by ceremonyYear and event
    fun categoriesFlow(ceremonyYear: String, event: String?): Flow<List<Category>> {
        val query = firestore.collection("categories")
            .whereEqualTo("ceremonyYear", ceremonyYear)
            .orderBy("displayOrder")

        // Filter by event if provided (done client-side like web admin to avoid composite index)
        return query.snapshots()
            .map { snapshot ->
                val categories = snapshot.toObjects<Category>()
                if (event != null) {
                    categories.filter { it.event == null || it.event == event }
                } else {
                    categories
                }
            }
    }

    fun categoryFlow(ceremonyYear: String, event: String?, categoryId: String): Flow<Category?> {
        return categoriesFlow(ceremonyYear, event)
            .map { categories -> categories.find { it.id == categoryId } }
    }

    // Legacy method for backwards compatibility - fetches by ceremonyId (for older competition data)
    fun categoriesFlowByCeremonyId(ceremonyId: String): Flow<List<Category>> {
        return firestore.collection("ceremonies")
            .document(ceremonyId)
            .collection("categories")
            .orderBy("displayOrder")
            .snapshots()
            .map { snapshot -> snapshot.toObjects<Category>() }
    }

    // ==================== Nominees ====================

    fun nomineesFlow(ceremonyId: String, categoryId: String): Flow<List<Nominee>> {
        return firestore.collection("ceremonies")
            .document(ceremonyId)
            .collection("categories")
            .document(categoryId)
            .collection("nominees")
            .snapshots()
            .map { snapshot -> snapshot.toObjects<Nominee>() }
    }

    // ==================== Participants ====================

    fun participantsFlow(competitionId: String): Flow<List<Participant>> {
        return firestore.collection("competitions")
            .document(competitionId)
            .collection("participants")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects<Participant>().filter { !it.blocked }
            }
    }

    // ==================== Votes ====================

    fun votesFlow(competitionId: String, userId: String): Flow<List<Vote>> {
        return competitionFlow(competitionId)
            .flatMapLatest { competition ->
                if (competition == null) {
                    flowOf(emptyList())
                } else {
                    ceremonyVotesQuery(competition.ceremonyYear, competition.event)
                        .whereEqualTo("odUserId", userId)
                        .snapshots()
                        .map { snapshot -> snapshot.toObjects<Vote>() }
                }
            }
    }

    suspend fun votesForUser(competitionId: String, userId: String): List<Vote> {
        val competition = firestore.collection("competitions")
            .document(competitionId)
            .get()
            .await()
            .toObject<Competition>() ?: return emptyList()

        return ceremonyVotesQuery(competition.ceremonyYear, competition.event)
            .whereEqualTo("odUserId", userId)
            .get()
            .await()
            .toObjects()
    }

    fun allVotesFlow(competitionId: String): Flow<List<Vote>> {
        return flowOf(emptyList())
    }

    // Get vote for a single category (used for vote confirmation)
    fun categoryVoteFlow(userId: String, ceremonyYear: String, event: String?, categoryId: String): Flow<Vote?> {
        return ceremonyVotesQuery(ceremonyYear, event)
            .whereEqualTo("odUserId", userId)
            .whereEqualTo("categoryId", categoryId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects<Vote>().maxByOrNull { it.votedAt?.seconds ?: 0 }
            }
    }

    // Get votes for all competitions matching a ceremony year/event
    fun ceremonyVotesFlow(userId: String, ceremonyYear: String, event: String?): Flow<Map<String, Vote>> {
        return ceremonyVotesQuery(ceremonyYear, event)
            .whereEqualTo("odUserId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects<Vote>().associateBy { it.categoryId }
            }
    }

    // ==================== Event Types ====================

    fun eventTypesFlow(): Flow<List<EventTypeData>> {
        return firestore.collection("eventTypes")
            .snapshots()
            .map { snapshot -> snapshot.toObjects<EventTypeData>() }
    }

    // ==================== Config ====================

    fun configFlow(): Flow<Map<String, Any>> {
        return firestore.collection("config")
            .document("features")
            .snapshots()
            .map { snapshot -> snapshot.data ?: emptyMap() }
    }
}
