package com.david.tehilim

import android.app.Application
import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
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
            // SYSTEM : suit la langue de l'appareil si fr/en (tag vide = pas
            // d'override). Pour toute autre langue système, l'app n'étant
            // traduite qu'en fr/en, on force l'**anglais** (et non le repli
            // par défaut sur les ressources `values/` qui sont en français).
            AppLanguage.SYSTEM -> {
                val sys = systemLanguage()
                if (sys == "fr" || sys == "en") "" else "en"
            }
        }
        // V1.3.11 — sur API 33+ on bypass AppCompat et on tape LocaleManager
        // directement (AppCompat ne persiste pas sur Android 16/SDK 37).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val lm = getSystemService(LocaleManager::class.java)
            lm?.applicationLocales = if (tag.isEmpty()) {
                LocaleList.getEmptyLocaleList()
            } else {
                LocaleList.forLanguageTags(tag)
            }
        } else {
            val locales = if (tag.isEmpty()) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(tag)
            }
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }

    /**
     * Langue de l'**appareil** (et non l'override per-app), pour décider du
     * repli en mode SYSTEM. On lit les locales système (LocaleManager API 33+,
     * sinon la config système) afin que la décision reste stable même après
     * qu'on a posé un override "en".
     */
    private fun systemLanguage(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val lm = getSystemService(LocaleManager::class.java)
            val list = lm?.systemLocales
            if (list == null || list.isEmpty) "en" else list[0].language
        } else {
            val cfg = android.content.res.Resources.getSystem().configuration
            if (cfg.locales.isEmpty) "en" else cfg.locales[0].language
        }
    }
}
