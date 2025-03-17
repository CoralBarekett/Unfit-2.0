package com.app.unfit20.repository

import android.content.Context
import android.net.Uri
import com.app.unfit20.UnfitApplication
import com.app.unfit20.data.local.AppDatabase
import com.app.unfit20.data.local.PostEntity
import com.app.unfit20.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class PostRepository(private val context: Context = UnfitApplication.getAppContext()) {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Local database access
    private val postDao = AppDatabase.getInstance(context).postDao()

    // Collection reference
    private val postsCollection = firestore.collection("posts")

    suspend fun uploadImage(imageUri: Uri): String = withContext(Dispatchers.IO) {
        val storageRef = storage.reference
        val fileRef = storageRef.child("post_images/${UUID.randomUUID()}")

        val uploadTask = fileRef.putFile(imageUri).await()
        return@withContext fileRef.downloadUrl.await().toString()
    }

    suspend fun uploadPost(post: Post) = withContext(Dispatchers.IO) {
        // Generate ID if it's empty
        val postId = if (post.id.isEmpty()) postsCollection.document().id else post.id
        val postWithId = post.copy(id = postId)

        postsCollection.document(postId).set(postWithId).await()
    }

    suspend fun getPosts(): List<Post> = withContext(Dispatchers.IO) {
        val snapshot = postsCollection
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()
        return@withContext snapshot.documents.mapNotNull { doc ->
            doc.toObject(Post::class.java)?.copy(id = doc.id)
        }
    }

    suspend fun getPostsByUser(userId: String): List<Post> = withContext(Dispatchers.IO) {
        val snapshot = postsCollection
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()
        return@withContext snapshot.documents.mapNotNull { doc ->
            doc.toObject(Post::class.java)?.copy(id = doc.id)
        }
    }

    suspend fun updatePostLike(postId: String, isLiked: Boolean) = withContext(Dispatchers.IO) {
        postsCollection.document(postId).update("isLiked", isLiked).await()
    }

    suspend fun updatePostSave(postId: String, isSaved: Boolean) = withContext(Dispatchers.IO) {
        postsCollection.document(postId).update("isSaved", isSaved).await()
    }

    suspend fun deletePost(postId: String) = withContext(Dispatchers.IO) {
        val post = postsCollection.document(postId).get().await().toObject(Post::class.java)

        // Delete image from storage if exists
        post?.imageUrl?.let { url ->
            if (url.isNotEmpty()) {
                // Extract the path from the URL
                val path = url.substringAfter("post_images/")
                storage.reference.child("post_images/$path").delete().await()
            }
        }

        // Delete from Firestore
        postsCollection.document(postId).delete().await()

        // Delete from local cache
        postDao.deletePost(postId)
    }

    // Local caching methods
    suspend fun cachePostLocally(post: Post) = withContext(Dispatchers.IO) {
        val postEntity = PostEntity(
            id = post.id,
            userId = post.userId,
            userName = post.userName,
            description = post.description,
            price = post.price,
            category = post.category,
            imageUrl = post.imageUrl,
            timestamp = post.timestamp,
            isLiked = post.isLiked,
            isSaved = post.isSaved
        )
        postDao.insertPost(postEntity)
    }

    suspend fun cachePosts(posts: List<Post>) = withContext(Dispatchers.IO) {
        val postEntities = posts.map { post ->
            PostEntity(
                id = post.id,
                userId = post.userId,
                userName = post.userName,
                description = post.description,
                price = post.price,
                category = post.category,
                imageUrl = post.imageUrl,
                timestamp = post.timestamp,
                isLiked = post.isLiked,
                isSaved = post.isSaved
            )
        }
        postDao.insertPosts(postEntities)
    }

    suspend fun getCachedPosts(): List<Post> = withContext(Dispatchers.IO) {
        return@withContext postDao.getAllPosts().map { entity ->
            Post(
                id = entity.id,
                userId = entity.userId,
                userName = entity.userName,
                description = entity.description,
                price = entity.price,
                category = entity.category,
                imageUrl = entity.imageUrl,
                timestamp = entity.timestamp,
                isLiked = entity.isLiked,
                isSaved = entity.isSaved
            )
        }
    }

    suspend fun updateCachedPost(post: Post) = withContext(Dispatchers.IO) {
        val postEntity = PostEntity(
            id = post.id,
            userId = post.userId,
            userName = post.userName,
            description = post.description,
            price = post.price,
            category = post.category,
            imageUrl = post.imageUrl,
            timestamp = post.timestamp,
            isLiked = post.isLiked,
            isSaved = post.isSaved
        )
        postDao.updatePost(postEntity)
    }
}