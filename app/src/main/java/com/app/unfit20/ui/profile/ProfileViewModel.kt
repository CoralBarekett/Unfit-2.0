package com.app.unfit20.ui.profile

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.unfit20.model.Post
import com.app.unfit20.model.User
import com.app.unfit20.repository.PostRepository
import com.app.unfit20.repository.UserRepository
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val userRepository = UserRepository()
    private val postRepository = PostRepository()

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _userPosts = MutableLiveData<List<Post>>()
    val userPosts: LiveData<List<Post>> = _userPosts

    private val _userLikedPosts = MutableLiveData<List<Post>>()
    val userLikedPosts: LiveData<List<Post>> = _userLikedPosts

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Load user profile
    fun loadUserProfile(userId: String? = null) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val targetUserId = userId ?: userRepository.getCurrentUserId()
                if (targetUserId.isNullOrEmpty()) {
                    _errorMessage.value = "User not found"
                    _isLoading.value = false
                    return@launch
                }
                Log.d("ProfileViewModel", "Loading profile for userId: $targetUserId")
                val loadedUser = userRepository.getUserById(targetUserId)
                _user.value = loadedUser
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading user profile", e)
                _errorMessage.value = e.message ?: "Failed to load user profile"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Load user posts
    fun loadUserPosts(userId: String? = null) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val targetUserId = userId ?: userRepository.getCurrentUserId()
                if (targetUserId.isNullOrEmpty()) {
                    _userPosts.value = emptyList()
                    return@launch
                }
                Log.d("ProfileViewModel", "Loading posts for userId: $targetUserId")
                val posts = postRepository.getUserPosts(targetUserId)
                Log.d("ProfileViewModel", "Loaded ${posts.size} posts")
                _userPosts.value = posts
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading user posts", e)
                _userPosts.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Load posts liked by user
    fun loadUserLikedPosts(userId: String? = null) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val targetUserId = userId ?: userRepository.getCurrentUserId()
                if (targetUserId.isNullOrEmpty()) {
                    _userLikedPosts.value = emptyList()
                    return@launch
                }
                Log.d("ProfileViewModel", "Loading liked posts for userId: $targetUserId")
                val posts = postRepository.getUserLikedPosts(targetUserId)
                Log.d("ProfileViewModel", "Loaded ${posts.size} liked posts")
                _userLikedPosts.value = posts
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading liked posts", e)
                _userLikedPosts.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Convenience method to load everything together
    fun loadFullProfile(userId: String?) {
        loadUserProfile(userId)
        loadUserPosts(userId)
        loadUserLikedPosts(userId)
    }

    // Check if viewing own profile
    fun isOwnProfile(userId: String?): Boolean {
        if (userId == null) return true // No userId means current user
        val currentUserId = userRepository.getCurrentUserId()
        return currentUserId == userId
    }

    // Clear error message
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}