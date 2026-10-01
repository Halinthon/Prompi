package com.prompi.app.di

import android.content.Context
import com.prompi.app.data.local.PrompiDatabase
import com.prompi.app.data.preferences.SettingsRepositoryImpl
import com.prompi.app.data.repository.BackupRepositoryImpl
import com.prompi.app.data.repository.CardRepositoryImpl
import com.prompi.app.data.repository.CategoryRepositoryImpl
import com.prompi.app.domain.repository.BackupRepository
import com.prompi.app.domain.repository.CardRepository
import com.prompi.app.domain.repository.CategoryRepository
import com.prompi.app.domain.repository.SettingsRepository

/**
 * Inyección de dependencias manual. Cada dependencia se crea de forma perezosa y una sola vez.
 * Si el proyecto crece, este contenedor puede sustituirse por Hilt sin cambiar las pantallas.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val database: PrompiDatabase by lazy { PrompiDatabase.build(appContext) }

    val categoryRepository: CategoryRepository by lazy { CategoryRepositoryImpl(database) }
    val cardRepository: CardRepository by lazy { CardRepositoryImpl(database) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepositoryImpl(appContext) }
    val backupRepository: BackupRepository by lazy {
        BackupRepositoryImpl(database, appContext.contentResolver)
    }
}
