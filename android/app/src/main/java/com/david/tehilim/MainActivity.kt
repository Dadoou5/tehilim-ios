package com.david.tehilim

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.david.tehilim.features.onboarding.OnboardingScreen
import com.david.tehilim.navigation.AppNavigation
import com.david.tehilim.ui.theme.TehilimTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as TehilimApplication).container

        setContent {
            TehilimTheme {
                val onboardingDone by container.preferences.onboardingDone.collectAsState(initial = true)
                var shouldShowOnboarding by remember { mutableStateOf<Boolean?>(null) }
                // Effet : à la première recomposition, on lit la pref une fois.
                if (shouldShowOnboarding == null) {
                    shouldShowOnboarding = !onboardingDone
                }

                if (shouldShowOnboarding == true) {
                    OnboardingScreen(container = container) {
                        shouldShowOnboarding = false
                    }
                } else {
                    AppNavigation(container = container)
                }
            }
        }
    }
}
