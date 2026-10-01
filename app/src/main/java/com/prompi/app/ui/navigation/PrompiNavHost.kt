package com.prompi.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.prompi.app.R
import com.prompi.app.ui.cards.CardsScreen
import com.prompi.app.ui.categories.CategoriesScreen
import com.prompi.app.ui.editor.EditorScreen
import com.prompi.app.ui.favorites.FavoritesScreen
import com.prompi.app.ui.search.SearchScreen
import com.prompi.app.ui.settings.SettingsScreen

@Composable
fun PrompiNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    /** Ignora pulsaciones dobles durante las transiciones (evita navegar dos veces). */
    fun NavBackStackEntry.ifResumed(block: () -> Unit) {
        if (lifecycle.currentState == Lifecycle.State.RESUMED) block()
    }

    NavHost(navController = navController, startDestination = CategoriesRoute, modifier = modifier) {
        composable<CategoriesRoute> { entry ->
            CategoriesScreen(
                onOpenCategory = { id -> entry.ifResumed { navController.navigate(CardsRoute(id)) } },
                onOpenSearch = { entry.ifResumed { navController.navigateToTopLevel(TopLevelDestination.SEARCH) } },
                onOpenSettings = { entry.ifResumed { navController.navigate(SettingsRoute) } },
            )
        }
        composable<CardsRoute> { entry ->
            CardsScreen(
                resultHandle = entry.savedStateHandle,
                onBack = { entry.ifResumed { navController.popBackStack() } },
                onCategoryMissing = { navController.popBackStack<CardsRoute>(inclusive = true) },
                onNewCard = { categoryId -> entry.ifResumed { navController.navigate(EditorRoute(categoryId)) } },
                onEditCard = { card ->
                    entry.ifResumed { navController.navigate(EditorRoute(card.categoryId, card.id)) }
                },
            )
        }
        composable<EditorRoute> { entry ->
            EditorScreen(
                onClose = { saved ->
                    entry.ifResumed {
                        if (saved) {
                            navController.previousBackStackEntry?.savedStateHandle
                                ?.set(NAV_RESULT_MESSAGE, R.string.msg_card_saved)
                        }
                        navController.popBackStack()
                    }
                },
            )
        }
        composable<FavoritesRoute> { entry ->
            FavoritesScreen(
                resultHandle = entry.savedStateHandle,
                onEditCard = { card ->
                    entry.ifResumed { navController.navigate(EditorRoute(card.categoryId, card.id)) }
                },
            )
        }
        composable<SearchRoute> { entry ->
            SearchScreen(
                resultHandle = entry.savedStateHandle,
                onEditCard = { card ->
                    entry.ifResumed { navController.navigate(EditorRoute(card.categoryId, card.id)) }
                },
            )
        }
        composable<SettingsRoute> { entry ->
            SettingsScreen(onBack = { entry.ifResumed { navController.popBackStack() } })
        }
    }
}
