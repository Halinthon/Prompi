package com.prompi.app.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import com.prompi.app.R

object ShareHelper {
    /**
     * Abre el diálogo estándar de compartir de Android con el título y el contenido de la ficha.
     * @return false si no hay ninguna aplicación capaz de recibir el texto.
     */
    fun share(context: Context, title: String, content: String): Boolean {
        val text = if (content.isBlank()) title else "$title\n\n$content"
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        return try {
            context.startActivity(Intent.createChooser(send, context.getString(R.string.share_chooser_title)))
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}
