package com.prompi.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.prompi.app.data.local.entity.CategoryEntity
import com.prompi.app.data.local.entity.CategoryWithCountRow
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CategoryDao {

    @Query(
        """
        SELECT cat.*, COUNT(c.id) AS card_count
        FROM categories cat
        LEFT JOIN cards c ON c.category_id = cat.id
        GROUP BY cat.id
        ORDER BY cat.position ASC, cat.id ASC
        """,
    )
    abstract fun observeWithCount(): Flow<List<CategoryWithCountRow>>

    @Query("SELECT * FROM categories ORDER BY position ASC, id ASC")
    abstract fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY position ASC, id ASC")
    abstract suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    abstract fun observeById(id: Long): Flow<CategoryEntity?>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM categories")
    abstract suspend fun nextPosition(): Int

    @Insert
    abstract suspend fun insert(category: CategoryEntity): Long

    @Query("UPDATE categories SET name = :name, updated_at = :updatedAt WHERE id = :id")
    abstract suspend fun rename(id: Long, name: String, updatedAt: Long)

    @Query("DELETE FROM categories WHERE id = :id")
    abstract suspend fun deleteById(id: Long)

    @Query("DELETE FROM categories")
    abstract suspend fun deleteAll()

    @Query("UPDATE categories SET position = :position WHERE id = :id")
    abstract suspend fun updatePosition(id: Long, position: Int)

    /** Guarda el nuevo orden completo en una sola transacción. */
    @Transaction
    open suspend fun reorder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> updatePosition(id, index) }
    }
}
