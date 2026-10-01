package com.prompi.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.ui.graphics.vector.ImageVector
import com.prompi.app.R
import kotlin.reflect.KClass

enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    @StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    CATEGORIES(CategoriesRoute, CategoriesRoute::class, R.string.nav_categories, Icons.Filled.Folder, Icons.Outlined.Folder),
    SEARCH(SearchRoute, SearchRoute::class, R.string.nav_search, Icons.Filled.Search, Icons.Outlined.Search),
    FAVORITES(FavoritesRoute, FavoritesRoute::class, R.string.nav_favorites, Icons.Filled.Star, Icons.Outlined.StarOutline),
}
