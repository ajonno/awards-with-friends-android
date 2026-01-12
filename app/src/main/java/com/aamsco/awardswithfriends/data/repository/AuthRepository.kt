package com.aamsco.awardswithfriends.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.aamsco.awardswithfriends.data.model.User
import com.aamsco.awardswithfriends.data.source.CloudFunctionsDataSource
import com.aamsco.awardswithfriends.data.source.FirestoreDataSource
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val firestoreDataSource: FirestoreDataSource,
    private val cloudFunctionsDataSource: CloudFunctionsDataSource,
    @ApplicationContext private val context: Context
) {
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isAuthenticated: Boolean
        get() = auth.currentUser != null

    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun currentUserFlow(): Flow<User?> {
        val uid = currentUser?.uid ?: return callbackFlow { trySend(null); awaitClose() }
        return firestoreDataSource.userFlow(uid)
    }

    // ==================== Google Sign-In ====================

    suspend fun signInWithGoogle(activityContext: Context): FirebaseUser {
        val credentialManager = CredentialManager.create(activityContext)

        // Get your web client ID from google-services.json or Firebase console
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(getWebClientId())
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(activityContext, request)
        val credential = result.credential

        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(
                googleIdTokenCredential.idToken,
                null
            )
            val authResult = auth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user ?: throw Exception("Sign in failed")

            // Create/update user document
            createOrUpdateUserDocument(user)

            return user
        } else {
            throw Exception("Invalid credential type")
        }
    }

    // ==================== Email/Password ====================

    suspend fun signInWithEmail(email: String, password: String): FirebaseUser {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return result.user ?: throw Exception("Sign in failed")
    }

    suspend fun signUpWithEmail(email: String, password: String): FirebaseUser {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw Exception("Sign up failed")

        // Create user document
        createOrUpdateUserDocument(user)

        return user
    }

    // ==================== Sign Out & Delete ====================

    fun signOut() {
        auth.signOut()
    }

    suspend fun deleteAccount() {
        cloudFunctionsDataSource.deleteAccount()
        auth.currentUser?.delete()?.await()
    }

    // ==================== Helpers ====================

    private suspend fun createOrUpdateUserDocument(user: FirebaseUser) {
        val userDoc = firestore.collection("users").document(user.uid)
        val snapshot = userDoc.get().await()

        if (!snapshot.exists()) {
            // Create new user
            val newUser = hashMapOf(
                "uid" to user.uid,
                "email" to (user.email ?: ""),
                "displayName" to (user.displayName ?: user.email?.substringBefore("@") ?: "User"),
                "photoURL" to user.photoUrl?.toString(),
                "createdAt" to com.google.firebase.Timestamp.now(),
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
            userDoc.set(newUser).await()
        } else {
            // Update existing user
            val updates = hashMapOf<String, Any>(
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
            user.displayName?.let { updates["displayName"] = it }
            user.photoUrl?.let { updates["photoURL"] = it.toString() }
            userDoc.update(updates).await()
        }
    }

    private fun getWebClientId(): String {
        // This should come from your google-services.json
        // You can also store it in BuildConfig or resources
        return context.getString(
            context.resources.getIdentifier(
                "default_web_client_id",
                "string",
                context.packageName
            )
        )
    }
}
