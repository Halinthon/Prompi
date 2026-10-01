package com.prompi.app.domain.repository

import android.net.Uri
import com.prompi.app.domain.model.Card
import com.prompi.app.domain.model.CardColor
import com.prompi.app.domain.model.CardWithCategory
import com.prompi.app.domain.model.Category
import com.prompi.app.domain.model.CategoryWithCount
import com.prompi.app.domain.model.ImportMode
import com.prompi.app.domain.model.ImportResult
import com.prompi.app.domain.model.ParsedBackup
import com.prompi.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeCategoriesWithCount(): Flow<List<CategoryWithCount>>
    fun observeCategories(): Flow<List<Category>>
    fun observeCategory(id: Long): Flow<Category?>
    fun observeTotalCardCount(): Flow<Int>
    suspend fun create(name: String): Long
    suspend fun rename(id: Long, name: String)

    /**
     * Elimina una categoría. Si [moveCardsTo] no es nulo, sus fichas se mueven al final
     * de esa categoría; en caso contrario, se eliminan junto con ella.
     */
    suspend fun delete(id: Long, moveCardsTo: Long?)
    suspend fun reorder(orderedIds: List<Long>)
}

interface CardRepository {
    fun observeByCategory(categoryId: Long): Flow<List<Card>>
    fun observeFavorites(): Flow<List<CardWithCategory>>
    fun search(query: String): Flow<List<CardWithCategory>>
    suspend fun get(id: Long): Card?
    suspend fun create(categoryId: Long, title: String, content: String, rating: Int): Long
    suspend fun update(id: Long, categoryId: Long, title: String, content: String, rating: Int)
    suspend fun setFavorite(id: Long, favorite: Boolean)
    suspend fun setRating(id: Long, rating: Int)
    suspend fun setColor(id: Long, color: CardColor)

    /** Elimina la ficha y la devuelve para poder deshacer la operación. */
    suspend fun delete(id: Long): Card?
    suspend fun restore(card: Card)
    suspend fun reorder(orderedIds: List<Long>)
}

interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}

interface BackupRepository {
    /** Exporta todo a [uri] y devuelve el número de fichas exportadas. */
    suspend fun export(uri: Uri): Result<Int>

    /** Lee y valida un archivo sin modificar la base de datos. */
    suspend fun read(uri: Uri): Result<ParsedBackup>

    /** Aplica una copia ya validada en una única transacción. */
    suspend fun import(backup: ParsedBackup, mode: ImportMode): ImportResult
}
