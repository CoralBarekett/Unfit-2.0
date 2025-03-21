package com.app.unfit20.model

data class ProductInfo(
    val id: String,
    val title: String,
    val price: Double?,
    val description: String?,
    val category: String?,
    val imageUrl: String?,
    val rating: Double?
)