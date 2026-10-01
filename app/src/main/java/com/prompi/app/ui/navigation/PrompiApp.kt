package com.prompi.app.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@Composable
fun PrompiApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination

    val showBottomBar = destination == null ||
        !(destination.hasRoute(EditorRoute::class) || destination.hasRoute(SettingsRoute::class))

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { item ->
                        val selected = destination.isInHierarchy(item)
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToTopLevel(item, selected) },
                            icon = {
                                Icon(if (selected) item.selectedIcon else item.unselectedIcon, contentDescription = null)
                            },
                            label = { Text(stringResource(item.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        PrompiNavHost(
            navController = navController,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        )
    }
}

/** La pestaña Categorías también queda marcada dentro de una categoría (CardsRoute). */
private fun NavDestination?.isInHierarchy(item: TopLevelDestination): Boolean {
    if (this == null) return item == TopLevelDestination.CATEGORIES
    if (item == TopLevelDestination.CATEGORIES && hasRoute(CardsRoute::class)) return true
    return hierarchy.any { it.hasRoute(item.routeClass) }
}

fun NavHostController.navigateToTopLevel(item: TopLevelDestination, alreadySelected: Boolean = false) {
    if (alreadySelected) {
        // Volver a pulsar Categorías regresa a la lista principal.
        if (item == TopLevelDestination.CATEGORIES) popBackStack(CategoriesRoute, inclusive = false)
        return
    }
    navigate(item.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
