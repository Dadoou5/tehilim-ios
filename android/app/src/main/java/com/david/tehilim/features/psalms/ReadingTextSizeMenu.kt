package com.david.tehilim.features.psalms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.TextDecrease
import androidx.compose.material.icons.outlined.TextIncrease
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.david.tehilim.AppContainer
import com.david.tehilim.R
import com.david.tehilim.core.model.TextSize
import kotlinx.coroutines.launch

/**
 * Bouton « Aa » + menu A− / A+ de taille du texte en lecture (persisté).
 * Partagé entre l'écran d'un Tehilim et l'écran d'une section du Tehilim 119.
 * Ajuste le texte principal (hébreu/phonétique) et, si la traduction est
 * affichée, la traduction, du même pas.
 */
@Composable
fun ReadingTextSizeButton(
    container: AppContainer,
    textSizeHebrew: TextSize,
    textSizeFR: TextSize,
    includeTranslation: Boolean
) {
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Outlined.FormatSize, contentDescription = stringResource(R.string.reading_text_size))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Column(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp).width(220.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.reading_text_size), style = MaterialTheme.typography.titleSmall)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    IconButton(
                        onClick = { scope.launch {
                            container.preferences.setTextSizeHebrew(textSizeHebrew.stepped(-1))
                            if (includeTranslation) container.preferences.setTextSizeFR(textSizeFR.stepped(-1))
                        } },
                        enabled = !textSizeHebrew.isSmallest
                    ) { Icon(Icons.Outlined.TextDecrease, stringResource(R.string.reading_text_smaller)) }

                    Text("Aa", fontSize = (17 * textSizeHebrew.scale).sp, fontWeight = FontWeight.SemiBold)

                    IconButton(
                        onClick = { scope.launch {
                            container.preferences.setTextSizeHebrew(textSizeHebrew.stepped(1))
                            if (includeTranslation) container.preferences.setTextSizeFR(textSizeFR.stepped(1))
                        } },
                        enabled = !textSizeHebrew.isLargest
                    ) { Icon(Icons.Outlined.TextIncrease, stringResource(R.string.reading_text_larger)) }
                }
                Text(stringResource(textSizeHebrew.labelRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
