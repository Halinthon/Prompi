package com.prompi.app.ui.theme

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.prompi.app.R
import com.prompi.app.domain.model.CardColor

/** Par de colores (fondo y texto) de una ficha. Contraste de texto ≥ 4.5:1 (WCAG AA). */
data class CardTone(val container: Color, val content: Color)

private val LightText = Color(0xFF1C1B1F)
private val DarkText = Color(0xFFE6E1E5)

private val lightTones = mapOf(
    CardColor.GRAY to CardTone(Color(0xFFF1F1F4), LightText),
    CardColor.BLUE to CardTone(Color(0xFFDCEBFA), LightText),
    CardColor.PINK to CardTone(Color(0xFFFBE1EA), LightText),
    CardColor.YELLOW to CardTone(Color(0xFFFBF1CC), LightText),
    CardColor.GREEN to CardTone(Color(0xFFDDF2E2), LightText),
)

private val darkTones = mapOf(
    CardColor.GRAY to CardTone(Color(0xFF2C2D31), DarkText),
    CardColor.BLUE to CardTone(Color(0xFF1F3448), DarkText),
    CardColor.PINK to CardTone(Color(0xFF45283A), DarkText),
    CardColor.YELLOW to CardTone(Color(0xFF433B1E), DarkText),
    CardColor.GREEN to CardTone(Color(0xFF213A2A), DarkText),
)

/** Devuelve los colores de la ficha adaptados al tema claro u oscuro activo. */
@Composable
@ReadOnlyComposable
fun CardColor.tone(): CardTone {
    val tones = if (LocalDarkTheme.current) darkTones else lightTones
    return tones.getValue(this)
}

@get:StringRes
val CardColor.labelRes: Int
    get() = when (this) {
        CardColor.GRAY -> R.string.color_gray
        CardColor.BLUE -> R.string.color_blue
        CardColor.PINK -> R.string.color_pink
        CardColor.YELLOW -> R.string.color_yellow
        CardColor.GREEN -> R.string.color_green
    }
