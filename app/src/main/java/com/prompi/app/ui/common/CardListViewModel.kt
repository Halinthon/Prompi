package com.prompi.app.ui.common

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prompi.app.R
import com.prompi.app.domain.model.Card
import com.prompi.app.domain.model.CardColor
import com.prompi.app.domain.repository.CardRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Eventos de un solo uso que la pantalla muestra en un Snackbar. */
sealed interface CardListEvent {
    data object CardDeleted : CardListEvent
    data class Message(@StringRes val messageRes: Int) : CardListEvent
}

/**
 * Lógica común a las pantallas que muestran fichas (categoría, Favoritos y Búsqueda):
 * expandir, favoritos, estrellas, color, eliminar y deshacer.
 * Las fichas expandidas se guardan en SavedStateHandle para sobrevivir a rotaciones
 * y a la muerte del proceso.
 */
abstract class CardListViewModel(
    protected val savedStateHandle: SavedStateHandle,
    protected val cardRepository: CardRepository,
) : ViewModel() {

    val expandedIds: StateFlow<Set<Long>> = savedStateHandle
        .getStateFlow(KEY_EXPANDED, LongArray(0))
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val _events = Channel<CardListEvent>(Channel.BUFFERED)
    val events: Flow<CardListEvent> = _events.receiveAsFlow()

    private var lastDeleted: Card? = null

    fun toggleExpanded(cardId: Long) {
        val current = savedStateHandle.get<LongArray>(KEY_EXPANDED) ?: LongArray(0)
        savedStateHandle[KEY_EXPANDED] = if (cardId in current) {
            current.filter { it != cardId }.toLongArray()
        } else {
            current + cardId
        }
    }

    fun toggleFavorite(card: Card) = launchSafely {
        val favorite = !card.isFavorite
        cardRepository.setFavorite(card.id, favorite)
        sendEvent(
            CardListEvent.Message(if (favorite) R.string.msg_added_favorite else R.string.msg_removed_favorite),
        )
    }

    fun setRating(card: Card, rating: Int) {
        if (rating == card.rating) return
        launchSafely { cardRepository.setRating(card.id, rating) }
    }

    fun setColor(card: Card, color: CardColor) {
        if (color == card.color) return
        launchSafely { cardRepository.setColor(card.id, color) }
    }

    fun delete(card: Card) = launchSafely {
        val deleted = cardRepository.delete(card.id)
        if (deleted != null) {
            lastDeleted = deleted
            sendEvent(CardListEvent.CardDeleted)
        }
    }

    fun undoDelete() {
        val card = lastDeleted ?: return
        lastDeleted = null
        launchSafely { cardRepository.restore(card) }
    }

    protected suspend fun sendEvent(event: CardListEvent) = _events.send(event)

    /** Ejecuta una operación y muestra un mensaje genérico si falla, sin cerrar la app. */
    protected fun launchSafely(block: suspend CoroutineScope.() -> Unit): Job =
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.send(CardListEvent.Message(R.string.error_generic))
            }
        }

    private companion object {
        const val KEY_EXPANDED = "expanded_card_ids"
    }
}
