package com.david.tehilim.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.david.tehilim.R
import com.david.tehilim.core.model.TextMode
import com.david.tehilim.core.model.TextSize
import com.david.tehilim.core.model.TranslationLanguage
import com.david.tehilim.core.model.Verse
import com.david.tehilim.core.model.VerseNumberStyle
import com.david.tehilim.core.service.HebrewTransliterator
import com.david.tehilim.ui.theme.frenchBodyStyle
import com.david.tehilim.ui.theme.hebrewBodyStyle
import com.david.tehilim.ui.theme.verseNumberStyle

private fun displayedNumber(verse: Verse, style: VerseNumberStyle): String = when (style) {
    VerseNumberStyle.HEBREW -> verse.hebrewNumber
    VerseNumberStyle.ARABIC -> verse.number.toString()
}

/**
 * Une ligne de verset — équivalent du VerseRowView iOS V1.9.0.
 *
 * Layout :
 * - Mode hébreu : texte hébreu aligné à droite (RTL) + numéro à droite
 * - Mode phonétique : texte translittéré à gauche + numéro à gauche
 * - Traduction en dessous si showTranslation
 * - **Side-by-side** (iPad paysage tablette) : hébreu+numéro à droite,
 *   traduction à gauche, côte à côte dans une Row
 *
 * **Long-press** : déclenche `onLongClick` (= partage du verset en image 1080×1080).
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
    onLongClick: (() -> Unit)? = null,
    sideBySideTranslation: Boolean = false,
    commentaries: List<com.david.tehilim.core.model.VerseCommentary> = emptyList(),
    showCommentaries: Boolean = false,
    commentaryCode: String = "he"
) {
    val useSideBySide = sideBySideTranslation && showTranslation && textMode == TextMode.HEBREW
    val baseModifier = modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)
    val interactiveModifier = if (onLongClick != null) {
        baseModifier.combinedClickable(onLongClick = onLongClick, onClick = {})
    } else baseModifier

    // V2.2.c — ligne verrouillée en LTR : le numéro reste à droite du texte
    // hébreu et la traduction calée à gauche, identiques quelle que soit la
    // langue d'interface. Sous UI hébraïque (RTL global), sans ce verrou, la
    // Row extérieure se mirroir et le texte hébreu se cale à gauche. Le texte
    // hébreu lui-même reste en RTL (provider local plus bas).
    androidx.compose.runtime.CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Ltr
    ) {
    // V1.2 — Side-by-side (paysage tablette) : 2 colonnes au lieu de stacked
    if (useSideBySide) {
        Row(
            modifier = interactiveModifier,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Colonne gauche : traduction (LTR)
            val translation = verse.translation(translationLang)
            Text(
                text = translation ?: stringResource(R.string.msg_translation_unavailable),
                style = frenchBodyStyle(textSizeFR.scale),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            // Colonne droite : hébreu + numéro (RTL)
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
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
        }
    } else {

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
                    text = stringResource(R.string.msg_translation_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // V2.4 — commentaires (mode étude), repliables sous le verset.
        if (showCommentaries && commentaries.isNotEmpty()) {
            CommentarySection(commentaries, commentaryCode, textSizeHebrew)
        }
    }
    }
    }
}

@Composable
private fun CommentarySection(
    commentaries: List<com.david.tehilim.core.model.VerseCommentary>,
    code: String,
    textSizeHebrew: TextSize
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Outlined.MenuBook, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp))
            Text(
                stringResource(R.string.commentaries_label) + " · ${commentaries.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)
            )
        }
        if (expanded) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                commentaries.forEach { CommentaryCard(it, code, textSizeHebrew) }
            }
        }
    }
}

@Composable
private fun CommentaryCard(c: com.david.tehilim.core.model.VerseCommentary, code: String, textSizeHebrew: TextSize) {
    val body = c.text(code)
    val isHebrew = body == c.he
    val dir = if (isHebrew) LayoutDirection.Rtl else LayoutDirection.Ltr
    androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides dir) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                    androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                )
                .padding(12.dp),
            horizontalAlignment = if (isHebrew) Alignment.End else Alignment.Start
        ) {
            Text(
                c.kind.hebrewName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (isHebrew) {
                Text(
                    text = buildAnnotatedString {
                        c.lemma?.takeIf { it.isNotBlank() }?.let {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface)) { append("$it ") }
                        }
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            append(c.he)
                        }
                    },
                    // Suit le réglage « Taille hébreu » (≈ 0,80× le verset).
                    style = hebrewBodyStyle(textSizeHebrew.scale * 0.80f),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth().padding(top = 3.dp)
                )
            } else {
                Text(
                    text = buildAnnotatedString {
                        // Marque gauche-à-droite (U+200E) en tête : force le paragraphe
                        // en LTR pour que le lemme hébreu reste à GAUCHE et que la
                        // traduction se lise normalement (sinon tout bascule en RTL).
                        append("‎")
                        // Dibour hamatchil hébreu en gras (non traduit), puis la traduction.
                        c.lemma?.takeIf { it.isNotBlank() }?.let {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface)) { append("$it ") }
                        }
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            append(body)
                        }
                    },
                    // Traduction (non grasse) légèrement plus petite que le
                    // commentaire hébreu, et suit le réglage « Taille hébreu ».
                    style = frenchBodyStyle(textSizeHebrew.scale * 1.01f).copy(
                        textDirection = androidx.compose.ui.text.style.TextDirection.Ltr
                    ),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth().padding(top = 3.dp)
                )
            }
        }
    }
}

