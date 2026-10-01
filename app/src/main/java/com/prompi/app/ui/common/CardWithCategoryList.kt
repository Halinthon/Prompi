package com.prompi.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prompi.app.domain.model.CardWithCategory
import com.prompi.app.ui.components.MaxContentWidth
import com.prompi.app.ui.components.PromptCard

/** Lista (sin reordenar) de fichas con el nombre de su categoría: Favoritos y Búsqueda. */
@Composable
fun CardWithCategoryList(
    items: List<CardWithCategory>,
    expandedIds: Set<Long>,
    actions: CardActions,
    contentPadding: PaddingValues,
    header: (LazyListScope.() -> Unit)? = null,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.widthIn(max = MaxContentWidth).fillMaxSize(),
        ) {
            header?.invoke(this)
            items(items, key = { it.card.id }) { item ->
                PromptCard(
                    card = item.card,
                    expanded = item.card.id in expandedIds,
                    actions = actions,
                    categoryName = item.categoryName,
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}
