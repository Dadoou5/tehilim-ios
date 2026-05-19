package com.david.tehilim

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
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
     * V1.3.5 — applique la locale AppCompat à la racine du contexte Activity.
     *
     * Sur AppCompatActivity, cet override est intégré. Sur ComponentActivity nu,
     * il faut le faire à la main, sinon `AppCompatDelegate.setApplicationLocales`
     * n'a aucun effet sur les Resources de l'Activity (le cache reste figé sur
     * la locale système). C'est exactement le bug qu'on avait : changement de
     * langue ignoré sauf pour les écrans qui lisent la pref via DataStore
     * (Cas de la vie).
     */
    override fun attachBaseContext(newBase: Context) {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) {
            super.attachBaseContext(newBase)
            return
        }
        val locale = locales[0] ?: return super.attachBaseContext(newBase)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as TehilimApplication).container

        setContent {
            TehilimTheme {
                // V1.3.3 — rememberSaveable pour que la splash ne rejoue PAS
                // quand l'Activity est recréée suite à un changement de langue.
                var splashDone by rememberSaveable { mutableStateOf(false) }

                val onboardingDone by container.preferences.onboardingDone.collectAsState(initial = true)
                var shouldShowOnboarding by remember { mutableStateOf<Boolean?>(null) }
                if (shouldShowOnboarding == null) {
                    shouldShowOnboarding = !onboardingDone
                }

                when {
                    !splashDone -> SplashScreen(onFinished = { splashDone = true })
                    shouldShowOnboarding == true -> OnboardingScreen(container = container) {
                        shouldShowOnboarding = false
                    }
                    else -> AppNavigation(container = container)
                }
            }
        }
    }
}
