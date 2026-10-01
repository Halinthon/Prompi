package com.prompi.app.ui.favorites

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prompi.app.R
import com.prompi.app.domain.model.Card
import com.prompi.app.ui.common.AppViewModelProvider
import com.prompi.app.ui.common.CardListEffects
import com.prompi.app.ui.common.CardWithCategoryList
import com.prompi.app.ui.common.rememberCardActions
import com.prompi.app.ui.common.rememberCardDialogState
import com.prompi.app.ui.components.EmptyState
import com.prompi.app.ui.components.LoadingBox
import com.prompi.app.ui.components.listContentPadding
import com.prompi.app.ui.components.quantityString
import com.prompi.app.ui.navigation.NavResultMessage

@Composable
fun FavoritesScreen(
    resultHandle: SavedStateHandle,
    onEditCard: (Card) -> Unit,
    viewModel: FavoritesViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val expandedIds by viewModel.expandedIds.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val dialogState = rememberCardDialogState()
    val actions = rememberCardActions(viewModel, dialogState, snackbarHostState, onEditCard)
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    NavResultMessage(resultHandle, snackbarHostState)
    CardListEffects(viewModel, dialogState, snackbarHostState) { id ->
        favorites?.firstOrNull { it.card.id == id }?.card
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.nav_favorites))
                        favorites?.let {
                            Text(
                                quantityString(R.plurals.card_count, it.size, it.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val list = favorites
        when {
            list == null -> LoadingBox(Modifier.padding(padding))
            list.isEmpty() -> EmptyState(
                icon = Icons.Outlined.StarOutline,
                title = stringResource(R.string.empty_favorites_title),
                message = stringResource(R.string.empty_favorites_message),
                modifier = Modifier.padding(padding),
            )
            else -> CardWithCategoryList(
                items = list,
                expandedIds = expandedIds,
                actions = actions,
                contentPadding = listContentPadding(padding),
            )
        }
    }
}
