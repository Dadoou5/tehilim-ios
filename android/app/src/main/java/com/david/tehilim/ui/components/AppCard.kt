package com.david.tehilim.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Carte stylisée Tehilim — équivalent du `.appCard()` modifier iOS.
 * Surface, coin arrondi, bordure subtile. Material 3 sous le capot.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)

    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors, border = border) {
            content()
        }
    } else {
        Card(modifier = modifier, shape = shape, colors = colors, border = border) {
            content()
        }
    }
}
