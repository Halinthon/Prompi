package com.prompi.app.ui.navigation

import kotlinx.serialization.Serializable

@Serializable data object CategoriesRoute
@Serializable data object SearchRoute
@Serializable data object FavoritesRoute
@Serializable data object SettingsRoute

@Serializable data class CardsRoute(val categoryId: Long)

/** Editor de fichas. [cardId] = [NEW_CARD_ID] crea una ficha nueva en [categoryId]. */
@Serializable data class EditorRoute(val categoryId: Long, val cardId: Long = NEW_CARD_ID)

const val NEW_CARD_ID = -1L

/** Clave del mensaje que una pantalla devuelve a la anterior (p. ej. "Ficha guardada"). */
const val NAV_RESULT_MESSAGE = "nav_result_message"
