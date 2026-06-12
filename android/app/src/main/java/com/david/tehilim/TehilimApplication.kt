package com.david.tehilim

import android.app.Application
import android.app.LocaleManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.david.tehilim.core.model.AppLanguage
import com.david.tehilim.core.service.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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

        // Feature « Chaîne de Tehilim » : connexion anonyme Supabase (uid stable
        // par appareil, session persistée par le SDK). No-op si la config
        // Supabase est absente → l'app reste 100 % locale.
        maybeSignInAnonymously()
        ensureChainNotificationChannel()

        // Applique la locale sauvegardée AVANT que la moindre Activity n'attache
        // sa configuration. Lecture synchrone du DataStore via runBlocking : le
        // DataStore est lu une seule fois au démarrage, l'impact perf est nul.
        val savedLang = runBlocking { container.preferences.appLanguage.first() }
        val tag = when (savedLang) {
            AppLanguage.FR -> "fr"
            AppLanguage.EN -> "en"
            AppLanguage.HE -> "he"
            // SYSTEM : suit la langue de l'appareil si fr/en/he (tag vide = pas
            // d'override). Pour toute autre langue système, l'app n'étant
            // traduite qu'en fr/en/he, on force l'**anglais** (et non le repli
            // par défaut sur les ressources `values/` qui sont en français).
            AppLanguage.SYSTEM -> {
                val sys = systemLanguage()
                if (sys == "fr" || sys == "en" || sys == "he" || sys == "iw") "" else "en"
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

    /** Connexion anonyme si Supabase est configuré et qu'on n'est pas déjà connecté. */
    private fun maybeSignInAnonymously() {
        val client = SupabaseClientProvider.client ?: return   // pas de config → no-op
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                // IMPORTANT : attendre le chargement de la session persistée AVANT de
                // tester currentUserOrNull(). Sinon, à la recréation du process (ex.
                // l'app est tuée en arrière-plan pendant un partage), on re-signe
                // anonymement et on crée un NOUVEL uid → perte du statut « maître ».
                client.auth.awaitInitialization()
                if (client.auth.currentUserOrNull() == null) {
                    client.auth.signInAnonymously()
                }
            }
        }
    }

    /** Canal de notification pour les push de chaîne (FCM en arrière-plan, Android 8+). */
    private fun ensureChainNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            if (mgr?.getNotificationChannel("tehilim_chain") == null) {
                mgr?.createNotificationChannel(
                    NotificationChannel("tehilim_chain", "Chaînes de Tehilim", NotificationManager.IMPORTANCE_DEFAULT)
                )
            }
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
