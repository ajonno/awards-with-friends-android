package com.aamsco.awardswithfriends.data.source

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudFunctionsDataSource @Inject constructor(
    private val functions: FirebaseFunctions
) {
    suspend fun createCompetition(name: String, ceremonyYear: String, event: String?): Map<String, Any> {
        val data = hashMapOf(
            "name" to name,
            "ceremonyYear" to ceremonyYear
        )
        if (event != null) {
            data["event"] = event
        }
        val result = functions
            .getHttpsCallable("createCompetition")
            .call(data)
            .await()

        @Suppress("UNCHECKED_CAST")
        return result.data as? Map<String, Any> ?: emptyMap()
    }

    suspend fun joinCompetition(inviteCode: String): Map<String, Any> {
        val data = hashMapOf(
            "inviteCode" to inviteCode.uppercase()
        )
        val result = functions
            .getHttpsCallable("joinCompetition")
            .call(data)
            .await()

        @Suppress("UNCHECKED_CAST")
        return result.data as? Map<String, Any> ?: emptyMap()
    }

    suspend fun leaveCompetition(competitionId: String) {
        val data = hashMapOf(
            "competitionId" to competitionId
        )
        functions
            .getHttpsCallable("leaveCompetition")
            .call(data)
            .await()
    }

    suspend fun deleteCompetition(competitionId: String) {
        val data = hashMapOf(
            "competitionId" to competitionId
        )
        functions
            .getHttpsCallable("deleteCompetition")
            .call(data)
            .await()
    }

    suspend fun castVote(
        competitionId: String,
        categoryId: String,
        nomineeId: String
    ) {
        val data = hashMapOf(
            "competitionId" to competitionId,
            "categoryId" to categoryId,
            "nomineeId" to nomineeId
        )
        functions
            .getHttpsCallable("castVote")
            .call(data)
            .await()
    }

    suspend fun castCeremonyVote(
        ceremonyYear: String,
        categoryId: String,
        nomineeId: String
    ) {
        val data = hashMapOf(
            "ceremonyYear" to ceremonyYear,
            "categoryId" to categoryId,
            "nomineeId" to nomineeId
        )
        functions
            .getHttpsCallable("castCeremonyVote")
            .call(data)
            .await()
    }

    suspend fun updateFcmToken(token: String) {
        val data = hashMapOf(
            "token" to token
        )
        functions
            .getHttpsCallable("updateFcmToken")
            .call(data)
            .await()
    }

    suspend fun deleteAccount() {
        functions
            .getHttpsCallable("deleteAccount")
            .call()
            .await()
    }

    suspend fun setCompetitionInactive(competitionId: String, inactive: Boolean) {
        val data = hashMapOf(
            "competitionId" to competitionId,
            "inactive" to inactive
        )
        functions
            .getHttpsCallable("setCompetitionInactive")
            .call(data)
            .await()
    }
}
