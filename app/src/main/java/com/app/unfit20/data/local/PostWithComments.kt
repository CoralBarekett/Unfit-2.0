package com.app.unfit20.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class PostWithComments(
    @Embedded val post: PostEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "postId"
    )
    val comments: List<CommentEntity>
)