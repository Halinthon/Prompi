package com.prompi.app

import android.app.Application
import com.prompi.app.di.AppContainer

/** Punto de entrada de la aplicación: crea el contenedor de dependencias. */
class PrompiApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
