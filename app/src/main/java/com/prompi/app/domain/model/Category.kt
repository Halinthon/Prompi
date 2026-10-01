package com.prompi.app.domain.model

data class Category(
    val id: Long,
    val name: String,
    val position: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Categoría junto con el número de fichas que contiene. */
data class CategoryWithCount(
    val category: Category,
    val cardCount: Int,
)
