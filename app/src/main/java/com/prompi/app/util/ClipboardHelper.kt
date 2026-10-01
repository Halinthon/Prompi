package com.prompi.app.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.prompi.app.R

object ClipboardHelper {
    /**
     * Copia texto al portapapeles del sistema. No requiere permisos.
     * @return false si el sistema no permitió copiar (caso muy poco frecuente).
     */
    fun copy(context: Context, text: String): Boolean = try {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard?.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.app_name), text))
        clipboard != null
    } catch (e: Exception) {
        false
    }
}
