package com.prompi.app.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prompi.app.R
import com.prompi.app.domain.model.Category
import com.prompi.app.domain.model.Limits
import com.prompi.app.ui.common.AppViewModelProvider
import com.prompi.app.ui.components.ConfirmDialog
import com.prompi.app.ui.components.CounterRow
import com.prompi.app.ui.components.LoadingBox
import com.prompi.app.ui.components.MaxContentWidth
import com.prompi.app.ui.components.RatingBar
import com.prompi.app.ui.components.VerticalSpace

/** @param onClose `true` si la ficha se guardó (para mostrar "Ficha guardada" al volver). */
@Composable
fun EditorScreen(
    onClose: (saved: Boolean) -> Unit,
    viewModel: EditorViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val currentOnClose by rememberUpdatedState(onClose)
    var showDiscard by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                EditorEvent.Saved -> currentOnClose(true)
                EditorEvent.NotFound -> currentOnClose(false)
                is EditorEvent.Error -> snackbarHostState.showSnackbar(context.getString(event.messageRes))
            }
        }
    }

    // Si la categoría elegida se elimina mientras se edita, se usa la primera disponible.
    LaunchedEffect(categories, viewModel.categoryId) {
        if (categories.isNotEmpty() && categories.none { it.id == viewModel.categoryId }) {
            viewModel.updateCategory(categories.first().id)
        }
    }

    fun requestClose() {
        if (viewModel.hasChanges && !viewModel.isSaving) showDiscard = true else onClose(false)
    }

    BackHandler(enabled = viewModel.hasChanges && !viewModel.isSaving) { showDiscard = true }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = ::requestClose) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_close))
                    }
                },
                title = {
                    Text(stringResource(if (viewModel.isNew) R.string.editor_title_new else R.string.editor_title_edit))
                },
                actions = {
                    if (viewModel.isSaving) {
                        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    } else {
                        TextButton(onClick = viewModel::save, enabled = !viewModel.isLoading) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                            Text(stringResource(R.string.action_save), Modifier.padding(start = 6.dp))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (viewModel.isLoading) {
            LoadingBox(Modifier.padding(padding))
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .imePadding(),
                contentAlignment = Alignment.TopCenter,
            ) {
                EditorForm(viewModel, categories, Modifier.widthIn(max = MaxContentWidth))
            }
        }
    }

    if (showDiscard) {
        ConfirmDialog(
            title = stringResource(R.string.dialog_discard_title),
            text = stringResource(R.string.dialog_discard_text),
            confirmLabel = stringResource(R.string.action_discard),
            destructive = true,
            onConfirm = {
                showDiscard = false
                onClose(false)
            },
            onDismiss = { showDiscard = false },
        )
    }
}

@Composable
private fun EditorForm(viewModel: EditorViewModel, categories: List<Category>, modifier: Modifier = Modifier) {
    val titleError = viewModel.showTitleError && !viewModel.isTitleValid

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        OutlinedTextField(
            value = viewModel.title,
            onValueChange = viewModel::updateTitle,
            label = { Text(stringResource(R.string.field_title)) },
            singleLine = true,
            isError = titleError,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next,
            ),
            supportingText = {
                CounterRow(
                    count = viewModel.title.length,
                    max = Limits.TITLE_MAX,
                    error = if (titleError) stringResource(R.string.error_title_required) else null,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )

        VerticalSpace(4.dp)
        CategorySelector(
            categories = categories,
            selectedId = viewModel.categoryId,
            onSelect = viewModel::updateCategory,
        )

        VerticalSpace(16.dp)
        Text(stringResource(R.string.field_rating), style = MaterialTheme.typography.labelLarge)
        RatingBar(
            rating = viewModel.rating,
            onRatingChange = viewModel::updateRating,
            starSize = 32.dp,
            touchSize = 48.dp,
        )

        VerticalSpace(16.dp)
        OutlinedTextField(
            value = viewModel.content,
            onValueChange = viewModel::updateContent,
            label = { Text(stringResource(R.string.field_content)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            supportingText = { CounterRow(count = viewModel.content.length, max = Limits.CONTENT_MAX) },
            minLines = 8,
            modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
        )
        VerticalSpace(24.dp)
    }
}

@Composable
private fun CategorySelector(categories: List<Category>, selectedId: Long, onSelect: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = categories.firstOrNull { it.id == selectedId }?.name.orEmpty()

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it && categories.size > 1 }) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(R.string.field_category)) },
            leadingIcon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onSelect(category.id)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}
