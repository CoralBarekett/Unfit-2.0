package com.app.unfit20.model

data class Post(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val category: String = "",
    val imageUrl: String = "",
    val timestamp: Long = 0,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false
)