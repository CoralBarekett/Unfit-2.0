package com.app.unfit20.ui.post

import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import com.app.unfit20.model.Post
import com.app.unfit20.repository.PostRepository
import kotlinx.coroutines.launch

class PostViewModel(private val repository: PostRepository) : ViewModel() {
    private var currentPage = 0
    private val pageSize = 10
    private var isLoadingMore = false

    fun loadNextPage() {
        if (isLoadingMore) return
        isLoadingMore = true
        viewModelScope.launch {
            try {
                val newPosts = repository.getPagedPosts(currentPage, pageSize)
                val currentList = _feedPosts.value.orEmpty()
                _feedPosts.postValue(currentList + newPosts)
                currentPage++
            } catch (_: Exception) {}
            isLoadingMore = false
        }
    }

    private val _post = MutableLiveData<Post>()
    val post: LiveData<Post> = _post

    private val _postState = MutableLiveData<PostState>()
    val postState: LiveData<PostState> = _postState

    private val _likePostResult = MutableLiveData<Boolean>()
    val likePostResult: LiveData<Boolean> = _likePostResult

    private val _commentResult = MutableLiveData<Boolean>()
    val commentResult: LiveData<Boolean> = _commentResult

    private val _deletePostResult = MutableLiveData<Boolean>()
    val deletePostResult: LiveData<Boolean> = _deletePostResult

    private val _feedPosts = MutableLiveData<List<Post>>()
    val feedPosts: LiveData<List<Post>> = _feedPosts

    private val _userPosts = MutableLiveData<List<Post>>()
    val userPosts: LiveData<List<Post>> = _userPosts

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

    fun loadUserPosts(userId: String) {
        viewModelScope.launch {
            try {
                val posts = repository.getUserPosts(userId)
                Log.d("USER_POSTS", "Received ${posts.size} posts")
                _userPosts.postValue(posts)
            } catch (e: Exception) {
                Log.e("USER_POSTS", "Failed to load user posts", e)
            }
        }
    }

    fun loadPost(postId: String) {
        viewModelScope.launch {
            try {
                val post = repository.getPost(postId)
                post?.let {
                    Log.d("PostViewModel", "Post loaded with ${it.comments.size} comments")
                    _post.postValue(it)
                }
            } catch (e: Exception) {
                Log.e("PostViewModel", "Failed to load post: ${e.message}", e)
            }
        }
    }

    fun setLocalPost(post: Post) {
        _post.postValue(post)
    }

    fun createPost(content: String, imageUri: Uri?, location: String?) {
        _postState.value = PostState.Loading
        viewModelScope.launch {
            try {
                val success = repository.createPost(content, imageUri, location)
                _postState.postValue(if (success) PostState.Success else PostState.Error("Failed to create post"))
            } catch (e: Exception) {
                _postState.postValue(PostState.Error(e.message ?: "Unknown error occurred"))
            }
        }
    }

    fun updatePost(postId: String, content: String, imageUri: Uri?, location: String?) {
        _postState.value = PostState.Loading
        viewModelScope.launch {
            try {
                val success = repository.updatePost(postId, content, imageUri, location)
                _postState.postValue(if (success) PostState.Success else PostState.Error("Failed to update post"))
            } catch (e: Exception) {
                _postState.postValue(PostState.Error(e.message ?: "Unknown error occurred"))
            }
        }
    }

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

    fun likePost(postId: String) {
        viewModelScope.launch {
            try {
                val success = repository.likePost(postId)
                _likePostResult.postValue(success)

                _post.value?.let { currentPost ->
                    if (currentPost.id == postId && success) {
                        _post.postValue(currentPost.copy(isLikedByCurrentUser = true, likesCount = currentPost.likesCount + 1))
                    }
                }

                _feedPosts.value?.let { posts ->
                    val updatedPosts = posts.map { post ->
                        if (post.id == postId && success) {
                            post.copy(isLikedByCurrentUser = true, likesCount = post.likesCount + 1)
                        } else post
                    }
                    _feedPosts.postValue(updatedPosts)
                }

                _userPosts.value?.let { posts ->
                    val updatedPosts = posts.map { post ->
                        if (post.id == postId && success) {
                            post.copy(isLikedByCurrentUser = true, likesCount = post.likesCount + 1)
                        } else post
                    }
                    _userPosts.postValue(updatedPosts)
                }
            } catch (e: Exception) {
                _likePostResult.postValue(false)
            }
        }
    }

    fun unlikePost(postId: String) {
        viewModelScope.launch {
            try {
                val success = repository.unlikePost(postId)
                _likePostResult.postValue(success)

                _post.value?.let { currentPost ->
                    if (currentPost.id == postId && success) {
                        _post.postValue(currentPost.copy(isLikedByCurrentUser = false, likesCount = maxOf(0, currentPost.likesCount - 1)))
                    }
                }

                _feedPosts.value?.let { posts ->
                    val updatedPosts = posts.map { post ->
                        if (post.id == postId && success) {
                            post.copy(isLikedByCurrentUser = false, likesCount = maxOf(0, post.likesCount - 1))
                        } else post
                    }
                    _feedPosts.postValue(updatedPosts)
                }

                _userPosts.value?.let { posts ->
                    val updatedPosts = posts.map { post ->
                        if (post.id == postId && success) {
                            post.copy(isLikedByCurrentUser = false, likesCount = maxOf(0, post.likesCount - 1))
                        } else post
                    }
                    _userPosts.postValue(updatedPosts)
                }
            } catch (e: Exception) {
                _likePostResult.postValue(false)
            }
        }
    }

    fun addComment(postId: String, comment: String) {
        viewModelScope.launch {
            try {
                val success = repository.addComment(postId, comment)
                _commentResult.postValue(success)
                if (success) loadPost(postId)
            } catch (e: Exception) {
                _commentResult.postValue(false)
            }
        }
    }

    fun getPost(postId: String): LiveData<Post?> = liveData {
        try {
            val post = repository.getPost(postId)
            emit(post)
        } catch (e: Exception) {
            emit(null)
        }
    }

    sealed class PostState {
        data object Loading : PostState()
        data object Success : PostState()
        data class Error(val message: String) : PostState()
    }

    class Factory(private val repository: PostRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PostViewModel::class.java)) {
                return PostViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}