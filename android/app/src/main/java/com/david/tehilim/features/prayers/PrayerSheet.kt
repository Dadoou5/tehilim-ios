package com.david.tehilim.features.prayers

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.david.tehilim.AppContainer
import com.david.tehilim.R
import com.david.tehilim.core.model.Prayer
import com.david.tehilim.core.model.TextMode
import com.david.tehilim.core.model.Verse
import com.david.tehilim.ui.components.IluyNishmatBanner
import com.david.tehilim.ui.components.VerseRow

/**
 * Bottom sheet pour afficher la prière avant/après la lecture des Tehilim.
 * Mirror du PrayerView SwiftUI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerSheet(
    kind: Prayer.Kind,
    container: AppContainer,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val textMode by container.preferences.textMode.collectAsState(initial = TextMode.HEBREW)
    val textSizeHebrew by container.preferences.textSizeHebrew.collectAsState(initial = com.david.tehilim.core.model.TextSize.MEDIUM)
    val textSizeFR by container.preferences.textSizeFR.collectAsState(initial = com.david.tehilim.core.model.TextSize.MEDIUM)
    val translationFR by container.preferences.translationFR.collectAsState(initial = false)
    val numberStyle by container.preferences.verseNumberStyle.collectAsState(initial = com.david.tehilim.core.model.VerseNumberStyle.HEBREW)
    val appLanguage by container.preferences.appLanguage.collectAsState(initial = com.david.tehilim.core.model.AppLanguage.SYSTEM)

    val prayer = Prayer.of(kind)
    val verses: List<Verse> = prayer.verseRefs.mapNotNull { ref ->
        container.psalmRepository.psalm(ref.psalmId)?.verses?.firstOrNull { it.number == ref.verseNumber }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
            item {
                Text(stringResource(kind.titleRes), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(kind.subtitleRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                IluyNishmatBanner()
            }
            items(verses) { v ->
                VerseRow(
                    verse = v,
                    showTranslation = translationFR,
                    textMode = textMode,
                    textSizeHebrew = textSizeHebrew,
                    textSizeFR = textSizeFR,
                    numberStyle = numberStyle,
                    translationLang = appLanguage.translation,
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider()
            }
        }
    }
}
