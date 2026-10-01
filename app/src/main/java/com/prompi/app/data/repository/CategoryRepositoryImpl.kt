package com.prompi.app.data.repository

import androidx.room.withTransaction
import com.prompi.app.data.local.PrompiDatabase
import com.prompi.app.data.local.entity.CategoryEntity
import com.prompi.app.data.local.toDomain
import com.prompi.app.domain.model.Category
import com.prompi.app.domain.model.CategoryWithCount
import com.prompi.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepositoryImpl(private val db: PrompiDatabase) : CategoryRepository {

    private val categoryDao = db.categoryDao()
    private val cardDao = db.cardDao()

    override fun observeCategoriesWithCount(): Flow<List<CategoryWithCount>> =
        categoryDao.observeWithCount().map { rows -> rows.map { it.toDomain() } }

    override fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeCategory(id: Long): Flow<Category?> =
        categoryDao.observeById(id).map { it?.toDomain() }

    override fun observeTotalCardCount(): Flow<Int> = cardDao.observeCount()

    override suspend fun create(name: String): Long = db.withTransaction {
        val now = System.currentTimeMillis()
        categoryDao.insert(
            CategoryEntity(
                name = name.trim(),
                position = categoryDao.nextPosition(),
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    override suspend fun rename(id: Long, name: String) {
        categoryDao.rename(id, name.trim(), System.currentTimeMillis())
    }

    override suspend fun delete(id: Long, moveCardsTo: Long?) {
        db.withTransaction {
            if (moveCardsTo != null && moveCardsTo != id) {
                cardDao.moveAll(from = id, to = moveCardsTo, offset = cardDao.nextPosition(moveCardsTo))
            } else {
                // Se borran explícitamente (además del CASCADE) para mantener sincronizado el índice FTS.
                cardDao.deleteByCategory(id)
            }
            categoryDao.deleteById(id)
        }
    }

    override suspend fun reorder(orderedIds: List<Long>) = categoryDao.reorder(orderedIds)
}
