package com.prompi.app.domain.model

/** Límites de validación compartidos por la interfaz, la base de datos y la importación. */
object Limits {
    const val TITLE_MAX = 50
    const val CONTENT_MAX = 7_000
    const val CATEGORY_NAME_MAX = 40
    const val RATING_MIN = 1
    const val RATING_MAX = 5
    const val RATING_DEFAULT = 1
    const val SEARCH_QUERY_MAX = 100
}
