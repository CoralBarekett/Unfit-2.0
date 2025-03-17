package com.app.unfit20.ui.auth

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.unfit20.model.User
import com.app.unfit20.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AuthViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance().reference

    // Login state
    private val _loginState = MutableLiveData<AuthState>()
    val loginState: LiveData<AuthState> = _loginState

    // Sign up state
    private val _signUpState = MutableLiveData<AuthState>()
    val signUpState: LiveData<AuthState> = _signUpState

    // Check if user is already logged in
    val isUserLoggedIn: Boolean
        get() = auth.currentUser != null

    // Get current user ID
    val currentUserId: String?
        get() = auth.currentUser?.uid

    // Authentication state
    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        object Success : AuthState()
        data class Error(val message: String) : AuthState()
    }

    init {
        _loginState.value = AuthState.Idle
        _signUpState.value = AuthState.Idle
    }

    // Login user
    fun login(email: String, password: String) {
        _loginState.value = AuthState.Loading

        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                _loginState.value = AuthState.Success
            } catch (e: Exception) {
                _loginState.value = AuthState.Error(e.message ?: "Login failed")
            }
        }
    }

    // Sign up user
    fun signUp(name: String, email: String, password: String, profileImageUri: Uri?) {
        _signUpState.value = AuthState.Loading

        viewModelScope.launch {
            try {
                // Create user with email and password
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val userId = authResult.user?.uid ?: throw Exception("Failed to create user")

                // Upload profile image if provided
                val profileImageUrl = profileImageUri?.let { uploadProfileImage(it, userId) }

                // Create user object
                val user = User(
                    id = userId,
                    userName = name,
                    email = email,
                    profileImageUrl = profileImageUrl
                )

                // Save user to database
                userRepository.createUser(user)

                _signUpState.value = AuthState.Success
            } catch (e: Exception) {
                _signUpState.value = AuthState.Error(e.message ?: "Sign up failed")
            }
        }
    }

    // Upload profile image to Firebase Storage
    private suspend fun uploadProfileImage(imageUri: Uri, userId: String): String {
        return try {
            val fileRef = storage.child("profile_images/$userId/${UUID.randomUUID()}")
            val uploadTask = fileRef.putFile(imageUri).await()
            fileRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            throw Exception("Failed to upload profile image: ${e.message}")
        }
    }

    // Logout user
    fun logout() {
        auth.signOut()
    }
}