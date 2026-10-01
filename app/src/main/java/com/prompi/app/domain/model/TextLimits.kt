package com.prompi.app.domain.model

/**
 * Recorta a [maxLength] unidades UTF-16 sin partir un emoji ni otro carácter que ocupe dos
 * unidades (par sustituto): si el corte cae en medio, se elimina la mitad sobrante.
 */
fun String.takeSafe(maxLength: Int): String {
    if (length <= maxLength) return this
    val cut = take(maxLength)
    return if (cut.isNotEmpty() && cut.last().isHighSurrogate()) cut.dropLast(1) else cut
}
