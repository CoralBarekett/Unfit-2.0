package com.app.unfit20.ui.post

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.unfit20.model.Post
import com.app.unfit20.repository.PostRepository
import com.app.unfit20.repository.UserRepository
import kotlinx.coroutines.launch

class PostViewModel : ViewModel() {

    private val postRepository = PostRepository()
    private val userRepository = UserRepository()

    // LiveData for tracking upload status
    private val _postUploadStatus = MutableLiveData<Boolean>()
    val postUploadStatus: LiveData<Boolean> = _postUploadStatus

    // LiveData for tracking upload result
    private val _postUploadResult = MutableLiveData<Boolean>()
    val postUploadResult: LiveData<Boolean> = _postUploadResult

    // LiveData for posts feed
    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts

    // LiveData for user posts
    private val _userPosts = MutableLiveData<List<Post>>()
    val userPosts: LiveData<List<Post>> = _userPosts

    init {
        loadPosts()
    }

    fun uploadPost(post: Post, imageUri: Uri) {
        viewModelScope.launch {
            try {
                _postUploadStatus.value = true

                // First upload the image and get URL
                val imageUrl = postRepository.uploadImage(imageUri)

                // Update post with image URL
                val updatedPost = post.copy(imageUrl = imageUrl)

                // Upload post data
                postRepository.uploadPost(updatedPost)

                // Add to local cache
                postRepository.cachePostLocally(updatedPost)

                _postUploadResult.value = true

                // Refresh posts
                loadPosts()
            } catch (e: Exception) {
                _postUploadResult.value = false
            } finally {
                _postUploadStatus.value = false
            }
        }
    }

    fun loadPosts() {
        viewModelScope.launch {
            try {
                // First try to load from cache
                val cachedPosts = postRepository.getCachedPosts()
                if (cachedPosts.isNotEmpty()) {
                    _posts.value = cachedPosts
                }

                // Then fetch from network to update
                val remotePosts = postRepository.getPosts()
                _posts.value = remotePosts

                // Update cache
                postRepository.cachePosts(remotePosts)
            } catch (e: Exception) {
                // If network fails, rely on cache
                val cachedPosts = postRepository.getCachedPosts()
                if (_posts.value == null || _posts.value?.isEmpty() == true) {
                    _posts.value = cachedPosts
                }
            }
        }
    }

    fun loadUserPosts() {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserId() ?: return@launch

            try {
                val posts = postRepository.getPostsByUser(userId)
                _userPosts.value = posts
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun getCurrentUserId(): String {
        return userRepository.getCurrentUserId() ?: ""
    }

    fun getCurrentUserName(): String {
        return userRepository.getCurrentUserName() ?: "Anonymous User"
    }

    fun likePost(post: Post, isLiked: Boolean) {
        viewModelScope.launch {
            try {
                postRepository.updatePostLike(post.id, isLiked)

                // Update local cache
                val updatedPost = post.copy(isLiked = isLiked)
                postRepository.updateCachedPost(updatedPost)

                // Refresh posts list
                loadPosts()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun savePost(post: Post, isSaved: Boolean) {
        viewModelScope.launch {
            try {
                postRepository.updatePostSave(post.id, isSaved)

                // Update local cache
                val updatedPost = post.copy(isSaved = isSaved)
                postRepository.updateCachedPost(updatedPost)

                // Refresh posts list
                loadPosts()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                postRepository.deletePost(postId)

                // Refresh posts
                loadPosts()
                loadUserPosts()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}