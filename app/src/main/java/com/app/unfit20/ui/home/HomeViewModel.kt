package com.app.unfit20.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.unfit20.model.Post
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

class HomeViewModel : ViewModel() {

    private val _postsState = MutableLiveData<PostsState>()
    val postsState: LiveData<PostsState> = _postsState

    fun loadPosts() {
        _postsState.value = PostsState.Loading

        viewModelScope.launch {
            try {
                // Simulate network delay
                delay(1000)

                // TODO: Replace with actual API call
                val posts = getDummyPosts()
                _postsState.value = PostsState.Success(posts)
            } catch (e: Exception) {
                _postsState.value = PostsState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    // Dummy data for testing
    private fun getDummyPosts(): List<Post> {
        return listOf(
            Post(
                id = "1",
                userId = "user1",
                userName = "JohnDoe",
                userAvatar = null,
                content = "Just completed a 5K run. Feeling great!",
                imageUrl = null,
                location = "New York",
                createdAt = Date(),
                updatedAt = null,
                likesCount = 15,
                commentsCount = 3,
                isLikedByCurrentUser = false
            ),
            Post(
                id = "2",
                userId = "user2",
                userName = "JaneSmith",
                userAvatar = null,
                content = "Finally hit my goal of 100kg squat!",
                imageUrl = null,
                location = "Los Angeles",
                createdAt = Date(),
                updatedAt = null,
                likesCount = 32,
                commentsCount = 7,
                isLikedByCurrentUser = true
            )
        )
    }

    sealed class PostsState {
        object Loading : PostsState()
        data class Success(val posts: List<Post>) : PostsState()
        data class Error(val message: String) : PostsState()
    }
}