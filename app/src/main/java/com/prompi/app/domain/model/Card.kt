package com.prompi.app.domain.model

/** Ficha (prompt o texto) perteneciente a una categoría. */
data class Card(
    val id: Long,
    val categoryId: Long,
    val title: String,
    val content: String,
    val rating: Int,
    val isFavorite: Boolean,
    val color: CardColor,
    val position: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Ficha acompañada del nombre de su categoría (Favoritos y Búsqueda). */
data class CardWithCategory(
    val card: Card,
    val categoryName: String,
)

/**
 * Colores disponibles para una ficha. Se guardan por nombre (no por índice) para que
 * la base de datos y las copias de seguridad sigan siendo válidas si se añaden colores.
 */
enum class CardColor {
    GRAY, BLUE, PINK, YELLOW, GREEN;

    companion object {
        val Default = GRAY

        fun fromKey(key: String?): CardColor = entries.firstOrNull { it.name == key } ?: Default

        fun isValidKey(key: String?): Boolean = entries.any { it.name == key }
    }
}
