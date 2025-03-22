package com.app.unfit20.model

import java.util.Date

data class Post(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String? = null,
    val content: String = "",
    val imageUrl: String? = null,
    val location: String? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date? = null,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLikedByCurrentUser: Boolean = false,
    val comments: List<Comment> = emptyList()
)