package com.prompi.app.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.prompi.app.domain.model.CardWithCategory
import com.prompi.app.domain.model.Limits
import com.prompi.app.domain.repository.CardRepository
import com.prompi.app.ui.common.CardListViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface SearchUiState {
    /** Sin texto: se muestra la invitación a buscar. */
    data object Idle : SearchUiState
    data object NoResults : SearchUiState
    data class Results(val items: List<CardWithCategory>) : SearchUiState
}

class SearchViewModel(
    savedStateHandle: SavedStateHandle,
    cardRepository: CardRepository,
) : CardListViewModel(savedStateHandle, cardRepository) {

    /** El texto se conserva al rotar y al cambiar de pestaña. */
    val query: StateFlow<String> = savedStateHandle.getStateFlow(KEY_QUERY, "")

    fun onQueryChange(value: String) {
        savedStateHandle[KEY_QUERY] = value.take(Limits.SEARCH_QUERY_MAX)
    }

    fun clearQuery() {
        savedStateHandle[KEY_QUERY] = ""
    }

    val uiState: StateFlow<SearchUiState> = query
        .map { it.trim() }
        .distinctUntilChanged()
        .debounce { if (it.isEmpty()) 0L else DEBOUNCE_MS }
        .flatMapLatest { text ->
            if (text.isEmpty()) {
                flowOf(SearchUiState.Idle)
            } else {
                cardRepository.search(text)
                    .map { list -> if (list.isEmpty()) SearchUiState.NoResults else SearchUiState.Results(list) }
                    .catch { emit(SearchUiState.NoResults) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState.Idle)

    private companion object {
        const val KEY_QUERY = "search_query"
        const val DEBOUNCE_MS = 300L
    }
}
