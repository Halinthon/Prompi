package com.prompi.app.domain.model

/** Contenido de una copia de seguridad ya validado y saneado, listo para importar. */
data class ParsedBackup(
    val categories: List<BackupCategoryData>,
    /** Número de valores que hubo que corregir (títulos largos, estrellas fuera de rango…). */
    val adjustedValues: Int,
) {
    val cardCount: Int get() = categories.sumOf { it.cards.size }
}

data class BackupCategoryData(
    val name: String,
    val position: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val cards: List<BackupCardData>,
)

data class BackupCardData(
    val title: String,
    val content: String,
    val rating: Int,
    val isFavorite: Boolean,
    val color: CardColor,
    val position: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

enum class ImportMode {
    /** Añade los datos del archivo a los existentes, omitiendo duplicados. */
    MERGE,

    /** Borra todos los datos actuales y los sustituye por los del archivo. */
    REPLACE,
}

data class ImportResult(
    val categoriesCreated: Int,
    val categoriesMerged: Int,
    val cardsImported: Int,
    val duplicatesSkipped: Int,
    val valuesAdjusted: Int,
)

/** Errores tipados de exportación/importación; la interfaz los traduce a mensajes. */
sealed class BackupException(cause: Throwable? = null) : Exception(cause) {
    class InvalidFile(cause: Throwable? = null) : BackupException(cause)
    class NotPrompiFile : BackupException()
    class UnsupportedVersion(val version: Int) : BackupException()
    class TooLarge : BackupException()
    class ReadError(cause: Throwable? = null) : BackupException(cause)
    class WriteError(cause: Throwable? = null) : BackupException(cause)
}
