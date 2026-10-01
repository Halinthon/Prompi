package com.prompi.app.ui.cards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.prompi.app.domain.model.Card
import com.prompi.app.domain.model.Category
import com.prompi.app.domain.repository.CardRepository
import com.prompi.app.domain.repository.CategoryRepository
import com.prompi.app.ui.common.CardListViewModel
import com.prompi.app.ui.navigation.CardsRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface CardsUiState {
    data object Loading : CardsUiState

    /** La categoría ya no existe (p. ej. se eliminó o se reemplazó al importar). */
    data object Missing : CardsUiState
    data class Ready(val category: Category, val cards: List<Card>) : CardsUiState
}

class CardsViewModel(
    savedStateHandle: SavedStateHandle,
    categoryRepository: CategoryRepository,
    cardRepository: CardRepository,
) : CardListViewModel(savedStateHandle, cardRepository) {

    val categoryId: Long = savedStateHandle.toRoute<CardsRoute>().categoryId

    val uiState: StateFlow<CardsUiState> = combine(
        categoryRepository.observeCategory(categoryId),
        cardRepository.observeByCategory(categoryId),
    ) { category, cards ->
        if (category == null) CardsUiState.Missing else CardsUiState.Ready(category, cards)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CardsUiState.Loading)

    fun reorder(orderedIds: List<Long>) = launchSafely { cardRepository.reorder(orderedIds) }
}
