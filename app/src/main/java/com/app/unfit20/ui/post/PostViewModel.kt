package com.app.unfit20.ui.post

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.liveData
import com.app.unfit20.model.Post
import com.app.unfit20.repository.PostRepository
import kotlinx.coroutines.launch

class PostViewModel(private val repository: PostRepository) : ViewModel() {

    // Current post being viewed
    private val _post = MutableLiveData<Post>()
    val post: LiveData<Post> = _post

    // State for post operations (create, update, delete)
    private val _postState = MutableLiveData<PostState>()
    val postState: LiveData<PostState> = _postState

    // Result of like operation
    private val _likePostResult = MutableLiveData<Boolean>()
    val likePostResult: LiveData<Boolean> = _likePostResult

    // Result of comment operation
    private val _commentResult = MutableLiveData<Boolean>()
    val commentResult: LiveData<Boolean> = _commentResult

    // Result of delete operation
    private val _deletePostResult = MutableLiveData<Boolean>()
    val deletePostResult: LiveData<Boolean> = _deletePostResult

    // Feed posts
    private val _feedPosts = MutableLiveData<List<Post>>()
    val feedPosts: LiveData<List<Post>> = _feedPosts

    // User posts
    private val _userPosts = MutableLiveData<List<Post>>()
    val userPosts: LiveData<List<Post>> = _userPosts

    // Get posts for feed
    fun loadFeedPosts() {
        viewModelScope.launch {
            try {
                val posts = repository.getAllPosts()
                _feedPosts.postValue(posts)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    // Get posts by specific user
    fun loadUserPosts(userId: String) {
        viewModelScope.launch {
            try {
                val posts = repository.getUserPosts(userId)
                _userPosts.postValue(posts)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    // Get a single post
    fun loadPost(postId: String) {
        viewModelScope.launch {
            try {
                val post = repository.getPost(postId)
                post?.let {
                    _post.postValue(it)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    // Create a new post
    fun createPost(content: String, imageUri: Uri?, location: String?) {
        _postState.value = PostState.Loading
        viewModelScope.launch {
            try {
                val success = repository.createPost(content, imageUri, location)
                _postState.postValue(
                    if (success) PostState.Success
                    else PostState.Error("Failed to create post")
                )
            } catch (e: Exception) {
                _postState.postValue(PostState.Error(e.message ?: "Unknown error occurred"))
            }
        }
    }

    // Update an existing post
    fun updatePost(postId: String, content: String, imageUri: Uri?, location: String?) {
        _postState.value = PostState.Loading
        viewModelScope.launch {
            try {
                val success = repository.updatePost(postId, content, imageUri, location)
                _postState.postValue(
                    if (success) PostState.Success
                    else PostState.Error("Failed to update post")
                )
            } catch (e: Exception) {
                _postState.postValue(PostState.Error(e.message ?: "Unknown error occurred"))
            }
        }
    }

    // Delete a post
    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                val success = repository.deletePost(postId)
                _deletePostResult.postValue(success)
            } catch (e: Exception) {
                _deletePostResult.postValue(false)
            }
        }
    }

    // Like a post
    fun likePost(postId: String) {
        viewModelScope.launch {
            try {
                val success = repository.likePost(postId)
                _likePostResult.postValue(success)

                // Update current post if it's the one being liked
                _post.value?.let { currentPost ->
                    if (currentPost.id == postId && success) {
                        _post.postValue(
                            currentPost.copy(
                                isLikedByCurrentUser = true,
                                likesCount = currentPost.likesCount + 1
                            )
                        )
                    }
                }

                // Also update in feed posts
                _feedPosts.value?.let { posts ->
                    val updatedPosts = posts.map { post ->
                        if (post.id == postId && success) {
                            post.copy(
                                isLikedByCurrentUser = true,
                                likesCount = post.likesCount + 1
                            )
                        } else {
                            post
                        }
                    }
                    _feedPosts.postValue(updatedPosts)
                }

                // Also update in user posts
                _userPosts.value?.let { posts ->
                    val updatedPosts = posts.map { post ->
                        if (post.id == postId && success) {
                            post.copy(
                                isLikedByCurrentUser = true,
                                likesCount = post.likesCount + 1
                            )
                        } else {
                            post
                        }
                    }
                    _userPosts.postValue(updatedPosts)
                }
            } catch (e: Exception) {
                _likePostResult.postValue(false)
            }
        }
    }

    // Unlike a post
    fun unlikePost(postId: String) {
        viewModelScope.launch {
            try {
                val success = repository.unlikePost(postId)
                _likePostResult.postValue(success)

                // Update current post if it's the one being unliked
                _post.value?.let { currentPost ->
                    if (currentPost.id == postId && success) {
                        _post.postValue(
                            currentPost.copy(
                                isLikedByCurrentUser = false,
                                likesCount = maxOf(0, currentPost.likesCount - 1)
                            )
                        )
                    }
                }

                // Also update in feed posts
                _feedPosts.value?.let { posts ->
                    val updatedPosts = posts.map { post ->
                        if (post.id == postId && success) {
                            post.copy(
                                isLikedByCurrentUser = false,
                                likesCount = maxOf(0, post.likesCount - 1)
                            )
                        } else {
                            post
                        }
                    }
                    _feedPosts.postValue(updatedPosts)
                }

                // Also update in user posts
                _userPosts.value?.let { posts ->
                    val updatedPosts = posts.map { post ->
                        if (post.id == postId && success) {
                            post.copy(
                                isLikedByCurrentUser = false,
                                likesCount = maxOf(0, post.likesCount - 1)
                            )
                        } else {
                            post
                        }
                    }
                    _userPosts.postValue(updatedPosts)
                }
            } catch (e: Exception) {
                _likePostResult.postValue(false)
            }
        }
    }

    // Add a comment to a post
    fun addComment(postId: String, comment: String) {
        viewModelScope.launch {
            try {
                val success = repository.addComment(postId, comment)
                _commentResult.postValue(success)

                if (success) {
                    // Refresh post data to get the updated comments
                    loadPost(postId)
                }
            } catch (e: Exception) {
                _commentResult.postValue(false)
            }
        }
    }

    // Get a post for direct access in fragment (without setting to _post LiveData)
    fun getPost(postId: String): LiveData<Post?> = liveData {
        try {
            val post = repository.getPost(postId)
            emit(post)
        } catch (e: Exception) {
            emit(null)
        }
    }

    // State class for post operations
    sealed class PostState {
//      data object Initial : PostState()
        data object Loading : PostState()
        data object Success : PostState()
        data class Error(val message: String) : PostState()
    }
}