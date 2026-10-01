package com.prompi.app.ui.favorites

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.prompi.app.domain.model.CardWithCategory
import com.prompi.app.domain.repository.CardRepository
import com.prompi.app.ui.common.CardListViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class FavoritesViewModel(
    savedStateHandle: SavedStateHandle,
    cardRepository: CardRepository,
) : CardListViewModel(savedStateHandle, cardRepository) {

    /** `null` mientras se carga. */
    val favorites: StateFlow<List<CardWithCategory>?> = cardRepository.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
