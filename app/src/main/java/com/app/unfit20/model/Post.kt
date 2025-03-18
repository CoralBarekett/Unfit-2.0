package com.app.unfit20.model

import java.util.Date

data class Post(
    val id: String,
    val userId: String,
    val userName: String,
    val userAvatar: String?,
    val content: String,
    val imageUrl: String?,
    val location: String?,
    val createdAt: Date,
    val updatedAt: Date?,
    val likesCount: Int,
    val commentsCount: Int,
    val isLikedByCurrentUser: Boolean,
    val comments: List<Comment> = emptyList()
)