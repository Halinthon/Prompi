package com.prompi.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Ficha almacenada. El contenido se guarda en la columna "body" (y no "content") para
 * evitar cualquier ambigüedad con la opción "content=" de las tablas virtuales FTS4.
 */
@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("category_id"), Index("is_favorite")],
)
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    val title: String,
    @ColumnInfo(name = "body") val content: String,
    val rating: Int,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean,
    val color: String,
    val position: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
