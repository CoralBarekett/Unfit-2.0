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

    // Current user
    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    // Result of updating the user profile
    private val _updateProfileResult = MutableLiveData<Boolean>()
    val updateProfileResult: LiveData<Boolean> = _updateProfileResult

    // Check if user is already logged in
    val isUserLoggedIn: Boolean
        get() = auth.currentUser != null

    // Authentication state using Kotlin 1.9 data object syntax
    sealed class AuthState {
        data object Idle : AuthState()
        data object Loading : AuthState()
        data object Success : AuthState()
        data class Error(val message: String) : AuthState()
    }

    init {
        _loginState.value = AuthState.Idle
        _signUpState.value = AuthState.Idle

        // If already logged in, load the current user from Firestore
        if (isUserLoggedIn) {
            viewModelScope.launch {
                _currentUser.value = userRepository.getCurrentUserSync()
            }
        }
    }

    // Load the current user (used by EditProfileFragment)
    fun loadCurrentUser() {
        viewModelScope.launch {
            _currentUser.value = userRepository.getCurrentUserSync()
        }
    }

    // Login user
    fun login(email: String, password: String) {
        _loginState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val success = userRepository.login(email, password)
                if (success) {
                    _loginState.value = AuthState.Success
                    // Refresh current user
                    _currentUser.value = userRepository.getCurrentUserSync()
                } else {
                    _loginState.value = AuthState.Error("Login failed")
                }
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
                // Create user with email/password
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val userId = authResult.user?.uid ?: throw Exception("Failed to create user")

                // Upload profile image if provided
                val profileImageUrl = profileImageUri?.let { uploadProfileImage(it, userId) }

                // Create user object
                val user = User(
                    id = userId,
                    name = name,
                    email = email,
                    profileImageUrl = profileImageUrl
                )

                // Save user to database
                val saved = userRepository.saveUser(user)
                if (saved) {
                    _signUpState.value = AuthState.Success
                    _currentUser.value = user
                } else {
                    _signUpState.value = AuthState.Error("Failed to save user data")
                }
            } catch (e: Exception) {
                _signUpState.value = AuthState.Error(e.message ?: "Sign up failed")
            }
        }
    }

    // Update user profile (name/photo)
    fun updateProfile(name: String, photoUri: Uri?) {
        viewModelScope.launch {
            try {
                // Call your repository to update the user's name & photo
                val success = userRepository.updateProfile(name, photoUri)
                if (success) {
                    // Reload the current user after a successful update
                    _currentUser.value = userRepository.getCurrentUserSync()
                    _updateProfileResult.value = true
                } else {
                    _updateProfileResult.value = false
                }
            } catch (e: Exception) {
                _updateProfileResult.value = false
            }
        }
    }

    // Upload profile image to Firebase Storage
    private suspend fun uploadProfileImage(imageUri: Uri, userId: String): String {
        return try {
            val fileRef = storage.child("profile_images/$userId/${UUID.randomUUID()}")
            fileRef.putFile(imageUri).await()
            fileRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            throw Exception("Failed to upload profile image: ${e.message}")
        }
    }

    // Logout user
    fun logout() {
        auth.signOut()
        _currentUser.value = null
    }
}