package com.app.unfit20.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface PostDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<PostEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: PostEntity)

    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    suspend fun getAllPosts(): List<PostEntity>

    @Query("SELECT * FROM posts ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getPostsPaged(limit: Int, offset: Int): List<PostEntity>

    @Query("SELECT * FROM posts WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getPostsByUserId(userId: String): List<PostEntity>

    @Query("SELECT * FROM posts WHERE id = :postId")
    suspend fun getPostById(postId: String): PostEntity?

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePostById(postId: String)

    @Query("DELETE FROM posts")
    suspend fun deleteAllPosts()

    @Transaction
    @Query("SELECT * FROM posts WHERE id = :postId")
    suspend fun getPostWithComments(postId: String): PostWithComments?
}