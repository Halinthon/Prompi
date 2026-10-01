package com.prompi.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prompi.app.R
import com.prompi.app.domain.model.Limits
import com.prompi.app.ui.theme.LocalDarkTheme
import com.prompi.app.ui.theme.StarDark
import com.prompi.app.ui.theme.StarLight

/**
 * Calificación de 1 a 5 estrellas. Si [onRatingChange] es nulo, es solo de lectura.
 * Cada estrella se anuncia en TalkBack como "Calificar con N estrellas".
 */
@Composable
fun RatingBar(
    rating: Int,
    onRatingChange: ((Int) -> Unit)?,
    modifier: Modifier = Modifier,
    starSize: Dp = 20.dp,
    touchSize: Dp = 48.dp,
) {
    val filledColor = if (LocalDarkTheme.current) StarDark else StarLight
    Row(modifier = modifier.selectableGroup(), verticalAlignment = Alignment.CenterVertically) {
        for (value in Limits.RATING_MIN..Limits.RATING_MAX) {
            val filled = value <= rating
            val description = stringResource(R.string.cd_rate_n, value)
            val interaction = if (onRatingChange != null) {
                Modifier.selectable(
                    selected = value == rating,
                    role = Role.RadioButton,
                    onClick = { onRatingChange(value) },
                )
            } else {
                Modifier
            }
            Box(
                modifier = Modifier
                    .size(touchSize)
                    .then(interaction)
                    .semantics { contentDescription = description },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (filled) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = null,
                    tint = if (filled) filledColor else LocalContentColor.current.copy(alpha = 0.55f),
                    modifier = Modifier.size(starSize),
                )
            }
        }
    }
}
