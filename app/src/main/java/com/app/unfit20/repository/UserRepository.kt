package com.app.unfit20.repository

import android.net.Uri
import com.app.unfit20.model.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.max

class UserRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance().reference
    private val usersCollection = firestore.collection("users")

    // Login with email and password
    suspend fun login(email: String, password: String): Boolean = withContext(Dispatchers.IO) {
        try {
            auth.signInWithEmailAndPassword(email, password).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Check if user exists in Firestore
    suspend fun userExists(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val doc = usersCollection.document(userId).get().await()
            doc.exists()
        } catch (e: Exception) {
            false
        }
    }

    // Save (or update) user information in Firestore
    suspend fun saveUser(user: User): Boolean = withContext(Dispatchers.IO) {
        try {
            if (user.id.isEmpty()) return@withContext false
            usersCollection.document(user.id).set(user).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Logout user
    fun logout() {
        auth.signOut()
    }

    // Get current user ID
    fun getCurrentUserId(): String? = auth.currentUser?.uid

    // Get user by ID
    suspend fun getUserById(userId: String): User? = withContext(Dispatchers.IO) {
        try {
            val userDoc = usersCollection.document(userId).get().await()
            if (!userDoc.exists()) return@withContext null
            val data = userDoc.data ?: return@withContext null
            User(
                id = userId,
                name = data["name"] as? String ?: "",
                email = data["email"] as? String ?: "",
                profileImageUrl = data["profileImageUrl"] as? String,
                bio = data["bio"] as? String,
                followersCount = (data["followersCount"] as? Long)?.toInt() ?: 0,
                followingCount = (data["followingCount"] as? Long)?.toInt() ?: 0
            )
        } catch (e: Exception) {
            null
        }
    }

    // Helper for AuthViewModel to get current user synchronously (if needed)
    suspend fun getCurrentUserSync(): User? = getCurrentUserId()?.let { getUserById(it) }

    // Update user profile
    suspend fun updateProfile(userName: String, photoUri: Uri?): Boolean = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: return@withContext false
            val photoUrl = if (photoUri != null) uploadProfileImage(photoUri) else auth.currentUser?.photoUrl?.toString()
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(userName)
                .apply { photoUrl?.let { setPhotoUri(Uri.parse(it)) } }
                .build()
            auth.currentUser?.updateProfile(profileUpdates)?.await()
            val updates = hashMapOf<String, Any>("name" to userName)
            photoUrl?.let { updates["profileImageUrl"] = it }
            usersCollection.document(userId).update(updates).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Update user bio
    suspend fun updateBio(bio: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: return@withContext false
            usersCollection.document(userId).update("bio", bio).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Upload profile image
    private suspend fun uploadProfileImage(imageUri: Uri): String = withContext(Dispatchers.IO) {
        val userId = getCurrentUserId() ?: throw Exception("User not authenticated")
        val storageRef = storage.child("profile_images/$userId/${UUID.randomUUID()}")
        storageRef.putFile(imageUri).await()
        storageRef.downloadUrl.await().toString()
    }

    // Follow a user
    suspend fun followUser(targetUserId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId() ?: return@withContext false
            if (currentUserId == targetUserId) return@withContext false
            val followingRef = usersCollection.document(currentUserId)
                .collection("following").document(targetUserId)
            followingRef.set(hashMapOf("timestamp" to Timestamp.now())).await()
            val followerRef = usersCollection.document(targetUserId)
                .collection("followers").document(currentUserId)
            followerRef.set(hashMapOf("timestamp" to Timestamp.now())).await()
            val targetUserRef = usersCollection.document(targetUserId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(targetUserRef)
                val count = snapshot.getLong("followersCount") ?: 0
                transaction.update(targetUserRef, "followersCount", count + 1)
            }.await()
            val currentUserRef = usersCollection.document(currentUserId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(currentUserRef)
                val count = snapshot.getLong("followingCount") ?: 0
                transaction.update(currentUserRef, "followingCount", count + 1)
            }.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Unfollow a user
    suspend fun unfollowUser(targetUserId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId() ?: return@withContext false
            if (currentUserId == targetUserId) return@withContext false
            val followingRef = usersCollection.document(currentUserId)
                .collection("following").document(targetUserId)
            followingRef.delete().await()
            val followerRef = usersCollection.document(targetUserId)
                .collection("followers").document(currentUserId)
            followerRef.delete().await()
            val targetUserRef = usersCollection.document(targetUserId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(targetUserRef)
                val count = snapshot.getLong("followersCount") ?: 0
                transaction.update(targetUserRef, "followersCount", max(0, count - 1))
            }.await()
            val currentUserRef = usersCollection.document(currentUserId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(currentUserRef)
                val count = snapshot.getLong("followingCount") ?: 0
                transaction.update(currentUserRef, "followingCount", max(0, count - 1))
            }.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Check if following a user
    suspend fun isFollowing(targetUserId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId() ?: return@withContext false
            if (currentUserId == targetUserId) return@withContext false
            val followingDoc = usersCollection.document(currentUserId)
                .collection("following").document(targetUserId)
                .get().await()
            followingDoc.exists()
        } catch (e: Exception) {
            false
        }
    }
}