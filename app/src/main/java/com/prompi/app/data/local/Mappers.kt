package com.prompi.app.data.local

import com.prompi.app.data.local.entity.CardEntity
import com.prompi.app.data.local.entity.CardWithCategoryRow
import com.prompi.app.data.local.entity.CategoryEntity
import com.prompi.app.data.local.entity.CategoryWithCountRow
import com.prompi.app.domain.model.Card
import com.prompi.app.domain.model.CardColor
import com.prompi.app.domain.model.CardWithCategory
import com.prompi.app.domain.model.Category
import com.prompi.app.domain.model.CategoryWithCount

fun CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    position = position,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun CategoryWithCountRow.toDomain() = CategoryWithCount(category.toDomain(), cardCount)

fun CardEntity.toDomain() = Card(
    id = id,
    categoryId = categoryId,
    title = title,
    content = content,
    rating = rating,
    isFavorite = isFavorite,
    color = CardColor.fromKey(color),
    position = position,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Card.toEntity() = CardEntity(
    id = id,
    categoryId = categoryId,
    title = title,
    content = content,
    rating = rating,
    isFavorite = isFavorite,
    color = color.name,
    position = position,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun CardWithCategoryRow.toDomain() = CardWithCategory(card.toDomain(), categoryName)
