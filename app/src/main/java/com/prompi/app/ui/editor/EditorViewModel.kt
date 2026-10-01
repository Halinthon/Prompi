package com.prompi.app.ui.editor

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.prompi.app.R
import com.prompi.app.domain.model.Category
import com.prompi.app.domain.model.Limits
import com.prompi.app.domain.model.takeSafe
import com.prompi.app.domain.repository.CardRepository
import com.prompi.app.domain.repository.CategoryRepository
import com.prompi.app.ui.navigation.EditorRoute
import com.prompi.app.ui.navigation.NEW_CARD_ID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Valores editables de una ficha; se compara con el original para saber si hay cambios. */
private data class Draft(val categoryId: Long, val title: String, val content: String, val rating: Int)

sealed interface EditorEvent {
    data object Saved : EditorEvent
    data object NotFound : EditorEvent
    data class Error(@StringRes val messageRes: Int) : EditorEvent
}

/**
 * Estado del editor. Los campos son estado de Compose (para escribir sin retrasos) y se
 * copian en [SavedStateHandle] en cada cambio, así el borrador sobrevive a la rotación y
 * a que Android cierre la app en segundo plano.
 */
class EditorViewModel(
    private val savedStateHandle: SavedStateHandle,
    categoryRepository: CategoryRepository,
    private val cardRepository: CardRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<EditorRoute>()
    val isNew: Boolean = route.cardId == NEW_CARD_ID

    val categories: StateFlow<List<Category>> = categoryRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var isLoading by mutableStateOf(savedStateHandle.get<Boolean>(KEY_LOADED) != true)
        private set
    var isSaving by mutableStateOf(false)
        private set

    var title by mutableStateOf(savedStateHandle.get<String>(KEY_TITLE) ?: "")
        private set
    var content by mutableStateOf(savedStateHandle.get<String>(KEY_CONTENT) ?: "")
        private set
    var rating by mutableStateOf(savedStateHandle.get<Int>(KEY_RATING) ?: Limits.RATING_DEFAULT)
        private set
    var categoryId by mutableStateOf(savedStateHandle.get<Long>(KEY_CATEGORY) ?: route.categoryId)
        private set

    /** Se activa al intentar guardar sin título, para mostrar el error solo entonces. */
    var showTitleError by mutableStateOf(savedStateHandle.get<Boolean>(KEY_TITLE_ERROR) ?: false)
        private set

    /**
     * Valores con los que se abrió el editor, para detectar cambios sin guardar. No se guarda
     * en [SavedStateHandle] (el contenido puede ocupar 7000 caracteres): si Android cierra la
     * app en segundo plano, se vuelve a leer de la base de datos.
     */
    private var original by mutableStateOf(Draft(route.categoryId, "", "", Limits.RATING_DEFAULT))

    private val _events = Channel<EditorEvent>(Channel.BUFFERED)
    val events: Flow<EditorEvent> = _events.receiveAsFlow()

    val hasChanges: Boolean
        get() = Draft(categoryId, title, content, rating) != original

    val isTitleValid: Boolean get() = title.isNotBlank()

    init {
        if (isLoading) {
            load()
        } else if (!isNew) {
            restoreOriginal()
        }
    }

    /** Tras recrear el proceso, recupera el original sin tocar el borrador restaurado. */
    private fun restoreOriginal() = viewModelScope.launch {
        val card = runCatching { cardRepository.get(route.cardId) }.getOrNull() ?: return@launch
        original = Draft(card.categoryId, card.title, card.content, card.rating)
    }

    private fun load() = viewModelScope.launch {
        if (!isNew) {
            val card = try {
                cardRepository.get(route.cardId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            if (card == null) {
                _events.send(EditorEvent.NotFound)
                return@launch
            }
            original = Draft(card.categoryId, card.title, card.content, card.rating)
            updateTitle(card.title)
            updateContent(card.content)
            updateRating(card.rating)
            updateCategory(card.categoryId)
        }
        savedStateHandle[KEY_LOADED] = true
        isLoading = false
    }

    fun updateTitle(value: String) {
        // Se eliminan los saltos de línea (p. ej. al pegar) porque el título es de una línea.
        val clean = value.replace('\n', ' ').replace('\r', ' ').takeSafe(Limits.TITLE_MAX)
        title = clean
        savedStateHandle[KEY_TITLE] = clean
        if (clean.isNotBlank() && showTitleError) setTitleError(false)
    }

    fun updateContent(value: String) {
        val clean = value.takeSafe(Limits.CONTENT_MAX)
        content = clean
        savedStateHandle[KEY_CONTENT] = clean
    }

    fun updateRating(value: Int) {
        val clean = value.coerceIn(Limits.RATING_MIN, Limits.RATING_MAX)
        rating = clean
        savedStateHandle[KEY_RATING] = clean
    }

    fun updateCategory(id: Long) {
        categoryId = id
        savedStateHandle[KEY_CATEGORY] = id
    }

    private fun setTitleError(value: Boolean) {
        showTitleError = value
        savedStateHandle[KEY_TITLE_ERROR] = value
    }

    fun save() {
        if (isSaving || isLoading) return
        if (!isTitleValid) {
            setTitleError(true)
            return
        }
        isSaving = true
        viewModelScope.launch {
            try {
                val cleanTitle = title.trim()
                if (isNew) {
                    cardRepository.create(categoryId, cleanTitle, content, rating)
                } else {
                    cardRepository.update(route.cardId, categoryId, cleanTitle, content, rating)
                }
                _events.send(EditorEvent.Saved)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                isSaving = false
                _events.send(EditorEvent.Error(R.string.error_save))
            }
        }
    }

    private companion object {
        const val KEY_LOADED = "editor_loaded"
        const val KEY_TITLE = "editor_title"
        const val KEY_CONTENT = "editor_content"
        const val KEY_RATING = "editor_rating"
        const val KEY_CATEGORY = "editor_category"
        const val KEY_TITLE_ERROR = "editor_title_error"
    }
}
