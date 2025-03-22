package com.app.unfit20.repository

import android.net.Uri
import android.util.Log
import com.app.unfit20.data.local.PostDao
import com.app.unfit20.data.local.PostEntity
import com.app.unfit20.model.Comment
import com.app.unfit20.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.UUID

class PostRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance().reference
    private val userRepository = UserRepository()

    private val postDao: PostDao? = null

    private val postsCollection = firestore.collection("posts")
    private val commentsCollection = firestore.collection("comments")
    private val likesCollection = firestore.collection("likes")

    suspend fun getUserLikedPosts(userId: String): List<Post> = withContext(Dispatchers.IO) {
        try {
            val likeDocs = likesCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val postIds = likeDocs.mapNotNull { doc ->
                doc.getString("postId")
            }
            if (postIds.isEmpty()) return@withContext emptyList<Post>()

            val posts = mutableListOf<Post>()
            for (postId in postIds) {
                val postDoc = postsCollection.document(postId).get().await()
                if (!postDoc.exists()) continue

                val postData = postDoc.data ?: continue
                val post = mapDocToPost(postDoc.id, postData)

                val commentDocs = commentsCollection
                    .whereEqualTo("postId", postId)
                    .orderBy("createdAt", Query.Direction.ASCENDING)
                    .get()
                    .await()
                val comments = commentDocs.mapNotNull { cDoc ->
                    mapDocToComment(cDoc.id, cDoc.data)
                }

                posts.add(post.copy(isLikedByCurrentUser = true, comments = comments))
            }

            Log.d("PostRepository", "Loaded ${posts.size} liked posts for user: $userId")
            posts.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            Log.e("PostRepository", "Error loading liked posts: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getAllPosts(): List<Post> = withContext(Dispatchers.IO) {
        try {
            val currentUserId = auth.currentUser?.uid
            val postDocs = postsCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val posts = mutableListOf<Post>()
            for (doc in postDocs) {
                val postData = doc.data ?: continue
                val postId = doc.id
                val post = mapDocToPost(postId, postData)

                val isLiked = if (currentUserId != null) {
                    val likeDoc = likesCollection
                        .whereEqualTo("postId", postId)
                        .whereEqualTo("userId", currentUserId)
                        .limit(1)
                        .get()
                        .await()
                    !likeDoc.isEmpty
                } else false

                val commentDocs = commentsCollection
                    .whereEqualTo("postId", postId)
                    .orderBy("createdAt", Query.Direction.ASCENDING)
                    .get()
                    .await()
                val comments = commentDocs.mapNotNull { cDoc ->
                    mapDocToComment(cDoc.id, cDoc.data)
                }

                posts.add(post.copy(isLikedByCurrentUser = isLiked, comments = comments))
            }

            Log.d("PostRepository", "Loaded ${posts.size} total posts")
            cachePosts(posts)
            posts

        } catch (e: Exception) {
            Log.e("PostRepository", "Error loading all posts: ${e.message}", e)
            val cached = loadPostsFromCache()
            if (cached.isNotEmpty()) cached else throw e
        }
    }

    suspend fun getUserPosts(userId: String): List<Post> = withContext(Dispatchers.IO) {
        try {
            val currentUserId = auth.currentUser?.uid
            val postDocs = postsCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val posts = mutableListOf<Post>()
            for (doc in postDocs) {
                val postData = doc.data ?: continue
                val postId = doc.id
                val post = mapDocToPost(postId, postData)

                val isLiked = if (currentUserId != null) {
                    val likeDoc = likesCollection
                        .whereEqualTo("postId", postId)
                        .whereEqualTo("userId", currentUserId)
                        .limit(1)
                        .get()
                        .await()
                    !likeDoc.isEmpty
                } else false

                val commentDocs = commentsCollection
                    .whereEqualTo("postId", postId)
                    .orderBy("createdAt", Query.Direction.ASCENDING)
                    .get()
                    .await()
                val comments = commentDocs.mapNotNull { cDoc ->
                    mapDocToComment(cDoc.id, cDoc.data)
                }

                posts.add(post.copy(isLikedByCurrentUser = isLiked, comments = comments))
            }
            Log.d("PostRepository", "Loaded ${posts.size} posts for user: $userId")
            posts
        } catch (e: Exception) {
            Log.e("PostRepository", "Error loading user posts: ${e.message}", e)
            emptyList()
        }
    }

    // Get a single post by ID
    suspend fun getPost(postId: String): Post? = withContext(Dispatchers.IO) {
        try {
            val currentUserId = auth.currentUser?.uid
            val postDoc = postsCollection.document(postId).get().await()
            if (!postDoc.exists()) return@withContext null

            val postData = postDoc.data ?: return@withContext null
            val post = mapDocToPost(postDoc.id, postData)

            // Check if current user liked it
            val isLiked = if (currentUserId != null) {
                val likeDoc = likesCollection
                    .whereEqualTo("postId", postId)
                    .whereEqualTo("userId", currentUserId)
                    .limit(1)
                    .get()
                    .await()
                !likeDoc.isEmpty
            } else false

            // Get comments
            val commentDocs = commentsCollection
                .whereEqualTo("postId", postId)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .await()
            val comments = commentDocs.mapNotNull { cDoc ->
                mapDocToComment(cDoc.id, cDoc.data)
            }

            post.copy(isLikedByCurrentUser = isLiked, comments = comments)

        } catch (e: Exception) {
            // Fallback to local cache
            loadPostFromCache(postId)
        }
    }

    suspend fun getPagedPosts(page: Int, pageSize: Int): List<Post> = withContext(Dispatchers.IO) {
        val offset = page * pageSize
        postDao?.let { dao ->
            val entities = dao.getPostsPaged(limit = pageSize, offset = offset)
            return@withContext entities.map { e ->
                Post(
                    id = e.id,
                    userId = e.userId,
                    userName = e.userName,
                    userAvatar = e.userAvatar.ifEmpty { null },
                    content = e.content,
                    imageUrl = e.imageUrl.ifEmpty { null },
                    location = e.location.ifEmpty { null },
                    createdAt = Date(e.createdAt),
                    updatedAt = e.updatedAt?.let { Date(it) },
                    likesCount = e.likesCount,
                    commentsCount = e.commentsCount,
                    isLikedByCurrentUser = e.isLiked,
                    comments = emptyList()
                )
            }
        } ?: emptyList()
    }

    // Create a new post
    suspend fun createPost(content: String, imageUri: Uri?, location: String?): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUserId = auth.currentUser?.uid ?: return@withContext false
            val user = userRepository.getUserById(currentUserId) ?: return@withContext false

            val imageUrl = imageUri?.let { uploadImage(it) }

            val postData = hashMapOf(
                "userId" to currentUserId,
                "userName" to user.name,
                "userAvatar" to user.profileImageUrl,
                "content" to content,
                "imageUrl" to imageUrl,
                "location" to location,
                "createdAt" to Date(),
                "likesCount" to 0,
                "commentsCount" to 0
            )
            postsCollection.add(postData).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Update an existing post
    suspend fun updatePost(postId: String, content: String, imageUri: Uri?, location: String?): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val currentUserId = auth.currentUser?.uid ?: return@withContext false
                val postDoc = postsCollection.document(postId).get().await()

                // Check if post belongs to current user
                if (!postDoc.exists() || postDoc.getString("userId") != currentUserId) {
                    return@withContext false
                }

                // Upload new image if provided
                val imageUrl = if (imageUri != null) {
                    uploadImage(imageUri)
                } else {
                    postDoc.getString("imageUrl")
                }

                val updateData = hashMapOf<String, Any>(
                    "content" to content,
                    "updatedAt" to Date()
                )
                imageUrl?.let { updateData["imageUrl"] = it }
                location?.let { updateData["location"] = it }

                postsCollection.document(postId).update(updateData).await()
                true
            } catch (e: Exception) {
                false
            }
        }

    // Delete a post
    suspend fun deletePost(postId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUserId = auth.currentUser?.uid ?: return@withContext false
            val postDoc = postsCollection.document(postId).get().await()

            // Check if the post belongs to current user
            if (!postDoc.exists() || postDoc.getString("userId") != currentUserId) {
                return@withContext false
            }

            // Delete the post
            postsCollection.document(postId).delete().await()

            // Delete associated comments
            val commentDocs = commentsCollection.whereEqualTo("postId", postId).get().await()
            for (doc in commentDocs) {
                commentsCollection.document(doc.id).delete().await()
            }

            // Delete associated likes
            val likeDocs = likesCollection.whereEqualTo("postId", postId).get().await()
            for (doc in likeDocs) {
                likesCollection.document(doc.id).delete().await()
            }

            // Delete from local cache
            deletePostFromCache(postId)
            true
        } catch (e: Exception) {
            false
        }
    }

    // Like a post
    suspend fun likePost(postId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUserId = auth.currentUser?.uid ?: return@withContext false
            val likeDoc = likesCollection
                .whereEqualTo("postId", postId)
                .whereEqualTo("userId", currentUserId)
                .limit(1)
                .get()
                .await()

            if (!likeDoc.isEmpty) {
                // Already liked
                return@withContext true
            }

            // Create like
            val likeData = hashMapOf(
                "postId" to postId,
                "userId" to currentUserId,
                "createdAt" to Date()
            )
            likesCollection.add(likeData).await()

            // Increment likes count
            val postRef = postsCollection.document(postId)
            firestore.runTransaction { transaction ->
                val postSnapshot = transaction.get(postRef)
                val likesCount = postSnapshot.getLong("likesCount") ?: 0
                transaction.update(postRef, "likesCount", likesCount + 1)
            }.await()

            true
        } catch (e: Exception) {
            false
        }
    }

    // Unlike a post
    suspend fun unlikePost(postId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUserId = auth.currentUser?.uid ?: return@withContext false
            val likeDoc = likesCollection
                .whereEqualTo("postId", postId)
                .whereEqualTo("userId", currentUserId)
                .limit(1)
                .get()
                .await()

            if (likeDoc.isEmpty) {
                // Not liked
                return@withContext true
            }

            // Delete the like
            val likeId = likeDoc.documents[0].id
            likesCollection.document(likeId).delete().await()

            // Decrement likes count
            val postRef = postsCollection.document(postId)
            firestore.runTransaction { transaction ->
                val postSnapshot = transaction.get(postRef)
                val likesCount = postSnapshot.getLong("likesCount") ?: 0
                transaction.update(postRef, "likesCount", maxOf(0, likesCount - 1))
            }.await()

            true
        } catch (e: Exception) {
            false
        }
    }

    // Add a comment
    suspend fun addComment(postId: String, comment: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUserId = auth.currentUser?.uid
            if (currentUserId == null) {
                Log.e("PostRepository", "User not authenticated")
                return@withContext false
            }

            val user = userRepository.getUserById(currentUserId)
            if (user == null) {
                Log.e("PostRepository", "User data not found for id: $currentUserId")
                return@withContext false
            }

            val commentData = hashMapOf(
                "postId" to postId,
                "userId" to currentUserId,
                "userName" to user.name,
                "userAvatar" to user.profileImageUrl,
                "content" to comment,
                "createdAt" to Date()
            )

            commentsCollection.add(commentData).await()
            Log.d("PostRepository", "Comment added to Firestore for postId: $postId")

            // Increment comment count
            val postRef = postsCollection.document(postId)
            firestore.runTransaction { transaction ->
                val postSnapshot = transaction.get(postRef)
                val commentsCount = postSnapshot.getLong("commentsCount") ?: 0
                transaction.update(postRef, "commentsCount", commentsCount + 1)
            }.await()

            Log.d("PostRepository", "Comment count incremented for postId: $postId")

            true
        } catch (e: Exception) {
            Log.e("PostRepository", "Error adding comment: ${e.message}", e)
            false
        }
    }

    // Upload image to Firebase Storage
    private suspend fun uploadImage(imageUri: Uri): String {
        return withContext(Dispatchers.IO) {
            val storageRef = storage.child("post_images/${UUID.randomUUID()}")
            storageRef.putFile(imageUri).await()
            storageRef.downloadUrl.await().toString()
        }
    }

    // Map Firestore document to Post
    private fun mapDocToPost(id: String, data: Map<String, Any>): Post {
        val userId = data["userId"] as? String ?: ""
        val userName = data["userName"] as? String ?: ""
        val userAvatar = data["userAvatar"] as? String
        val content = data["content"] as? String ?: ""
        val imageUrl = data["imageUrl"] as? String
        val location = data["location"] as? String
        val createdAt = data["createdAt"] as? Date ?: Date()
        val updatedAt = data["updatedAt"] as? Date
        val likesCount = (data["likesCount"] as? Long)?.toInt() ?: 0
        val commentsCount = (data["commentsCount"] as? Long)?.toInt() ?: 0

        return Post(
            id = id,
            userId = userId,
            userName = userName,
            userAvatar = userAvatar,
            content = content,
            imageUrl = imageUrl,
            location = location,
            createdAt = createdAt,
            updatedAt = updatedAt,
            likesCount = likesCount,
            commentsCount = commentsCount,
            isLikedByCurrentUser = false // set later
        )
    }

    // Map Firestore document to Comment
    private fun mapDocToComment(id: String, data: Map<String, Any>): Comment? {
        val postId = data["postId"] as? String ?: return null
        val userId = data["userId"] as? String ?: return null
        val userName = data["userName"] as? String ?: return null
        val userAvatar = data["userAvatar"] as? String
        val content = data["content"] as? String ?: return null
        val createdAt = data["createdAt"] as? Date ?: Date()

        return Comment(
            id = id,
            postId = postId,
            userId = userId,
            userName = userName,
            userAvatar = userAvatar,
            content = content,
            createdAt = createdAt
        )
    }

    // --- Local caching with Room ---
    private suspend fun cachePosts(posts: List<Post>) {
        postDao?.let { dao ->
            val entities = posts.map { post ->
                PostEntity(
                    id = post.id,
                    userId = post.userId,
                    userName = post.userName,
                    userAvatar = post.userAvatar ?: "",
                    content = post.content,
                    imageUrl = post.imageUrl ?: "",
                    location = post.location ?: "",
                    createdAt = post.createdAt.time,
                    updatedAt = post.updatedAt?.time,
                    likesCount = post.likesCount,
                    commentsCount = post.commentsCount,
                    isLiked = post.isLikedByCurrentUser
                )
            }
            dao.insertAll(entities)
        }
    }

    private suspend fun loadPostsFromCache(): List<Post> {
        postDao?.let { dao ->
            val entities = dao.getAllPosts()
            return entities.map { e ->
                Post(
                    id = e.id,
                    userId = e.userId,
                    userName = e.userName,
                    userAvatar = e.userAvatar.ifEmpty { null },
                    content = e.content,
                    imageUrl = e.imageUrl.ifEmpty { null },
                    location = e.location.ifEmpty { null },
                    createdAt = Date(e.createdAt),
                    updatedAt = e.updatedAt?.let { Date(it) },
                    likesCount = e.likesCount,
                    commentsCount = e.commentsCount,
                    isLikedByCurrentUser = e.isLiked,
                    comments = emptyList() // not cached
                )
            }
        }
        return emptyList()
    }

    private suspend fun loadPostFromCache(postId: String): Post? {
        postDao?.let { dao ->
            val e = dao.getPostById(postId) ?: return null
            return Post(
                id = e.id,
                userId = e.userId,
                userName = e.userName,
                userAvatar = e.userAvatar.ifEmpty { null },
                content = e.content,
                imageUrl = e.imageUrl.ifEmpty { null },
                location = e.location.ifEmpty { null },
                createdAt = Date(e.createdAt),
                updatedAt = e.updatedAt?.let { Date(it) },
                likesCount = e.likesCount,
                commentsCount = e.commentsCount,
                isLikedByCurrentUser = e.isLiked,
                comments = emptyList() // not cached
            )
        }
        return null
    }

    private suspend fun deletePostFromCache(postId: String) {
        postDao?.deletePostById(postId)
    }
}
