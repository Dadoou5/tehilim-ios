package com.david.tehilim.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Theme Tehilim — Material 3 avec couleurs custom matching iOS.
 *
 * Sur Android 12+, on peut activer Material You (dynamic color) qui pioche
 * dans le wallpaper de l'utilisateur. Par défaut désactivé pour garder la
 * cohérence visuelle avec iOS.
 */
@Composable
fun TehilimTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,    // mis à `false` pour cohérence iOS
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> darkColorScheme(
            primary = AccentMain,
            onPrimary = TextPrimaryDark,
            background = BgPrimaryDark,
            surface = BgSurfaceDark,
            surfaceVariant = BgElevatedDark,
            onBackground = TextPrimaryDark,
            onSurface = TextPrimaryDark,
            onSurfaceVariant = TextSecondaryDark,
            outline = DividerDark,
            error = ErrorToken
        )
        else -> lightColorScheme(
            primary = AccentMain,
            onPrimary = BgSurfaceLight,
            background = BgPrimaryLight,
            surface = BgSurfaceLight,
            surfaceVariant = BgElevatedLight,
            onBackground = TextPrimaryLight,
            onSurface = TextPrimaryLight,
            onSurfaceVariant = TextSecondaryLight,
            outline = DividerToken,
            error = ErrorToken
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TehilimTypography,
        content = content
    )
}
