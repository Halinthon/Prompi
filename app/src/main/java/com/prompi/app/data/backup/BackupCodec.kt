package com.prompi.app.data.backup

import com.prompi.app.domain.model.BackupCardData
import com.prompi.app.domain.model.BackupCategoryData
import com.prompi.app.domain.model.BackupException
import com.prompi.app.domain.model.CardColor
import com.prompi.app.domain.model.Limits
import com.prompi.app.domain.model.ParsedBackup
import com.prompi.app.domain.model.takeSafe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Codificación y validación de copias de seguridad. Es Kotlin puro (sin Android),
 * por lo que se prueba con pruebas unitarias normales.
 */
object BackupCodec {

    const val APP_ID = "Prompi"
    const val CURRENT_FORMAT_VERSION = 1
    const val MAX_FILE_BYTES = 20 * 1024 * 1024

    const val DEFAULT_CATEGORY_NAME = "Sin nombre"
    const val DEFAULT_CARD_TITLE = "Sin título"

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(file: BackupFileDto): String = json.encodeToString(BackupFileDto.serializer(), file)

    /**
     * Lee, valida y sanea el texto de una copia de seguridad.
     * @throws BackupException si el archivo no es válido o es de una versión no soportada.
     */
    fun decode(text: String, now: Long = System.currentTimeMillis()): ParsedBackup {
        val root: JsonObject = try {
            json.parseToJsonElement(text.removePrefix("\uFEFF")).jsonObject
        } catch (e: Exception) {
            throw BackupException.InvalidFile(e)
        }

        val app = runCatching { root["app"]?.jsonPrimitive?.contentOrNull }.getOrNull()
        if (app != APP_ID) throw BackupException.NotPrompiFile()

        val version = runCatching { root["formatVersion"]?.jsonPrimitive?.intOrNull }.getOrNull()
            ?: throw BackupException.InvalidFile()
        if (version > CURRENT_FORMAT_VERSION) throw BackupException.UnsupportedVersion(version)
        if (version < 1) throw BackupException.InvalidFile()

        val dto = try {
            json.decodeFromJsonElement(BackupFileDto.serializer(), root)
        } catch (e: Exception) {
            throw BackupException.InvalidFile(e)
        }
        return sanitize(dto, now)
    }

    /** Corrige valores fuera de rango en lugar de rechazar el archivo completo. */
    private fun sanitize(file: BackupFileDto, now: Long): ParsedBackup {
        var adjusted = 0

        fun timestamp(value: Long): Long = if (value > 0) value else now

        val categories = file.categories
            .sortedBy { it.position }
            .mapIndexed { categoryIndex, category ->
                val trimmedName = category.name.trim()
                val name = when {
                    trimmedName.isEmpty() -> { adjusted++; DEFAULT_CATEGORY_NAME }
                    trimmedName.length > Limits.CATEGORY_NAME_MAX -> {
                        adjusted++; trimmedName.takeSafe(Limits.CATEGORY_NAME_MAX)
                    }
                    else -> trimmedName
                }

                val cards = category.cards
                    .sortedBy { it.position }
                    .mapIndexed { cardIndex, card ->
                        val trimmedTitle = card.title.replace('\n', ' ').trim()
                        val title = when {
                            trimmedTitle.isEmpty() -> { adjusted++; DEFAULT_CARD_TITLE }
                            trimmedTitle.length > Limits.TITLE_MAX -> {
                                adjusted++; trimmedTitle.takeSafe(Limits.TITLE_MAX)
                            }
                            else -> trimmedTitle
                        }
                        val content = if (card.content.length > Limits.CONTENT_MAX) {
                            adjusted++; card.content.takeSafe(Limits.CONTENT_MAX)
                        } else {
                            card.content
                        }
                        val rating = card.rating.coerceIn(Limits.RATING_MIN, Limits.RATING_MAX)
                        if (rating != card.rating) adjusted++
                        if (!CardColor.isValidKey(card.color)) adjusted++

                        BackupCardData(
                            title = title,
                            content = content,
                            rating = rating,
                            isFavorite = card.isFavorite,
                            color = CardColor.fromKey(card.color),
                            position = cardIndex,
                            createdAt = timestamp(card.createdAt),
                            updatedAt = timestamp(card.updatedAt),
                        )
                    }

                BackupCategoryData(
                    name = name,
                    position = categoryIndex,
                    createdAt = timestamp(category.createdAt),
                    updatedAt = timestamp(category.updatedAt),
                    cards = cards,
                )
            }

        return ParsedBackup(categories = categories, adjustedValues = adjusted)
    }
}
