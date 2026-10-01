package com.prompi.app

import com.prompi.app.domain.model.takeSafe
import org.junit.Assert.assertEquals
import org.junit.Test

class TextLimitsTest {

    @Test
    fun `texto corto no cambia`() {
        assertEquals("hola", "hola".takeSafe(10))
    }

    @Test
    fun `recorta al limite`() {
        assertEquals("abc", "abcdef".takeSafe(3))
    }

    @Test
    fun `no parte un emoji por la mitad`() {
        val text = "ab\uD83D\uDE00" // "ab😀": el emoji ocupa 2 unidades UTF-16
        assertEquals("ab", text.takeSafe(3))
        assertEquals(text, text.takeSafe(4))
    }
}
