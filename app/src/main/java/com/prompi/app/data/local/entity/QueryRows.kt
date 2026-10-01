package com.prompi.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class CategoryWithCountRow(
    @Embedded val category: CategoryEntity,
    @ColumnInfo(name = "card_count") val cardCount: Int,
)

data class CardWithCategoryRow(
    @Embedded val card: CardEntity,
    @ColumnInfo(name = "category_name") val categoryName: String,
)
