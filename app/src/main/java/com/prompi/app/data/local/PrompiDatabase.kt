package com.prompi.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.prompi.app.data.local.dao.CardDao
import com.prompi.app.data.local.dao.CategoryDao
import com.prompi.app.data.local.entity.CardEntity
import com.prompi.app.data.local.entity.CardFtsEntity
import com.prompi.app.data.local.entity.CategoryEntity

/**
 * Base de datos local de Prompi.
 *
 * Al cambiar el esquema: incrementa [version], añade una Migration explícita en
 * [build] y sube a Git el nuevo JSON generado en app/schemas. Nunca se usa una
 * migración destructiva, para no perder datos del usuario.
 */
@Database(
    entities = [CategoryEntity::class, CardEntity::class, CardFtsEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class PrompiDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun cardDao(): CardDao

    companion object {
        private const val DATABASE_NAME = "prompi.db"

        fun build(context: Context): PrompiDatabase =
            Room.databaseBuilder(context, PrompiDatabase::class.java, DATABASE_NAME)
                // .addMigrations(MIGRATION_1_2, ...) cuando existan nuevas versiones.
                .build()
    }
}
