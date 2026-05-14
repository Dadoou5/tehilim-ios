package com.david.tehilim.ui.theme

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Mirror simplifié du AdaptiveLayout iOS.
 *
 * - `screenWidthDp` > 600 → tablette (regular size class iOS équivalent)
 * - Reading max width = 700dp pour confort de lecture sur grand écran
 * - Side-by-side activé en paysage tablette (width >= 900dp)
 */
object AdaptiveLayout {
    const val READING_MAX_WIDTH_DP = 700
    const val SIDE_BY_SIDE_MIN_WIDTH_DP = 900
    const val SIDE_BY_SIDE_MAX_WIDTH_DP = 1200

    @Composable
    @ReadOnlyComposable
    fun isTablet(): Boolean = LocalConfiguration.current.screenWidthDp >= 600

    @Composable
    @ReadOnlyComposable
    fun useSideBySide(): Boolean = LocalConfiguration.current.screenWidthDp >= SIDE_BY_SIDE_MIN_WIDTH_DP

    @Composable
    @ReadOnlyComposable
    fun horizontalPadding(): Dp = if (isTablet()) 24.dp else 16.dp

    @Composable
    @ReadOnlyComposable
    fun psalm119Columns(): Int = if (isTablet()) 6 else 4

    @Composable
    @ReadOnlyComposable
    fun exploreColumns(): Int = if (isTablet()) 3 else 2
}

/** Modifier qui plafonne la largeur d'une vue de lecture (700dp par défaut) et centre. */
fun Modifier.readingWidth(maxWidthDp: Int = AdaptiveLayout.READING_MAX_WIDTH_DP): Modifier =
    this
        .widthIn(max = maxWidthDp.dp)
        .fillMaxWidth()
