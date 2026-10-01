package com.prompi.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prompi.app.R
import com.prompi.app.domain.model.Card
import com.prompi.app.ui.common.CardActions
import com.prompi.app.ui.theme.FavoriteDark
import com.prompi.app.ui.theme.FavoriteLight
import com.prompi.app.ui.theme.LocalDarkTheme
import com.prompi.app.ui.theme.tone
import androidx.compose.material3.Card as MaterialCard

private const val COLLAPSED_TITLE_LINES = 2
private const val COLLAPSED_CONTENT_LINES = 4

/**
 * Ficha de prompt. Un toque la expande o la minimiza; mantenerla pulsada abre el selector
 * de color. El botón de copiar, siempre visible a la derecha, copia solo el contenido.
 *
 * @param categoryName si no es nulo, se muestra la categoría (Favoritos y Búsqueda).
 * @param dragHandle asa de arrastre; solo se pasa en la lista reordenable de una categoría.
 */
@Composable
fun PromptCard(
    card: Card,
    expanded: Boolean,
    actions: CardActions,
    modifier: Modifier = Modifier,
    categoryName: String? = null,
    isDragging: Boolean = false,
    dragHandle: (@Composable () -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
) {
    val tone = card.color.tone()
    val expandLabel = stringResource(if (expanded) R.string.cd_collapse else R.string.cd_expand)
    val colorLabel = stringResource(R.string.action_change_color)
    val stateLabel = stringResource(if (expanded) R.string.state_expanded else R.string.state_collapsed)
    val copyLabel = stringResource(R.string.action_copy)
    val favoriteLabel = stringResource(
        if (card.isFavorite) R.string.action_remove_favorite else R.string.action_add_favorite,
    )
    val moveUpLabel = stringResource(R.string.action_move_up)
    val moveDownLabel = stringResource(R.string.action_move_down)

    MaterialCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = tone.container, contentColor = tone.content),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 0.dp),
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(
                    onClickLabel = expandLabel,
                    onLongClickLabel = colorLabel,
                    onLongClick = { actions.onChangeColor(card) },
                    onClick = { actions.onToggleExpand(card) },
                )
                .semantics {
                    stateDescription = stateLabel
                    // Acciones rápidas de TalkBack, incluida la alternativa accesible al arrastre.
                    customActions = listOfNotNull(
                        CustomAccessibilityAction(copyLabel) { actions.onCopy(card); true },
                        CustomAccessibilityAction(favoriteLabel) { actions.onToggleFavorite(card); true },
                        onMoveUp?.let { CustomAccessibilityAction(moveUpLabel) { it(); true } },
                        onMoveDown?.let { CustomAccessibilityAction(moveDownLabel) { it(); true } },
                    )
                }
                .animateContentSize(animationSpec = tween(durationMillis = 200))
                .padding(
                    start = if (dragHandle != null) 0.dp else 16.dp,
                    top = 8.dp,
                    end = 4.dp,
                    bottom = 4.dp,
                ),
        ) {
            if (dragHandle != null) dragHandle()

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 6.dp),
            ) {
                if (categoryName != null) {
                    CategoryLabel(categoryName)
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = if (expanded) Int.MAX_VALUE else COLLAPSED_TITLE_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
                if (card.content.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = card.content,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = if (expanded) Int.MAX_VALUE else COLLAPSED_CONTENT_LINES,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                RatingBar(
                    rating = card.rating,
                    onRatingChange = { actions.onRatingChange(card, it) },
                    starSize = 18.dp,
                    touchSize = 48.dp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FilledTonalIconButton(onClick = { actions.onCopy(card) }) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = stringResource(R.string.action_copy))
                }
                IconButton(onClick = { actions.onToggleFavorite(card) }) {
                    Icon(
                        imageVector = if (card.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = stringResource(
                            if (card.isFavorite) R.string.action_remove_favorite else R.string.action_add_favorite,
                        ),
                        tint = if (card.isFavorite) {
                            if (LocalDarkTheme.current) FavoriteDark else FavoriteLight
                        } else {
                            tone.content.copy(alpha = 0.7f)
                        },
                    )
                }
                CardMenu(card = card, actions = actions, onMoveUp = onMoveUp, onMoveDown = onMoveDown)
            }
        }
    }
}

@Composable
private fun CardMenu(
    card: Card,
    actions: CardActions,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.cd_more_options))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_edit)) },
                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                onClick = { open = false; actions.onEdit(card) },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_share)) },
                leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                onClick = { open = false; actions.onShare(card) },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_change_color)) },
                leadingIcon = { Icon(Icons.Outlined.Palette, contentDescription = null) },
                onClick = { open = false; actions.onChangeColor(card) },
            )
            if (onMoveUp != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_move_up)) },
                    leadingIcon = { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null) },
                    onClick = { open = false; onMoveUp() },
                )
            }
            if (onMoveDown != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_move_down)) },
                    leadingIcon = { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null) },
                    onClick = { open = false; onMoveDown() },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                leadingIcon = {
                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                },
                onClick = { open = false; actions.onDelete(card) },
            )
        }
    }
}
