package com.prompi.app.ui.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
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
import com.prompi.app.ui.components.MaxContentWidth
import com.prompi.app.ui.components.listContentPadding
import com.prompi.app.ui.components.quantityString
import com.prompi.app.ui.navigation.NavResultMessage

@Composable
fun SearchScreen(
    resultHandle: SavedStateHandle,
    onEditCard: (Card) -> Unit,
    viewModel: SearchViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val expandedIds by viewModel.expandedIds.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val dialogState = rememberCardDialogState()
    val actions = rememberCardActions(viewModel, dialogState, snackbarHostState, onEditCard)
    val keyboard = LocalSoftwareKeyboardController.current

    NavResultMessage(resultHandle, snackbarHostState)
    CardListEffects(viewModel, dialogState, snackbarHostState) { id ->
        (uiState as? SearchUiState.Results)?.items?.firstOrNull { it.card.id == id }?.card
    }

    Scaffold(
        topBar = {
            SearchField(
                query = query,
                onQueryChange = viewModel::onQueryChange,
                onClear = viewModel::clearQuery,
                onSearch = { keyboard?.hide() },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (val state = uiState) {
            SearchUiState.Idle -> EmptyState(
                icon = Icons.Filled.Search,
                title = stringResource(R.string.search_idle_title),
                message = stringResource(R.string.search_idle_message),
                modifier = Modifier.padding(padding),
            )
            SearchUiState.NoResults -> EmptyState(
                icon = Icons.Outlined.SearchOff,
                title = stringResource(R.string.search_no_results_title),
                message = stringResource(R.string.search_no_results_message),
                modifier = Modifier.padding(padding),
            )
            is SearchUiState.Results -> CardWithCategoryList(
                items = state.items,
                expandedIds = expandedIds,
                actions = actions,
                contentPadding = listContentPadding(padding),
                header = {
                    item(key = "results_count") {
                        Text(
                            text = quantityString(R.plurals.search_results_count, state.items.size, state.items.size),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    // Enfoca el campo solo la primera vez que se abre la pestaña (no tras rotar ni al volver).
    var autoFocusDone by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!autoFocusDone) {
            autoFocusDone = true
            if (query.isEmpty()) runCatching { focusRequester.requestFocus() }
        }
    }

    Surface(color = MaterialTheme.colorScheme.surface) {
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(Modifier.widthIn(max = MaxContentWidth).fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = onClear) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_clear_search))
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                )
            }
        }
    }
}
