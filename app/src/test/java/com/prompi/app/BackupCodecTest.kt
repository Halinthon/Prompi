package com.prompi.app

import com.prompi.app.data.backup.BackupCardDto
import com.prompi.app.data.backup.BackupCategoryDto
import com.prompi.app.data.backup.BackupCodec
import com.prompi.app.data.backup.BackupFileDto
import com.prompi.app.domain.model.BackupException
import com.prompi.app.domain.model.CardColor
import com.prompi.app.domain.model.Limits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupCodecTest {

    private val now = 1_700_000_000_000L

    private fun sampleFile() = BackupFileDto(
        app = BackupCodec.APP_ID,
        formatVersion = BackupCodec.CURRENT_FORMAT_VERSION,
        exportedAt = "2026-10-01T10:00:00Z",
        categories = listOf(
            BackupCategoryDto(
                name = "Correos",
                position = 0,
                createdAt = 10,
                updatedAt = 20,
                cards = listOf(
                    BackupCardDto("Saludo", "Hola,\n\"¿qué tal?\" ✨", 4, true, "PINK", 0, 30, 40),
                    BackupCardDto("Despedida", "Un saludo", 1, false, "GRAY", 1, 50, 60),
                ),
            ),
            BackupCategoryDto(name = "Código", position = 1, createdAt = 70, updatedAt = 80),
        ),
    )

    private inline fun <reified T : BackupException> assertThrows(block: () -> Unit): T {
        try {
            block()
        } catch (e: BackupException) {
            if (e is T) return e
            fail("Se esperaba ${T::class.simpleName} pero se lanzó ${e::class.simpleName}")
        }
        fail("Se esperaba ${T::class.simpleName}")
        throw IllegalStateException()
    }

    @Test
    fun `exportar e importar conserva todos los datos`() {
        val parsed = BackupCodec.decode(BackupCodec.encode(sampleFile()), now)

        assertEquals(0, parsed.adjustedValues)
        assertEquals(2, parsed.categories.size)
        assertEquals(2, parsed.cardCount)
        val first = parsed.categories[0]
        assertEquals("Correos", first.name)
        assertEquals(10L, first.createdAt)
        val card = first.cards[0]
        assertEquals("Saludo", card.title)
        assertEquals("Hola,\n\"¿qué tal?\" ✨", card.content)
        assertEquals(4, card.rating)
        assertTrue(card.isFavorite)
        assertEquals(CardColor.PINK, card.color)
        assertEquals(40L, card.updatedAt)
        assertFalse(first.cards[1].isFavorite)
        assertTrue(parsed.categories[1].cards.isEmpty())
    }

    @Test
    fun `JSON no valido`() {
        assertThrows<BackupException.InvalidFile> { BackupCodec.decode("{ esto no es json", now) }
        assertThrows<BackupException.InvalidFile> { BackupCodec.decode("", now) }
    }

    @Test
    fun `JSON que no es de Prompi`() {
        assertThrows<BackupException.NotPrompiFile> { BackupCodec.decode("""{"nombre":"otra app"}""", now) }
        assertThrows<BackupException.NotPrompiFile> { BackupCodec.decode("""{"app":"Otra","formatVersion":1}""", now) }
        assertThrows<BackupException.InvalidFile> { BackupCodec.decode("""[1,2,3]""", now) }
    }

    @Test
    fun `version futura no soportada`() {
        val error = assertThrows<BackupException.UnsupportedVersion> {
            BackupCodec.decode("""{"app":"Prompi","formatVersion":99,"categories":[]}""", now)
        }
        assertEquals(99, error.version)
    }

    @Test
    fun `campos ausentes y claves desconocidas se toleran`() {
        val parsed = BackupCodec.decode(
            """{"app":"Prompi","formatVersion":1,"extra":true,
               "categories":[{"name":"A","cards":[{"title":"T","futuro":1}]}]}""",
            now,
        )
        val card = parsed.categories.single().cards.single()
        assertEquals("", card.content)
        assertEquals(Limits.RATING_DEFAULT, card.rating)
        assertEquals(CardColor.GRAY, card.color)
        assertEquals(now, card.createdAt)
    }

    @Test
    fun `valores fuera de rango se corrigen y se cuentan`() {
        val longTitle = "x".repeat(Limits.TITLE_MAX + 10)
        val longContent = "y".repeat(Limits.CONTENT_MAX + 1)
        val file = BackupFileDto(
            app = BackupCodec.APP_ID,
            formatVersion = 1,
            categories = listOf(
                BackupCategoryDto(
                    name = "   ",
                    cards = listOf(
                        BackupCardDto(title = longTitle, content = longContent, rating = 9, color = "MORADO"),
                        BackupCardDto(title = "  ", content = "ok", rating = 0),
                    ),
                ),
            ),
        )
        val parsed = BackupCodec.decode(BackupCodec.encode(file), now)
        val category = parsed.categories.single()
        val (a, b) = category.cards

        assertEquals(BackupCodec.DEFAULT_CATEGORY_NAME, category.name)
        assertEquals(Limits.TITLE_MAX, a.title.length)
        assertEquals(Limits.CONTENT_MAX, a.content.length)
        assertEquals(Limits.RATING_MAX, a.rating)
        assertEquals(CardColor.Default, a.color)
        assertEquals(BackupCodec.DEFAULT_CARD_TITLE, b.title)
        assertEquals(Limits.RATING_MIN, b.rating)
        // nombre + título + contenido + estrellas + color + título vacío + estrellas
        assertEquals(7, parsed.adjustedValues)
    }

    @Test
    fun `las posiciones se normalizan segun el orden original`() {
        val file = BackupFileDto(
            app = BackupCodec.APP_ID,
            formatVersion = 1,
            categories = listOf(
                BackupCategoryDto(name = "B", position = 7),
                BackupCategoryDto(name = "A", position = 2),
            ),
        )
        val parsed = BackupCodec.decode(BackupCodec.encode(file), now)
        assertEquals(listOf("A", "B"), parsed.categories.map { it.name })
        assertEquals(listOf(0, 1), parsed.categories.map { it.position })
    }

    @Test
    fun `se acepta un BOM al inicio del archivo`() {
        val text = "\uFEFF" + BackupCodec.encode(sampleFile())
        assertEquals(2, BackupCodec.decode(text, now).categories.size)
    }
}
