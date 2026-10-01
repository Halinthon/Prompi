package com.prompi.app.ui.categories

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prompi.app.R
import com.prompi.app.domain.model.CategoryWithCount
import com.prompi.app.domain.repository.CategoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoriesUiState(
    val isLoading: Boolean = true,
    val categories: List<CategoryWithCount> = emptyList(),
    val totalCards: Int = 0,
)

class CategoriesViewModel(private val repository: CategoryRepository) : ViewModel() {

    val uiState: StateFlow<CategoriesUiState> = combine(
        repository.observeCategoriesWithCount(),
        repository.observeTotalCardCount(),
    ) { categories, total ->
        CategoriesUiState(isLoading = false, categories = categories, totalCards = total)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())

    private val _messages = Channel<Int>(Channel.BUFFERED)
    val messages: Flow<Int> = _messages.receiveAsFlow()

    fun create(name: String) = launchSafely(R.string.msg_category_created) { repository.create(name.trim()) }

    fun rename(id: Long, name: String) = launchSafely(R.string.msg_category_renamed) { repository.rename(id, name.trim()) }

    fun delete(id: Long, moveCardsTo: Long?) = launchSafely(R.string.msg_category_deleted) {
        repository.delete(id, moveCardsTo)
    }

    fun reorder(orderedIds: List<Long>) = launchSafely(null) { repository.reorder(orderedIds) }

    private fun launchSafely(@StringRes successRes: Int?, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
                if (successRes != null) _messages.send(successRes)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.send(R.string.error_generic)
            }
        }
    }
}

/** Valida un nombre de categoría. Devuelve el texto de error o `null` si es válido. */
@StringRes
fun validateCategoryName(name: String, existing: List<CategoryWithCount>, excludeId: Long? = null): Int? {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return R.string.error_name_required
    val duplicate = existing.any {
        it.category.id != excludeId && it.category.name.trim().equals(trimmed, ignoreCase = true)
    }
    return if (duplicate) R.string.error_name_duplicate else null
}
