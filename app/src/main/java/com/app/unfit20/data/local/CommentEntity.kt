package com.app.unfit20.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "comments",
    foreignKeys = [
        ForeignKey(
            entity = PostEntity::class,
            parentColumns = ["id"],
            childColumns = ["postId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("postId")]
)
data class CommentEntity(
    @PrimaryKey
    val id: String,
    val postId: String,
    val userId: String,
    val userName: String,
    val userAvatar: String,
    val content: String,
    val createdAt: Long
)