package com.prompi.app.ui.categories

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import com.prompi.app.R
import com.prompi.app.domain.model.CategoryWithCount
import com.prompi.app.domain.model.Limits
import com.prompi.app.domain.model.takeSafe
import com.prompi.app.ui.components.CounterRow
import com.prompi.app.ui.components.RadioRow
import com.prompi.app.ui.components.ScrollableDialogContent
import com.prompi.app.ui.components.VerticalSpace
import com.prompi.app.ui.components.quantityString
import androidx.compose.ui.unit.dp

/** Diálogo para crear o renombrar una categoría. */
@Composable
fun CategoryNameDialog(
    title: String,
    confirmLabel: String,
    initialName: String,
    validate: (String) -> Int?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialName, selection = TextRange(initialName.length)))
    }
    var showError by rememberSaveable { mutableStateOf(false) }
    val errorRes = validate(value.text)
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    fun submit() {
        if (errorRes == null) onConfirm(value.text.trim()) else showError = true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = {
                        // Al pegar un texto largo se recorta al límite en vez de ignorarlo.
                        val clean = it.text.replace('\n', ' ').takeSafe(Limits.CATEGORY_NAME_MAX)
                        value = if (clean == it.text) {
                            it
                        } else {
                            TextFieldValue(clean, TextRange(minOf(it.selection.end, clean.length)))
                        }
                        showError = false
                    },
                    label = { Text(stringResource(R.string.field_category_name)) },
                    singleLine = true,
                    isError = showError && errorRes != null,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    supportingText = {
                        CounterRow(
                            count = value.text.length,
                            max = Limits.CATEGORY_NAME_MAX,
                            error = if (showError && errorRes != null) stringResource(errorRes) else null,
                        )
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                )
            }
        },
        confirmButton = { TextButton(onClick = ::submit) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/**
 * Confirmación para eliminar una categoría. Si tiene fichas, permite moverlas a otra
 * categoría (por defecto la primera disponible) o eliminarlas todas.
 */
@Composable
fun DeleteCategoryDialog(
    target: CategoryWithCount,
    others: List<CategoryWithCount>,
    onConfirm: (moveCardsTo: Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    val hasCards = target.cardCount > 0
    // -1 = eliminar las fichas; cualquier otro valor = ID de la categoría destino.
    var selection by rememberSaveable { mutableStateOf(others.firstOrNull()?.category?.id ?: DELETE_CARDS) }
    if (selection != DELETE_CARDS && others.none { it.category.id == selection }) {
        selection = others.firstOrNull()?.category?.id ?: DELETE_CARDS
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_delete_category_title)) },
        text = {
            ScrollableDialogContent {
                if (!hasCards) {
                    Text(stringResource(R.string.dialog_delete_category_empty, target.category.name))
                } else {
                    Text(
                        quantityString(
                            R.plurals.dialog_delete_category_has_cards,
                            target.cardCount,
                            target.category.name,
                            target.cardCount,
                        ),
                    )
                    VerticalSpace(12.dp)
                    if (others.isNotEmpty()) {
                        Text(
                            stringResource(R.string.dialog_delete_category_move_to),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        others.forEach { other ->
                            RadioRow(
                                selected = selection == other.category.id,
                                text = other.category.name,
                                onClick = { selection = other.category.id },
                            )
                        }
                    }
                    RadioRow(
                        selected = selection == DELETE_CARDS,
                        text = stringResource(R.string.dialog_delete_category_delete_all),
                        onClick = { selection = DELETE_CARDS },
                        destructive = true,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(if (!hasCards || selection == DELETE_CARDS) null else selection) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(stringResource(R.string.action_delete)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

private const val DELETE_CARDS = -1L
