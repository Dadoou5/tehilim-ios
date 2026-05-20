package com.david.tehilim

import android.app.LocaleManager
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
