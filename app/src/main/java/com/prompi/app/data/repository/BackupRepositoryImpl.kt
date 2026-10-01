package com.prompi.app.data.repository

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import androidx.room.withTransaction
import com.prompi.app.data.backup.BackupCardDto
import com.prompi.app.data.backup.BackupCategoryDto
import com.prompi.app.data.backup.BackupCodec
import com.prompi.app.data.backup.BackupFileDto
import com.prompi.app.data.local.PrompiDatabase
import com.prompi.app.data.local.entity.CardEntity
import com.prompi.app.data.local.entity.CategoryEntity
import com.prompi.app.domain.model.BackupException
import com.prompi.app.domain.model.ImportMode
import com.prompi.app.domain.model.ImportResult
import com.prompi.app.domain.model.ParsedBackup
import com.prompi.app.domain.repository.BackupRepository
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Exportación e importación mediante el Storage Access Framework: el usuario elige el
 * archivo con el selector estándar de Android, por lo que no se necesita ningún permiso.
 */
class BackupRepositoryImpl(
    private val db: PrompiDatabase,
    private val contentResolver: ContentResolver,
) : BackupRepository {

    private val categoryDao = db.categoryDao()
    private val cardDao = db.cardDao()

    override suspend fun export(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val categories = categoryDao.getAll()
            val cardsByCategory = cardDao.getAll().groupBy { it.categoryId }
            val file = BackupFileDto(
                app = BackupCodec.APP_ID,
                formatVersion = BackupCodec.CURRENT_FORMAT_VERSION,
                exportedAt = Instant.now().toString(),
                categories = categories.map { category ->
                    BackupCategoryDto(
                        name = category.name,
                        position = category.position,
                        createdAt = category.createdAt,
                        updatedAt = category.updatedAt,
                        cards = cardsByCategory[category.id].orEmpty()
                            .sortedBy { it.position }
                            .map { card ->
                                BackupCardDto(
                                    title = card.title,
                                    content = card.content,
                                    rating = card.rating,
                                    isFavorite = card.isFavorite,
                                    color = card.color,
                                    position = card.position,
                                    createdAt = card.createdAt,
                                    updatedAt = card.updatedAt,
                                )
                            },
                    )
                },
            )
            val bytes = BackupCodec.encode(file).toByteArray(Charsets.UTF_8)
            openOutput(uri).use { it.write(bytes) }
            Result.success(file.categories.sumOf { it.cards.size })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            deletePartialFile(uri)
            Result.failure(BackupException.WriteError(e))
        } catch (e: OutOfMemoryError) {
            deletePartialFile(uri)
            Result.failure(BackupException.WriteError())
        }
    }

    override suspend fun read(uri: Uri): Result<ParsedBackup> = withContext(Dispatchers.IO) {
        try {
            val bytes = contentResolver.openInputStream(uri)?.use { readLimited(it) }
                ?: throw BackupException.ReadError()
            if (bytes.size > BackupCodec.MAX_FILE_BYTES) throw BackupException.TooLarge()
            Result.success(BackupCodec.decode(bytes.toString(Charsets.UTF_8)))
        } catch (e: CancellationException) {
            throw e
        } catch (e: BackupException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(BackupException.ReadError(e))
        } catch (e: OutOfMemoryError) {
            // Un archivo grande puede no caber en la memoria de dispositivos modestos.
            Result.failure(BackupException.TooLarge())
        }
    }

    override suspend fun import(backup: ParsedBackup, mode: ImportMode): ImportResult =
        db.withTransaction {
            if (mode == ImportMode.REPLACE) {
                cardDao.deleteAll()
                categoryDao.deleteAll()
            }

            val existing = categoryDao.getAll()
            val byName = existing.associateBy { it.name.trim().lowercase() }.toMutableMap()
            var nextCategoryPosition = (existing.maxOfOrNull { it.position } ?: -1) + 1

            var created = 0
            var merged = 0
            var imported = 0
            var duplicates = 0

            for (category in backup.categories) {
                val key = category.name.trim().lowercase()
                val target = byName[key]
                val categoryId: Long
                val knownCards: MutableSet<Pair<String, String>>
                var nextCardPosition: Int

                if (target != null) {
                    // Categoría con el mismo nombre: se fusiona.
                    categoryId = target.id
                    merged++
                    val cards = cardDao.getByCategory(categoryId)
                    knownCards = cards.map { it.title to it.content }.toMutableSet()
                    nextCardPosition = (cards.maxOfOrNull { it.position } ?: -1) + 1
                } else {
                    val entity = CategoryEntity(
                        name = category.name,
                        position = nextCategoryPosition++,
                        createdAt = category.createdAt,
                        updatedAt = category.updatedAt,
                    )
                    categoryId = categoryDao.insert(entity)
                    byName[key] = entity.copy(id = categoryId)
                    created++
                    knownCards = mutableSetOf()
                    nextCardPosition = 0
                }

                for (card in category.cards) {
                    // Una ficha con el mismo título y contenido en la misma categoría es un duplicado.
                    if (!knownCards.add(card.title to card.content)) {
                        duplicates++
                        continue
                    }
                    cardDao.insert(
                        CardEntity(
                            categoryId = categoryId,
                            title = card.title,
                            content = card.content,
                            rating = card.rating,
                            isFavorite = card.isFavorite,
                            color = card.color.name,
                            position = nextCardPosition++,
                            createdAt = card.createdAt,
                            updatedAt = card.updatedAt,
                        ),
                    )
                    imported++
                }
            }

            ImportResult(
                categoriesCreated = created,
                categoriesMerged = merged,
                cardsImported = imported,
                duplicatesSkipped = duplicates,
                valuesAdjusted = backup.adjustedValues,
            )
        }

    /** Si la exportación falla a medias, elimina el archivo incompleto que creó el selector. */
    private fun deletePartialFile(uri: Uri) {
        try {
            DocumentsContract.deleteDocument(contentResolver, uri)
        } catch (e: Exception) {
            // El proveedor puede no permitir borrar: el archivo se queda, pero no es crítico.
        }
    }

    /** Algunos proveedores no admiten el modo "wt" (truncar); en ese caso se usa "w". */
    private fun openOutput(uri: Uri): OutputStream {
        val truncating: OutputStream? = try {
            contentResolver.openOutputStream(uri, "wt")
        } catch (e: FileNotFoundException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
        return truncating
            ?: contentResolver.openOutputStream(uri, "w")
            ?: throw BackupException.WriteError()
    }

    /** Lee como máximo MAX_FILE_BYTES + 1 bytes para detectar archivos demasiado grandes. */
    private fun readLimited(input: InputStream): ByteArray {
        val limit = BackupCodec.MAX_FILE_BYTES + 1
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        val output = ByteArrayOutputStream()
        while (output.size() < limit) {
            val read = input.read(buffer)
            if (read == -1) break
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }
}
