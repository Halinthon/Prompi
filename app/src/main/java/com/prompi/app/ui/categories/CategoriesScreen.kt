package com.prompi.app.ui.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prompi.app.R
import com.prompi.app.domain.model.CategoryWithCount
import com.prompi.app.ui.common.AppViewModelProvider
import com.prompi.app.ui.components.DragHandle
import com.prompi.app.ui.components.EmptyState
import com.prompi.app.ui.components.LoadingBox
import com.prompi.app.ui.components.MaxContentWidth
import com.prompi.app.ui.components.listContentPadding
import com.prompi.app.ui.components.quantityString
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun CategoriesScreen(
    onOpenCategory: (Long) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: CategoriesViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Diálogos abiertos: se guardan IDs para que sigan visibles tras rotar.
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var renameId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { res ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(context.getString(res))
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.app_name))
                        if (!state.isLoading) {
                            Text(
                                quantityString(R.plurals.total_cards, state.totalCards, state.totalCards),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.nav_search))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_new_category)) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.isLoading -> LoadingBox(Modifier.padding(padding))
            state.categories.isEmpty() -> EmptyState(
                icon = Icons.Outlined.CreateNewFolder,
                title = stringResource(R.string.empty_categories_title),
                message = stringResource(R.string.empty_categories_message),
                modifier = Modifier.padding(padding),
            )
            else -> CategoryList(
                categories = state.categories,
                contentPadding = listContentPadding(padding, extraBottom = 88.dp),
                onOpen = onOpenCategory,
                onRename = { renameId = it },
                onDelete = { deleteId = it },
                onReorder = viewModel::reorder,
            )
        }
    }

    if (showCreate) {
        CategoryNameDialog(
            title = stringResource(R.string.dialog_new_category_title),
            confirmLabel = stringResource(R.string.action_create),
            initialName = "",
            validate = { validateCategoryName(it, state.categories) },
            onConfirm = {
                viewModel.create(it)
                showCreate = false
            },
            onDismiss = { showCreate = false },
        )
    }

    renameId?.let { id ->
        val target = state.categories.firstOrNull { it.category.id == id }
        if (target == null) {
            if (!state.isLoading) renameId = null
        } else {
            CategoryNameDialog(
                title = stringResource(R.string.dialog_rename_category_title),
                confirmLabel = stringResource(R.string.action_save),
                initialName = target.category.name,
                validate = { validateCategoryName(it, state.categories, excludeId = id) },
                onConfirm = {
                    if (it != target.category.name) viewModel.rename(id, it)
                    renameId = null
                },
                onDismiss = { renameId = null },
            )
        }
    }

    deleteId?.let { id ->
        val target = state.categories.firstOrNull { it.category.id == id }
        if (target == null) {
            if (!state.isLoading) deleteId = null
        } else {
            DeleteCategoryDialog(
                target = target,
                others = state.categories.filter { it.category.id != id },
                onConfirm = { moveTo ->
                    viewModel.delete(id, moveTo)
                    deleteId = null
                },
                onDismiss = { deleteId = null },
            )
        }
    }
}

@Composable
private fun CategoryList(
    categories: List<CategoryWithCount>,
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    onOpen: (Long) -> Unit,
    onRename: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onReorder: (List<Long>) -> Unit,
) {
    // Copia local para que el arrastre sea fluido; se sincroniza con la base de datos al soltar.
    val items = remember { categories.toMutableStateList() }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(categories) {
        if (!dragging) {
            items.clear()
            items.addAll(categories)
        }
    }

    fun commit() = onReorder(items.map { it.category.id })

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
            itemsIndexed(items, key = { _, item -> item.category.id }) { index, item ->
                ReorderableItem(reorderState, key = item.category.id) { isDragging ->
                    CategoryItem(
                        item = item,
                        isDragging = isDragging,
                        onClick = { onOpen(item.category.id) },
                        onRename = { onRename(item.category.id) },
                        onDelete = { onDelete(item.category.id) },
                        onMoveUp = if (index > 0) ({ move(index, index - 1) }) else null,
                        onMoveDown = if (index in 0 until items.lastIndex) ({ move(index, index + 1) }) else null,
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
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryItem(
    item: CategoryWithCount,
    isDragging: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    dragHandle: @Composable () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val countText = quantityString(R.plurals.card_count, item.cardCount, item.cardCount)
    val moveUpLabel = stringResource(R.string.action_move_up)
    val moveDownLabel = stringResource(R.string.action_move_down)

    ElevatedCard(
        onClick = onClick,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (isDragging) 8.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                // Alternativa accesible al arrastre para TalkBack.
                customActions = listOfNotNull(
                    onMoveUp?.let { CustomAccessibilityAction(moveUpLabel) { it(); true } },
                    onMoveDown?.let { CustomAccessibilityAction(moveDownLabel) { it(); true } },
                )
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        ) {
            dragHandle()
            Icon(
                Icons.Outlined.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 12.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    item.category.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    countText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.cd_more_options))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_rename)) },
                        leadingIcon = { Icon(Icons.Outlined.DriveFileRenameOutline, null) },
                        onClick = { menuOpen = false; onRename() },
                    )
                    if (onMoveUp != null) {
                        DropdownMenuItem(
                            text = { Text(moveUpLabel) },
                            leadingIcon = { Icon(Icons.Outlined.KeyboardArrowUp, null) },
                            onClick = { menuOpen = false; onMoveUp() },
                        )
                    }
                    if (onMoveDown != null) {
                        DropdownMenuItem(
                            text = { Text(moveDownLabel) },
                            leadingIcon = { Icon(Icons.Outlined.KeyboardArrowDown, null) },
                            onClick = { menuOpen = false; onMoveDown() },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        }
    }
}
