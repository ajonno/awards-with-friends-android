package com.aamsco.awardswithfriends.data.repository

import com.aamsco.awardswithfriends.data.model.Competition
import com.aamsco.awardswithfriends.data.model.Participant
import com.aamsco.awardswithfriends.data.model.Vote
import com.aamsco.awardswithfriends.data.source.CloudFunctionsDataSource
import com.aamsco.awardswithfriends.data.source.FirestoreDataSource
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompetitionRepository @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val cloudFunctionsDataSource: CloudFunctionsDataSource,
    private val auth: FirebaseAuth
) {
    private val currentUserId: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")

    // ==================== Competitions ====================

    fun competitionsFlow(): Flow<List<Competition>> {
        return firestoreDataSource.competitionsFlow(currentUserId)
    }

    fun competitionFlow(competitionId: String): Flow<Competition?> {
        return firestoreDataSource.competitionFlow(competitionId)
    }

    suspend fun createCompetition(name: String, ceremonyId: String): Map<String, Any> {
        return cloudFunctionsDataSource.createCompetition(name, ceremonyId)
    }

    suspend fun joinCompetition(inviteCode: String): Map<String, Any> {
        return cloudFunctionsDataSource.joinCompetition(inviteCode)
    }

    suspend fun leaveCompetition(competitionId: String) {
        cloudFunctionsDataSource.leaveCompetition(competitionId)
    }

    suspend fun deleteCompetition(competitionId: String) {
        cloudFunctionsDataSource.deleteCompetition(competitionId)
    }

    suspend fun setCompetitionInactive(competitionId: String, inactive: Boolean) {
        cloudFunctionsDataSource.setCompetitionInactive(competitionId, inactive)
    }

    // ==================== Participants ====================

    fun participantsFlow(competitionId: String): Flow<List<Participant>> {
        return firestoreDataSource.participantsFlow(competitionId)
    }

    // ==================== Votes ====================

    fun myVotesFlow(competitionId: String): Flow<List<Vote>> {
        return firestoreDataSource.votesFlow(competitionId, currentUserId)
    }

    fun allVotesFlow(competitionId: String): Flow<List<Vote>> {
        return firestoreDataSource.allVotesFlow(competitionId)
    }

    suspend fun castVote(competitionId: String, categoryId: String, nomineeId: String) {
        cloudFunctionsDataSource.castVote(competitionId, categoryId, nomineeId)
    }

    // ==================== Helpers ====================

    fun isOwner(competition: Competition): Boolean {
        return competition.createdBy == currentUserId
    }
}
