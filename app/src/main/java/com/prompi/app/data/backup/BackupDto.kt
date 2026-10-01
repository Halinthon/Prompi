package com.prompi.app.data.backup

import kotlinx.serialization.Serializable

/*
 * Formato JSON de las copias de seguridad (formatVersion = 1).
 * Las fichas van anidadas dentro de su categoría, por lo que el archivo no depende
 * de los ID internos de la base de datos y puede leerse o editarse a mano.
 * Los valores por defecto permiten leer archivos a los que les falte algún campo.
 */

@Serializable
data class BackupFileDto(
    val app: String,
    val formatVersion: Int,
    val exportedAt: String = "",
    val categories: List<BackupCategoryDto> = emptyList(),
)

@Serializable
data class BackupCategoryDto(
    val name: String = "",
    val position: Int = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val cards: List<BackupCardDto> = emptyList(),
)

@Serializable
data class BackupCardDto(
    val title: String = "",
    val content: String = "",
    val rating: Int = 1,
    val isFavorite: Boolean = false,
    val color: String = "GRAY",
    val position: Int = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
