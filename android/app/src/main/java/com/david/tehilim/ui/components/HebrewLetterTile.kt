package com.david.tehilim.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.david.tehilim.ui.theme.EzraSilFontFamily

/**
 * Pavé d'une lettre hébraïque — mirror du HebrewLetterTile SwiftUI V1.9.2.
 * Affiche la lettre (grande, Ezra), le nom phonétique, l'index + range de versets.
 */
@Composable
fun HebrewLetterTile(
    letter: String,
    index: Int,
    name: String? = null,
    verseStart: Int? = null,
    verseEnd: Int? = null,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 110.dp)
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                letter,
                style = TextStyle(fontFamily = EzraSilFontFamily, fontSize = 48.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (name != null) {
                Text(
                    name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                if (verseStart != null && verseEnd != null) "$index · v. $verseStart–$verseEnd" else "$index",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
