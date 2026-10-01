package com.prompi.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

/**
 * Índice de texto completo sobre título y contenido de las fichas.
 *
 * - Es una tabla "de contenido externo": no duplica el texto, y Room crea triggers que la
 *   mantienen sincronizada automáticamente con la tabla "cards".
 * - El tokenizador unicode61 con remove_diacritics=2 ignora mayúsculas y tildes
 *   ("programacion" encuentra "Programación"). Requiere SQLite 3.27+, disponible desde Android 11.
 */
@Fts4(
    contentEntity = CardEntity::class,
    tokenizer = FtsOptions.TOKENIZER_UNICODE61,
    tokenizerArgs = ["remove_diacritics=2"],
)
@Entity(tableName = "cards_fts")
data class CardFtsEntity(
    val title: String,
    @ColumnInfo(name = "body") val content: String,
)
