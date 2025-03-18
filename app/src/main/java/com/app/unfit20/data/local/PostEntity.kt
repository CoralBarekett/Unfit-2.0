package com.app.unfit20.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val userName: String,
    val userAvatar: String,
    val content: String,
    val imageUrl: String,
    val location: String,
    val createdAt: Long,
    val updatedAt: Long? = null,
    val likesCount: Int,
    val commentsCount: Int,
    val isLiked: Boolean
)