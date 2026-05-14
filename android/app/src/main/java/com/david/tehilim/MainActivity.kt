package com.david.tehilim

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.david.tehilim.navigation.AppNavigation
import com.david.tehilim.ui.theme.TehilimTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash screen API moderne (Android 12+) — fallback compat 8+.
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as TehilimApplication).container

        setContent {
            TehilimTheme {
                AppNavigation(container = container)
            }
        }
    }
}
