package com.app.unfit20.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val userName: String,
    val description: String,
    val price: Double,
    val category: String,
    val imageUrl: String,
    val timestamp: Long,
    val isLiked: Boolean,
    val isSaved: Boolean
)