package com.prompi.app.domain.search

import java.text.Normalizer

/**
 * Convierte el texto escrito por el usuario en una consulta segura para SQLite FTS4.
 *
 * - Elimina cualquier carácter que no sea letra o número, de modo que comillas, asteriscos,
 *   paréntesis o guiones nunca provoquen errores de sintaxis.
 * - Pasa todo a minúsculas: así "AND", "OR" o "NOT" nunca se interpretan como operadores.
 * - Añade "*" a cada término para buscar por prefijo ("corr" encuentra "correo").
 * - Los términos se combinan con Y lógico (todos deben aparecer).
 */
object SearchQueryBuilder {

    const val MAX_TERMS = 10

    data class FtsQuery(
        /** Consulta sobre título y contenido. */
        val all: String,
        /** Misma consulta restringida al título, usada para ordenar resultados. */
        val titleOnly: String,
    )

    fun build(raw: String): FtsQuery? {
        val normalized = Normalizer.normalize(raw, Normalizer.Form.NFC).lowercase()
        val terms = buildString {
            for (ch in normalized) append(if (ch.isLetterOrDigit()) ch else ' ')
        }
            .split(' ')
            .filter { it.isNotBlank() }
            .distinct()
            .take(MAX_TERMS)

        if (terms.isEmpty()) return null
        return FtsQuery(
            all = terms.joinToString(" ") { "$it*" },
            titleOnly = terms.joinToString(" ") { "title:$it*" },
        )
    }
}
