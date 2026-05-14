package com.david.tehilim.features.psalm119

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.david.tehilim.AppContainer
import com.david.tehilim.core.model.TextMode
import com.david.tehilim.ui.components.IluyNishmatBanner
import com.david.tehilim.ui.components.VerseRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Psalm119SectionScreen(container: AppContainer, index: Int, navController: NavController) {
    val section = container.psalm119Repository.sectionAt(index) ?: return
    val psalm = container.psalmRepository.psalm(119) ?: return
    val verses = psalm.verses.filter { it.number in section.versesRange }

    val textMode by container.preferences.textMode.collectAsState(initial = TextMode.HEBREW)
    val textSizeHebrew by container.preferences.textSizeHebrew.collectAsState(initial = com.david.tehilim.core.model.TextSize.MEDIUM)
    val textSizeFR by container.preferences.textSizeFR.collectAsState(initial = com.david.tehilim.core.model.TextSize.MEDIUM)
    val translationFR by container.preferences.translationFR.collectAsState(initial = true)
    val numberStyle by container.preferences.verseNumberStyle.collectAsState(initial = com.david.tehilim.core.model.VerseNumberStyle.HEBREW)
    val appLanguage by container.preferences.appLanguage.collectAsState(initial = com.david.tehilim.core.model.AppLanguage.SYSTEM)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${section.letter} — ${section.name}") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            item { IluyNishmatBanner() }
            item {
                Text(
                    "v. ${section.verseStart}–${section.verseEnd}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
            items(verses) { v ->
                VerseRow(
                    verse = v,
                    showTranslation = translationFR,
                    textMode = textMode,
                    textSizeHebrew = textSizeHebrew,
                    textSizeFR = textSizeFR,
                    numberStyle = numberStyle,
                    translationLang = appLanguage.translation
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}
