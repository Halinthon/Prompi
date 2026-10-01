package com.prompi.app.ui.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prompi.app.R
import com.prompi.app.domain.model.Card
import com.prompi.app.ui.common.AppViewModelProvider
import com.prompi.app.ui.common.CardActions
import com.prompi.app.ui.common.CardListEffects
import com.prompi.app.ui.common.rememberCardActions
import com.prompi.app.ui.common.rememberCardDialogState
import com.prompi.app.ui.components.BackButton
import com.prompi.app.ui.components.DragHandle
import com.prompi.app.ui.components.EmptyState
import com.prompi.app.ui.components.LoadingBox
import com.prompi.app.ui.components.MaxContentWidth
import com.prompi.app.ui.components.PromptCard
import com.prompi.app.ui.components.listContentPadding
import com.prompi.app.ui.components.quantityString
import com.prompi.app.ui.navigation.NavResultMessage
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun CardsScreen(
    resultHandle: SavedStateHandle,
    onBack: () -> Unit,
    onCategoryMissing: () -> Unit,
    onNewCard: (categoryId: Long) -> Unit,
    onEditCard: (Card) -> Unit,
    viewModel: CardsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val expandedIds by viewModel.expandedIds.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val dialogState = rememberCardDialogState()
    val actions = rememberCardActions(viewModel, dialogState, snackbarHostState, onEditCard)
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val currentOnMissing by rememberUpdatedState(onCategoryMissing)

    NavResultMessage(resultHandle, snackbarHostState)

    // Si la categoría desaparece (eliminada o reemplazada al importar), se vuelve atrás.
    LaunchedEffect(state) {
        if (state is CardsUiState.Missing) currentOnMissing()
    }

    val ready = state as? CardsUiState.Ready
    CardListEffects(viewModel, dialogState, snackbarHostState) { id -> ready?.cards?.firstOrNull { it.id == id } }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton(onBack) },
                title = {
                    if (ready != null) {
                        Column {
                            Text(ready.category.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                quantityString(R.plurals.card_count, ready.cards.size, ready.cards.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            if (ready != null) {
                ExtendedFloatingActionButton(
                    onClick = { onNewCard(ready.category.id) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.action_new_card)) },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            ready == null -> LoadingBox(Modifier.padding(padding))
            ready.cards.isEmpty() -> EmptyState(
                icon = Icons.AutoMirrored.Outlined.NoteAdd,
                title = stringResource(R.string.empty_cards_title),
                message = stringResource(R.string.empty_cards_message),
                modifier = Modifier.padding(padding),
            )
            else -> ReorderableCardList(
                cards = ready.cards,
                expandedIds = expandedIds,
                actions = actions,
                contentPadding = listContentPadding(padding, extraBottom = 88.dp),
                onReorder = viewModel::reorder,
            )
        }
    }
}

@Composable
private fun ReorderableCardList(
    cards: List<Card>,
    expandedIds: Set<Long>,
    actions: CardActions,
    contentPadding: PaddingValues,
    onReorder: (List<Long>) -> Unit,
) {
    // Copia local para un arrastre fluido; se guarda el orden al soltar.
    val items = remember { cards.toMutableStateList() }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(cards) {
        if (!dragging) {
            items.clear()
            items.addAll(cards)
        }
    }

    fun commit() = onReorder(items.map { it.id })

    fun move(from: Int, to: Int) {
        if (to !in items.indices) return
        items.add(to, items.removeAt(from))
        commit()
    }

    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        items.add(to.index, items.removeAt(from.index))
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            state = listState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.widthIn(max = MaxContentWidth).fillMaxSize(),
        ) {
            itemsIndexed(items, key = { _, card -> card.id }) { index, card ->
                ReorderableItem(reorderState, key = card.id) { isDragging ->
                    PromptCard(
                        card = card,
                        expanded = card.id in expandedIds,
                        actions = actions,
                        isDragging = isDragging,
                        dragHandle = {
                            DragHandle(
                                Modifier.draggableHandle(
                                    onDragStarted = { dragging = true },
                                    onDragStopped = {
                                        dragging = false
                                        commit()
                                    },
                                ),
                            )
                        },
                        onMoveUp = if (index > 0) ({ move(index, index - 1) }) else null,
                        onMoveDown = if (index in 0 until items.lastIndex) ({ move(index, index + 1) }) else null,
                    )
                }
            }
        }
    }
}
