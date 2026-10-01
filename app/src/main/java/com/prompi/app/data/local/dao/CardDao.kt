package com.prompi.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.prompi.app.data.local.entity.CardEntity
import com.prompi.app.data.local.entity.CardWithCategoryRow
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CardDao {

    @Query("SELECT * FROM cards WHERE category_id = :categoryId ORDER BY position ASC, id ASC")
    abstract fun observeByCategory(categoryId: Long): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE category_id = :categoryId ORDER BY position ASC, id ASC")
    abstract suspend fun getByCategory(categoryId: Long): List<CardEntity>

    @Query("SELECT * FROM cards ORDER BY category_id ASC, position ASC, id ASC")
    abstract suspend fun getAll(): List<CardEntity>

    @Query("SELECT * FROM cards WHERE id = :id")
    abstract suspend fun get(id: Long): CardEntity?

    @Query("SELECT COUNT(*) FROM cards")
    abstract fun observeCount(): Flow<Int>

    @Query(
        """
        SELECT c.*, cat.name AS category_name
        FROM cards c
        INNER JOIN categories cat ON cat.id = c.category_id
        WHERE c.is_favorite = 1
        ORDER BY cat.position ASC, c.position ASC, c.id ASC
        """,
    )
    abstract fun observeFavorites(): Flow<List<CardWithCategoryRow>>

    /**
     * Búsqueda de texto completo. Las coincidencias en el título aparecen primero;
     * después, las más recientes. Se limita a 200 resultados para mantener la fluidez.
     */
    @Query(
        """
        SELECT c.*, cat.name AS category_name
        FROM cards c
        INNER JOIN categories cat ON cat.id = c.category_id
        WHERE c.id IN (SELECT rowid FROM cards_fts WHERE cards_fts MATCH :query)
        ORDER BY
            CASE WHEN c.id IN (SELECT rowid FROM cards_fts WHERE cards_fts MATCH :titleQuery)
                THEN 0 ELSE 1 END,
            c.updated_at DESC
        LIMIT 200
        """,
    )
    abstract fun search(query: String, titleQuery: String): Flow<List<CardWithCategoryRow>>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM cards WHERE category_id = :categoryId")
    abstract suspend fun nextPosition(categoryId: Long): Int

    @Insert
    abstract suspend fun insert(card: CardEntity): Long

    @Update
    abstract suspend fun update(card: CardEntity)

    @Query("UPDATE cards SET is_favorite = :favorite WHERE id = :id")
    abstract suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE cards SET rating = :rating WHERE id = :id")
    abstract suspend fun setRating(id: Long, rating: Int)

    @Query("UPDATE cards SET color = :color WHERE id = :id")
    abstract suspend fun setColor(id: Long, color: String)

    @Query("DELETE FROM cards WHERE id = :id")
    abstract suspend fun deleteById(id: Long)

    @Query("DELETE FROM cards WHERE category_id = :categoryId")
    abstract suspend fun deleteByCategory(categoryId: Long)

    @Query("DELETE FROM cards")
    abstract suspend fun deleteAll()

    /** Desplaza una posición hacia abajo todas las fichas de una categoría. */
    @Query("UPDATE cards SET position = position + 1 WHERE category_id = :categoryId")
    abstract suspend fun shiftDown(categoryId: Long)

    @Query("UPDATE cards SET position = :position WHERE id = :id")
    abstract suspend fun updatePosition(id: Long, position: Int)

    /** Mueve todas las fichas de una categoría a otra, conservando su orden relativo. */
    @Query("UPDATE cards SET category_id = :to, position = position + :offset WHERE category_id = :from")
    abstract suspend fun moveAll(from: Long, to: Long, offset: Int)

    /** Inserta una ficha nueva en la primera posición de su categoría. */
    @Transaction
    open suspend fun insertAtTop(card: CardEntity): Long {
        shiftDown(card.categoryId)
        return insert(card.copy(position = 0))
    }

    @Transaction
    open suspend fun reorder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> updatePosition(id, index) }
    }
}
