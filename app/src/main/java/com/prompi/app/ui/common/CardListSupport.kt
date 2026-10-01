package com.prompi.app.ui.common

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.prompi.app.R
import com.prompi.app.domain.model.Card
import com.prompi.app.ui.components.ColorPickerSheet
import com.prompi.app.ui.components.ConfirmDialog
import com.prompi.app.util.ClipboardHelper
import com.prompi.app.util.ShareHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Acciones disponibles sobre una ficha, compartidas por todas las listas. */
@Immutable
data class CardActions(
    val onToggleExpand: (Card) -> Unit,
    val onCopy: (Card) -> Unit,
    val onShare: (Card) -> Unit,
    val onToggleFavorite: (Card) -> Unit,
    val onRatingChange: (Card, Int) -> Unit,
    val onEdit: (Card) -> Unit,
    val onChangeColor: (Card) -> Unit,
    val onDelete: (Card) -> Unit,
)

/**
 * Diálogos abiertos (selector de color y confirmación de borrado). Se guardan solo los ID,
 * con rememberSaveable, para que el diálogo siga abierto tras rotar la pantalla.
 */
@Stable
class CardDialogState(colorState: MutableState<Long?>, deleteState: MutableState<Long?>) {
    var colorTargetId: Long? by colorState
    var deleteTargetId: Long? by deleteState
}

@Composable
fun rememberCardDialogState(): CardDialogState {
    val colorState = rememberSaveable { mutableStateOf<Long?>(null) }
    val deleteState = rememberSaveable { mutableStateOf<Long?>(null) }
    return remember(colorState, deleteState) { CardDialogState(colorState, deleteState) }
}

@Composable
fun rememberCardActions(
    viewModel: CardListViewModel,
    dialogState: CardDialogState,
    snackbarHostState: SnackbarHostState,
    onEdit: (Card) -> Unit,
): CardActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnEdit by rememberUpdatedState(onEdit)

    return remember(viewModel, dialogState, snackbarHostState, context, scope) {
        fun showMessage(messageRes: Int) {
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(context.getString(messageRes), duration = SnackbarDuration.Short)
            }
        }

        CardActions(
            onToggleExpand = { viewModel.toggleExpanded(it.id) },
            onCopy = { card ->
                if (card.content.isBlank()) {
                    showMessage(R.string.msg_nothing_to_copy)
                } else {
                    val copied = ClipboardHelper.copy(context, card.content)
                    showMessage(if (copied) R.string.msg_copied else R.string.error_generic)
                }
            },
            onShare = { card ->
                if (!ShareHelper.share(context, card.title, card.content)) showMessage(R.string.error_share)
            },
            onToggleFavorite = { viewModel.toggleFavorite(it) },
            onRatingChange = { card, rating -> viewModel.setRating(card, rating) },
            onEdit = { currentOnEdit(it) },
            onChangeColor = { dialogState.colorTargetId = it.id },
            onDelete = { dialogState.deleteTargetId = it.id },
        )
    }
}

/**
 * Efectos comunes: Snackbars (incluido "Deshacer"), selector de color y confirmación de borrado.
 * @param findCard busca una ficha visible por su ID.
 */
@Composable
fun CardListEffects(
    viewModel: CardListViewModel,
    dialogState: CardDialogState,
    snackbarHostState: SnackbarHostState,
    findCard: (Long) -> Card?,
) {
    val context = LocalContext.current

    LaunchedEffect(viewModel, snackbarHostState) {
        val effectScope: CoroutineScope = this
        viewModel.events.collect { event ->
            // Cada Snackbar se lanza aparte para no bloquear la recepción de nuevos eventos.
            snackbarHostState.currentSnackbarData?.dismiss()
            effectScope.launch {
                when (event) {
                    CardListEvent.CardDeleted -> {
                        val result = snackbarHostState.showSnackbar(
                            message = context.getString(R.string.msg_card_deleted),
                            actionLabel = context.getString(R.string.action_undo),
                            duration = SnackbarDuration.Short,
                        )
                        if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
                    }
                    is CardListEvent.Message -> snackbarHostState.showSnackbar(context.getString(event.messageRes))
                }
            }
        }
    }

    dialogState.colorTargetId?.let { id ->
        val card = findCard(id)
        if (card != null) {
            ColorPickerSheet(
                selected = card.color,
                onSelect = { color ->
                    viewModel.setColor(card, color)
                    dialogState.colorTargetId = null
                },
                onDismiss = { dialogState.colorTargetId = null },
            )
        }
    }

    dialogState.deleteTargetId?.let { id ->
        val card = findCard(id)
        if (card != null) {
            ConfirmDialog(
                title = stringResource(R.string.dialog_delete_card_title),
                text = stringResource(R.string.dialog_delete_card_text, card.title),
                confirmLabel = stringResource(R.string.action_delete),
                destructive = true,
                onConfirm = {
                    dialogState.deleteTargetId = null
                    viewModel.delete(card)
                },
                onDismiss = { dialogState.deleteTargetId = null },
            )
        }
    }
}
