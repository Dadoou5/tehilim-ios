package com.david.tehilim

import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.david.tehilim.features.onboarding.OnboardingScreen
import com.david.tehilim.features.splash.SplashScreen
import com.david.tehilim.navigation.AppNavigation
import com.david.tehilim.ui.theme.TehilimTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale

/**
 * Flux de démarrage V1.2.6 :
 *
 *   Android SplashScreen API (icône statique, < 1s)
 *     ↓
 *   SplashScreen Compose animé (titre hébreu + dédicace, ~1.8s, mirror iOS)
 *     ↓
 *   OnboardingScreen (1ère utilisation uniquement)
 *     ↓
 *   AppNavigation (5 onglets)
 */
class MainActivity : ComponentActivity() {

    /**
     * V1.3.11 — applique la locale per-app au contexte de base.
     *
     * Sur API 33+ on lit LocaleManager directement (AppCompatDelegate ne
     * persiste pas sur certains devices Android 16/SDK 37).
     * Sur API 32- on tombe sur AppCompatDelegate.
     */
    override fun attachBaseContext(newBase: Context) {
        val baseLocale = newBase.resources.configuration.locales[0]
        val targetLocale: Locale? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val lm = newBase.getSystemService(LocaleManager::class.java)
            val list = lm?.applicationLocales
            Log.i(TAG, "attachBaseContext: SDK=${Build.VERSION.SDK_INT}, base=$baseLocale, lm=$list")
            if (list == null || list.isEmpty) null else list[0]
        } else {
            val locales = AppCompatDelegate.getApplicationLocales()
            Log.i(TAG, "attachBaseContext: SDK=${Build.VERSION.SDK_INT}, base=$baseLocale, appcompat=$locales")
            if (locales.isEmpty) null else locales[0]
        }
        if (targetLocale == null) {
            super.attachBaseContext(newBase)
            return
        }
        Log.i(TAG, "attachBaseContext: wrapping with locale=$targetLocale")
        Locale.setDefault(targetLocale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(targetLocale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    companion object {
        private const val TAG = "TehilimLang"
    }

    /**
     * V1.4 — quand l'app est déjà ouverte et que le widget/notification
     * envoie un nouveau deep link (`tehilim://daily`, etc.), Android appelle
     * `onNewIntent` plutôt que de recréer l'Activity (grâce à
     * `launchMode="singleTop"` dans le manifest). On met à jour l'Intent
     * courant avec `setIntent()` pour que NavHost.compose re-route le deep
     * link via son mécanisme `navDeepLink` au prochain recomposition.
     *
     * Sans ce relais, l'Intent original (souvent ACTION_MAIN du launcher)
     * restait dans l'Activity, et le nouveau deep link était ignoré → bug
     * où taper le widget ne ramenait pas l'utilisateur sur Aujourd'hui.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as TehilimApplication).container

        // V1.4 — lecture synchrone du flag onboarding AVANT setContent pour
        // éviter le piège du `collectAsState(initial = true)` qui captait
        // `true` à la 1re composition (alors que la valeur réelle DataStore
        // pour un fresh install est `false`) → l'onboarding ne s'affichait
        // jamais. Pattern aligné sur `TehilimApplication.applyLanguagePreference`.
        val initialOnboardingDone = runBlocking { container.preferences.onboardingDone.first() }

        // V1.4 — détecte si l'app est lancée via deep link (widget / notif).
        // Dans ce cas, on saute le Compose splash (1.8s d'animation) pour
        // accéder directement à la destination demandée. Le system splash
        // (installSplashScreen, ~300ms) joue toujours, mais c'est inévitable
        // côté OS. Pour les launches classiques (tap icône launcher),
        // l'animation Compose continue à s'afficher normalement.
        val launchedViaDeepLink = intent?.data != null

        setContent {
            // V1.4 — câble la pref `theme` (système/clair/sombre) sur le
            // mode dark du TehilimTheme. Avant, TehilimTheme lisait
            // uniquement `isSystemInDarkTheme()`, le picker dans Réglages
            // était ignoré.
            val themePref by container.preferences.theme.collectAsState(initial = com.david.tehilim.core.model.AppTheme.SYSTEM)
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val darkMode = when (themePref) {
                com.david.tehilim.core.model.AppTheme.LIGHT -> false
                com.david.tehilim.core.model.AppTheme.DARK -> true
                com.david.tehilim.core.model.AppTheme.SYSTEM -> systemDark
            }
            TehilimTheme(darkTheme = darkMode) {
                // V1.3.3 — rememberSaveable pour que la splash ne rejoue PAS
                // quand l'Activity est recréée suite à un changement de langue.
                // V1.4 — initial state = true si lancement via deep link
                // (skip animation, l'user veut le contenu vite).
                var splashDone by rememberSaveable { mutableStateOf(launchedViaDeepLink) }

                // V1.4 — décision « onboarding ou pas » commitée UNE fois au
                // démarrage à partir du `initialOnboardingDone` lu de façon
                // synchrone (cf. onCreate). Reste stable pour la session ;
                // re-écrasable via le callback quand l'utilisateur finit
                // l'onboarding.
                var shouldShowOnboarding by remember { mutableStateOf(!initialOnboardingDone) }

                // Mode Chabbat : si actif, l'app est inaccessible et l'écran de
                // démarrage laisse place à « Chabbat Chalom ».
                val shabbat = com.david.tehilim.features.shabbat.rememberShabbatGate(container)

                when {
                    !splashDone -> SplashScreen(onFinished = { splashDone = true })
                    shabbat.isBlocking -> com.david.tehilim.features.shabbat.ChabbatChalomScreen(
                        endsAt = shabbat.endsAt,
                        onContinue = shabbat.onContinue
                    )
                    shouldShowOnboarding -> OnboardingScreen(container = container) {
                        shouldShowOnboarding = false
                    }
                    else -> AppNavigation(container = container)
                }
            }
        }
    }
}
