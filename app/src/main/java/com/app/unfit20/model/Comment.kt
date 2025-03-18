package com.app.unfit20.model

import java.util.Date

data class Comment(
    val id: String,
    val postId: String,
    val userId: String,
    val userName: String,
    val userAvatar: String?,
    val content: String,
    val createdAt: Date
)