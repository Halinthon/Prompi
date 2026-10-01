package com.prompi.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val LightColors = lightColorScheme(
    primary = Indigo40,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = Indigo90,
    onPrimaryContainer = Indigo10,
    secondary = Slate40,
    secondaryContainer = Slate90,
    onSecondaryContainer = Slate20,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightBackground,
    onSurface = LightOnBackground,
)

private val DarkColors = darkColorScheme(
    primary = Indigo80,
    onPrimary = Indigo20,
    primaryContainer = Indigo30,
    onPrimaryContainer = Indigo90,
    secondary = Slate80,
    secondaryContainer = Slate30,
    onSecondaryContainer = Slate90,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkBackground,
    onSurface = DarkOnBackground,
)

/** Indica a los componentes si el tema activo es oscuro (lo decide el usuario, no solo el sistema). */
val LocalDarkTheme = staticCompositionLocalOf { false }

/**
 * Tema de Prompi. El color dinámico de Android 12+ está desactivado a propósito para que
 * la identidad visual y los colores de las fichas sean iguales en todos los dispositivos.
 */
@Composable
fun PrompiTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            content = content,
        )
    }
}
