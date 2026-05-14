package com.david.tehilim.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.david.tehilim.ui.theme.EzraSilFontFamily

/**
 * Bandeau de dédicace « Ilouy Nichmat » affiché au début de chaque écran de lecture.
 * Mirror du composant iOS IluyNishmatBanner.swift.
 */
@Composable
fun IluyNishmatBanner(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(thickness = 0.5.dp)
        Spacer(Modifier.height(8.dp))

        // Hébreu — affichage forcé en RTL
        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
                text = "לעילוי נשמת ג׳והאן מאיר בן שרה בוגנים",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = EzraSilFontFamily
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(2.dp))

        Text(
            text = "Pour l'élévation de l'âme de Johann Meïr ben Sarah Bouganim",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(thickness = 0.5.dp)
    }
}
