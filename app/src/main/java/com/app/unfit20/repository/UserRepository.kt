package com.app.unfit20.repository

import android.net.Uri
import com.app.unfit20.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class UserRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    suspend fun login(email: String, password: String): Boolean {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun signUp(email: String, password: String, userName: String): Boolean {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()

            // Update display name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(userName)
                .build()

            result.user?.updateProfile(profileUpdates)?.await()

            // Save user to Firestore
            result.user?.uid?.let { uid ->
                val user = hashMapOf(
                    "uid" to uid,
                    "email" to email,
                    "userName" to userName,
                    "profileImageUrl" to ""
                )

                firestore.collection("users").document(uid).set(user).await()
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    fun logout() {
        auth.signOut()
    }

    suspend fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser ?: return null

        try {
            val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()
            return if (userDoc.exists()) {
                User(
                    id = firebaseUser.uid,
                    email = userDoc.getString("email") ?: "",
                    userName = userDoc.getString("userName") ?: firebaseUser.displayName ?: "",
                    profileImageUrl = userDoc.getString("profileImageUrl") ?: firebaseUser.photoUrl?.toString() ?: ""
                )
            } else {
                User(
                    id = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    userName = firebaseUser.displayName ?: "",
                    profileImageUrl = firebaseUser.photoUrl?.toString() ?: ""
                )
            }
        } catch (e: Exception) {
            return User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                userName = firebaseUser.displayName ?: "",
                profileImageUrl = firebaseUser.photoUrl?.toString() ?: ""
            )
        }
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun getCurrentUserName(): String? {
        return auth.currentUser?.displayName
    }

    suspend fun updateProfile(userName: String, photoUri: Uri?): Boolean {
        val user = auth.currentUser ?: return false

        try {
            var downloadUrl: Uri? = null

            // Upload new profile image if provided
            if (photoUri != null) {
                val storageRef = storage.reference.child("profile_images/${UUID.randomUUID()}")
                storageRef.putFile(photoUri).await()
                downloadUrl = storageRef.downloadUrl.await()
            }

            // Update Firebase Auth profile
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(userName)
                .apply {
                    downloadUrl?.let { setPhotoUri(it) }
                }
                .build()

            user.updateProfile(profileUpdates).await()

            // Update Firestore
            val userUpdates = hashMapOf<String, Any>(
                "userName" to userName
            )

            downloadUrl?.let {
                userUpdates["profileImageUrl"] = it.toString()
            }

            firestore.collection("users").document(user.uid).update(userUpdates).await()
            return true
        } catch (e: Exception) {
            return false
        }
    }
}