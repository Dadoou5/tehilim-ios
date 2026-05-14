package com.david.tehilim.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.david.tehilim.core.model.TextMode
import com.david.tehilim.core.model.TextSize
import com.david.tehilim.core.model.TranslationLanguage
import com.david.tehilim.core.model.Verse
import com.david.tehilim.core.model.VerseNumberStyle
import com.david.tehilim.core.service.HebrewTransliterator
import com.david.tehilim.ui.theme.frenchBodyStyle
import com.david.tehilim.ui.theme.hebrewBodyStyle
import com.david.tehilim.ui.theme.verseNumberStyle

/**
 * Une ligne de verset — équivalent du VerseRowView iOS.
 *
 * Layout :
 * - Mode hébreu : texte hébreu aligné à droite (RTL) + numéro à droite
 * - Mode phonétique : texte translittéré à gauche + numéro à gauche
 * - Traduction en dessous si showTranslation
 *
 * **Long-press** : déclenche `onLongClick` (= partage du verset en image stylisée 1080×1080).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VerseRow(
    verse: Verse,
    showTranslation: Boolean,
    textMode: TextMode,
    textSizeHebrew: TextSize,
    textSizeFR: TextSize,
    numberStyle: VerseNumberStyle,
    translationLang: TranslationLanguage,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null
) {
    val baseModifier = modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)
    val interactiveModifier = if (onLongClick != null) {
        baseModifier.combinedClickable(onLongClick = onLongClick, onClick = {})
    } else baseModifier

    Column(
        modifier = interactiveModifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (textMode == TextMode.HEBREW) {
            // Texte hébreu : RTL — numéro à droite, texte aligné à droite
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = verse.hebrew,
                        style = hebrewBodyStyle(textSizeHebrew.scale),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = displayedNumber(verse, numberStyle),
                    style = verseNumberStyle(textSizeHebrew.scale),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Mode phonétique : numéro à gauche, texte translittéré à gauche
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = displayedNumber(verse, numberStyle),
                    style = verseNumberStyle(textSizeHebrew.scale),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // V1.1 : translittération sépharade via HebrewTransliterator
                Text(
                    text = HebrewTransliterator.transliterate(verse.hebrew),
                    style = hebrewBodyStyle(textSizeHebrew.scale),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (showTranslation) {
            val translation = verse.translation(translationLang)
            if (!translation.isNullOrBlank()) {
                Text(
                    text = translation,
                    style = frenchBodyStyle(textSizeFR.scale),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Traduction non disponible.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun displayedNumber(verse: Verse, style: VerseNumberStyle): String =
    when (style) {
        VerseNumberStyle.HEBREW -> verse.hebrewNumber
        VerseNumberStyle.ARABIC -> verse.number.toString()
    }
