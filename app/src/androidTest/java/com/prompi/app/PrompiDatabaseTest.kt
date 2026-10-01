package com.prompi.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prompi.app.data.local.PrompiDatabase
import com.prompi.app.data.local.entity.CardEntity
import com.prompi.app.data.local.entity.CategoryEntity
import com.prompi.app.domain.search.SearchQueryBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrompiDatabaseTest {

    private lateinit var db: PrompiDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PrompiDatabase::class.java).build()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun category(name: String, position: Int = 0): Long =
        db.categoryDao().insert(CategoryEntity(name = name, position = position, createdAt = 1, updatedAt = 1))

    private suspend fun card(categoryId: Long, title: String, content: String = ""): Long =
        db.cardDao().insertAtTop(
            CardEntity(
                categoryId = categoryId, title = title, content = content, rating = 1,
                isFavorite = false, color = "GRAY", position = 0, createdAt = 1, updatedAt = 1,
            ),
        )

    private suspend fun search(text: String) = SearchQueryBuilder.build(text)!!.let {
        db.cardDao().search(it.all, it.titleOnly).first()
    }

    @Test
    fun busquedaIgnoraTildesYMayusculas() = runTest {
        val id = category("Recetas")
        card(id, "Canción de cuna", "Letra para dormir")
        card(id, "Café", "Cómo preparar un CAFÉ perfecto")

        assertEquals(listOf("Café"), search("cafe").map { it.card.title })
        assertEquals(listOf("Canción de cuna"), search("CANCION").map { it.card.title })
        assertEquals("Recetas", search("dormir").single().categoryName)
        assertTrue(search("inexistente").isEmpty())
    }

    @Test
    fun coincidenciasEnTituloAparecenPrimero() = runTest {
        val id = category("General")
        card(id, "Notas", "Un correo importante")
        card(id, "Correo formal", "Plantilla")

        assertEquals(listOf("Correo formal", "Notas"), search("correo").map { it.card.title })
    }

    @Test
    fun busquedaPorPrefijoYVariosTerminos() = runTest {
        val id = category("General")
        card(id, "Resumen ejecutivo", "Informe trimestral")
        card(id, "Resumen corto", "Texto")

        assertEquals(2, search("resu").size)
        assertEquals(listOf("Resumen ejecutivo"), search("resumen trimes").map { it.card.title })
    }

    @Test
    fun indiceSeActualizaAlEditarYBorrar() = runTest {
        val id = category("General")
        val cardId = card(id, "Antiguo", "texto")
        val entity = db.cardDao().get(cardId)!!
        db.cardDao().update(entity.copy(title = "Nuevo"))

        assertTrue(search("antiguo").isEmpty())
        assertEquals(1, search("nuevo").size)

        db.cardDao().deleteById(cardId)
        assertTrue(search("nuevo").isEmpty())
    }

    @Test
    fun borrarCategoriaEliminaSusFichasEnCascada() = runTest {
        val keep = category("Conservar", 0)
        val remove = category("Borrar", 1)
        card(keep, "A")
        val removed = card(remove, "B")

        db.categoryDao().deleteById(remove)

        assertNull(db.cardDao().get(removed))
        assertEquals(1, db.cardDao().observeCount().first())
    }

    @Test
    fun nuevasFichasSeInsertanArribaYSePuedenReordenar() = runTest {
        val id = category("General")
        val a = card(id, "A")
        val b = card(id, "B")
        val c = card(id, "C")

        assertEquals(listOf("C", "B", "A"), db.cardDao().getByCategory(id).map { it.title })

        db.cardDao().reorder(listOf(a, c, b))
        assertEquals(listOf("A", "C", "B"), db.cardDao().getByCategory(id).map { it.title })
    }

    @Test
    fun moverFichasAOtraCategoriaLasAnadeAlFinal() = runTest {
        val from = category("Origen", 0)
        val to = category("Destino", 1)
        card(to, "D1")
        card(from, "O1")

        db.cardDao().moveAll(from, to, db.cardDao().nextPosition(to))

        assertEquals(listOf("D1", "O1"), db.cardDao().getByCategory(to).map { it.title })
        assertTrue(db.cardDao().getByCategory(from).isEmpty())
    }
}
