package com.david.tehilim.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.david.tehilim.R

/**
 * Typographie Tehilim — Material 3 par défaut pour l'UI + police Ezra SIL SR
 * pour l'hébreu (téamim/nikud).
 *
 * La police `ezra_sil_sr.ttf` est copiée automatiquement par Gradle depuis
 * le projet iOS au build.
 */

val EzraSilFontFamily = FontFamily(
    Font(R.font.ezra_sil_sr, FontWeight.Normal)
)

val TehilimTypography = Typography(
    // Material 3 styles par défaut — Compose les fournit, on garde.
)

/**
 * Helper pour obtenir un TextStyle hébreu avec scale dynamique selon la pref
 * `textSizeHebrew`. À appeler depuis les Composables qui rendent du texte hébreu.
 */
fun hebrewBodyStyle(scale: Float = 1.0f): TextStyle = TextStyle(
    fontFamily = EzraSilFontFamily,
    fontSize = (22 * scale).sp,
    lineHeight = (32 * scale).sp,
    fontWeight = FontWeight.Normal
)

fun hebrewTitleStyle(scale: Float = 1.0f): TextStyle = TextStyle(
    fontFamily = EzraSilFontFamily,
    fontSize = (28 * scale).sp,
    lineHeight = (38 * scale).sp,
    fontWeight = FontWeight.SemiBold
)

fun verseNumberStyle(scale: Float = 1.0f): TextStyle = TextStyle(
    fontSize = (15 * scale).sp,
    fontWeight = FontWeight.Medium
)

fun frenchBodyStyle(scale: Float = 1.0f): TextStyle = TextStyle(
    fontSize = (16 * scale).sp,
    lineHeight = (22 * scale).sp
)
