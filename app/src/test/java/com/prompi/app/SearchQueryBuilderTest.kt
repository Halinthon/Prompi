package com.prompi.app

import com.prompi.app.domain.search.SearchQueryBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchQueryBuilderTest {

    @Test
    fun `texto vacio o solo simbolos devuelve null`() {
        assertNull(SearchQueryBuilder.build(""))
        assertNull(SearchQueryBuilder.build("   "))
        assertNull(SearchQueryBuilder.build("\"*()-:^"))
    }

    @Test
    fun `cada termino se busca por prefijo`() {
        val query = SearchQueryBuilder.build("correo  formal")!!
        assertEquals("correo* formal*", query.all)
        assertEquals("title:correo* title:formal*", query.titleOnly)
    }

    @Test
    fun `operadores FTS se convierten en terminos normales`() {
        val query = SearchQueryBuilder.build("gato AND NOT perro OR \"pez\"")!!
        assertEquals("gato* and* not* perro* or* pez*", query.all)
    }

    @Test
    fun `se conservan letras con tilde y la enie`() {
        assertEquals("canción* año*", SearchQueryBuilder.build("Canción AÑO")!!.all)
    }

    @Test
    fun `terminos repetidos se eliminan y se limita su numero`() {
        assertEquals("hola*", SearchQueryBuilder.build("hola HOLA hola")!!.all)
        val many = (1..20).joinToString(" ") { "t$it" }
        val terms = SearchQueryBuilder.build(many)!!.all.split(' ')
        assertEquals(SearchQueryBuilder.MAX_TERMS, terms.size)
    }

    @Test
    fun `los guiones separan palabras`() {
        assertEquals("e* mail*", SearchQueryBuilder.build("e-mail")!!.all)
    }
}
