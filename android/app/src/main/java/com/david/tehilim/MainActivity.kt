package com.david.tehilim

import android.content.Context
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
     * V1.3.9 — applique la locale AppCompat manuellement UNIQUEMENT sur API < 33.
     *
     * Sur API 33+ (Android 13+), LocaleManager du système applique déjà la locale
     * per-app à `newBase` avant que ce hook ne soit appelé — notre wrapping
     * supplémentaire ferait double emploi et peut cacher une stale config.
     *
     * Sur API 32-, AppCompat n'a pas de hook automatique pour ComponentActivity
     * (uniquement pour AppCompatActivity) → on applique nous-mêmes la locale au
     * contexte de base, sinon le cache Resources reste figé.
     */
    override fun attachBaseContext(newBase: Context) {
        Log.i(TAG, "attachBaseContext: SDK=${Build.VERSION.SDK_INT}, base locale=${newBase.resources.configuration.locales[0]}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+ : laisse l'OS faire son job, pas de double wrap.
            super.attachBaseContext(newBase)
            return
        }
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) {
            super.attachBaseContext(newBase)
            return
        }
        val locale = locales[0] ?: return super.attachBaseContext(newBase)
        Log.i(TAG, "attachBaseContext: applying AppCompat locale=$locale")
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    companion object {
        private const val TAG = "TehilimLang"
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
