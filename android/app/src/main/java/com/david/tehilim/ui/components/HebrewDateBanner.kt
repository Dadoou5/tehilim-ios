package com.david.tehilim.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.david.tehilim.core.service.HebrewDateFormatter
import com.david.tehilim.ui.theme.EzraSilFontFamily

/**
 * Bandeau date hébraïque — équivalent du HebrewDateBanner SwiftUI.
 *
 * Affichage 2 lignes :
 *   « Jeudi · 27 Iyar 5786 »
 *   « כ״ז באייר ה׳תשפ״ו »  (RTL, police Ezra)
 */
@Composable
fun HebrewDateBanner(modifier: Modifier = Modifier) {
    // remember pour ne pas refaire le calcul à chaque recomposition
    val date = remember { HebrewDateFormatter.formatted() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = "${date.dayOfWeek} · ${date.transliterated}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
                text = date.hebrew,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = EzraSilFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
