package com.david.tehilim

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.david.tehilim.core.model.AppLanguage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Point d'entrée de l'application Android.
 *
 * Initialise le conteneur de services [AppContainer] (équivalent du DI iOS),
 * exposé via [container] pour les Composables qui en ont besoin.
 *
 * Architecture délibérément simple — pas de Hilt/Koin pour le MVP. Le container
 * est instancié au démarrage et conservé pendant tout le cycle de vie de l'app.
 *
 * V1.3.0 — applique la locale (FR/EN/SYSTEM) AVANT toute UI pour que les
 * stringResource() résolvent dans la bonne langue dès le splash.
 */
class TehilimApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext)

        // Applique la locale sauvegardée AVANT que la moindre Activity n'attache
        // sa configuration. Lecture synchrone du DataStore via runBlocking : le
        // DataStore est lu une seule fois au démarrage, l'impact perf est nul.
        val savedLang = runBlocking { container.preferences.appLanguage.first() }
        val tag = when (savedLang) {
            AppLanguage.FR -> "fr"
            AppLanguage.EN -> "en"
            AppLanguage.SYSTEM -> ""
        }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }
}
