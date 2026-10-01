package com.prompi.app.data.repository

import androidx.room.withTransaction
import com.prompi.app.data.local.PrompiDatabase
import com.prompi.app.data.local.entity.CardEntity
import com.prompi.app.data.local.toDomain
import com.prompi.app.data.local.toEntity
import com.prompi.app.domain.model.Card
import com.prompi.app.domain.model.CardColor
import com.prompi.app.domain.model.CardWithCategory
import com.prompi.app.domain.model.Limits
import com.prompi.app.domain.repository.CardRepository
import com.prompi.app.domain.search.SearchQueryBuilder
import com.prompi.app.domain.model.takeSafe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class CardRepositoryImpl(private val db: PrompiDatabase) : CardRepository {

    private val cardDao = db.cardDao()

    override fun observeByCategory(categoryId: Long): Flow<List<Card>> =
        cardDao.observeByCategory(categoryId).map { rows -> rows.map { it.toDomain() } }

    override fun observeFavorites(): Flow<List<CardWithCategory>> =
        cardDao.observeFavorites().map { rows -> rows.map { it.toDomain() } }

    override fun search(query: String): Flow<List<CardWithCategory>> {
        val ftsQuery = SearchQueryBuilder.build(query) ?: return flowOf(emptyList())
        return cardDao.search(ftsQuery.all, ftsQuery.titleOnly)
            .map { rows -> rows.map { it.toDomain() } }
            .catch { emit(emptyList()) }
    }

    override suspend fun get(id: Long): Card? = cardDao.get(id)?.toDomain()

    override suspend fun create(categoryId: Long, title: String, content: String, rating: Int): Long {
        val now = System.currentTimeMillis()
        return cardDao.insertAtTop(
            CardEntity(
                categoryId = categoryId,
                title = title.takeSafe(Limits.TITLE_MAX),
                content = content.takeSafe(Limits.CONTENT_MAX),
                rating = rating.coerceIn(Limits.RATING_MIN, Limits.RATING_MAX),
                isFavorite = false,
                color = CardColor.Default.name,
                position = 0,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    override suspend fun update(id: Long, categoryId: Long, title: String, content: String, rating: Int) {
        db.withTransaction {
            val current = cardDao.get(id) ?: return@withTransaction
            var position = current.position
            if (current.categoryId != categoryId) {
                // Al cambiar de categoría, la ficha pasa a la primera posición de la nueva.
                cardDao.shiftDown(categoryId)
                position = 0
            }
            cardDao.update(
                current.copy(
                    categoryId = categoryId,
                    title = title.takeSafe(Limits.TITLE_MAX),
                    content = content.takeSafe(Limits.CONTENT_MAX),
                    rating = rating.coerceIn(Limits.RATING_MIN, Limits.RATING_MAX),
                    position = position,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    override suspend fun setFavorite(id: Long, favorite: Boolean) = cardDao.setFavorite(id, favorite)

    override suspend fun setRating(id: Long, rating: Int) =
        cardDao.setRating(id, rating.coerceIn(Limits.RATING_MIN, Limits.RATING_MAX))

    override suspend fun setColor(id: Long, color: CardColor) = cardDao.setColor(id, color.name)

    override suspend fun delete(id: Long): Card? = db.withTransaction {
        val card = cardDao.get(id)
        if (card != null) cardDao.deleteById(id)
        card?.toDomain()
    }

    override suspend fun restore(card: Card) {
        cardDao.insert(card.toEntity())
    }

    override suspend fun reorder(orderedIds: List<Long>) = cardDao.reorder(orderedIds)
}
