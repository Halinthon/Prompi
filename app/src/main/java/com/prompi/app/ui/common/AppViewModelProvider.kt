package com.prompi.app.ui.common

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.prompi.app.MainViewModel
import com.prompi.app.PrompiApplication
import com.prompi.app.ui.cards.CardsViewModel
import com.prompi.app.ui.categories.CategoriesViewModel
import com.prompi.app.ui.editor.EditorViewModel
import com.prompi.app.ui.favorites.FavoritesViewModel
import com.prompi.app.ui.search.SearchViewModel
import com.prompi.app.ui.settings.SettingsViewModel

/** Fábrica única de ViewModels que obtiene las dependencias del [com.prompi.app.di.AppContainer]. */
object AppViewModelProvider {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer { MainViewModel(app().container.settingsRepository) }
        initializer { CategoriesViewModel(app().container.categoryRepository) }
        initializer {
            CardsViewModel(
                savedStateHandle = createSavedStateHandle(),
                categoryRepository = app().container.categoryRepository,
                cardRepository = app().container.cardRepository,
            )
        }
        initializer {
            EditorViewModel(
                savedStateHandle = createSavedStateHandle(),
                categoryRepository = app().container.categoryRepository,
                cardRepository = app().container.cardRepository,
            )
        }
        initializer { FavoritesViewModel(createSavedStateHandle(), app().container.cardRepository) }
        initializer { SearchViewModel(createSavedStateHandle(), app().container.cardRepository) }
        initializer {
            SettingsViewModel(app().container.settingsRepository, app().container.backupRepository)
        }
    }
}

private fun CreationExtras.app(): PrompiApplication =
    checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as PrompiApplication
