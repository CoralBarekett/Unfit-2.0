package com.app.unfit20.ui.profile

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
                // If userId is null, load current user profile
                val targetUserId = userId ?: userRepository.getCurrentUserId() ?: ""
                if (targetUserId.isEmpty()) {
                    _errorMessage.value = "User not found"
                    _isLoading.value = false
                    return@launch
                }

                val loadedUser = userRepository.getUserById(targetUserId)
                _user.value = loadedUser
            } catch (e: Exception) {
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
                val targetUserId = userId ?: userRepository.getCurrentUserId() ?: ""
                if (targetUserId.isEmpty()) {
                    _userPosts.value = emptyList()
                    return@launch
                }
                val posts = postRepository.getUserPosts(targetUserId)
                _userPosts.value = posts
            } catch (e: Exception) {
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
                val targetUserId = userId ?: userRepository.getCurrentUserId() ?: ""
                if (targetUserId.isEmpty()) {
                    _userLikedPosts.value = emptyList()
                    return@launch
                }
                val posts = postRepository.getUserLikedPosts(targetUserId)
                _userLikedPosts.value = posts
            } catch (e: Exception) {
                _userLikedPosts.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
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