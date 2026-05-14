package com.david.tehilim

import android.app.Application

/**
 * Point d'entrée de l'application Android.
 *
 * Initialise le conteneur de services [AppContainer] (équivalent du DI iOS),
 * exposé via [container] pour les Composables qui en ont besoin.
 *
 * Architecture délibérément simple — pas de Hilt/Koin pour le MVP. Le container
 * est instancié au démarrage et conservé pendant tout le cycle de vie de l'app.
 */
class TehilimApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext)
    }
}
