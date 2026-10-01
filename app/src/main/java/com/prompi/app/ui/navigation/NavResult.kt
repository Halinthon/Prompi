package com.prompi.app.ui.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prompi.app.ui.components.NavResultEffect

/** Muestra en un Snackbar el mensaje devuelto por la pantalla anterior (p. ej. el editor). */
@Composable
fun NavResultMessage(resultHandle: SavedStateHandle, snackbarHostState: SnackbarHostState) {
    val flow = remember(resultHandle) { resultHandle.getStateFlow<Int?>(NAV_RESULT_MESSAGE, null) }
    val messageRes by flow.collectAsStateWithLifecycle()
    NavResultEffect(messageRes, snackbarHostState) { resultHandle[NAV_RESULT_MESSAGE] = null }
}
