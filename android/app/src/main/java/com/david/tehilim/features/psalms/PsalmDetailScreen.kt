package com.david.tehilim.features.psalms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.david.tehilim.AppContainer
import com.david.tehilim.core.model.TextMode
import com.david.tehilim.features.sharing.VerseShareRenderer
import com.david.tehilim.ui.components.IluyNishmatBanner
import com.david.tehilim.ui.components.VerseRow
import com.david.tehilim.ui.theme.EzraSilFontFamily
import com.david.tehilim.ui.theme.hebrewTitleStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PsalmDetailScreen(container: AppContainer, psalmId: Int, navController: NavController) {
    val psalm = container.psalmRepository.psalm(psalmId)
    if (psalm == null) {
        Text("Tehilim introuvable.")
        return
    }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600
    val isLandscape = LocalConfiguration.current.screenWidthDp > LocalConfiguration.current.screenHeightDp
    val favorites by container.favorites.ids.collectAsState()
    val isFav = favorites.contains(psalmId)

    val textMode by container.preferences.textMode.collectAsState(initial = TextMode.HEBREW)
    val textSizeHebrew by container.preferences.textSizeHebrew.collectAsState(initial = com.david.tehilim.core.model.TextSize.MEDIUM)
    val textSizeFR by container.preferences.textSizeFR.collectAsState(initial = com.david.tehilim.core.model.TextSize.MEDIUM)
    val translationFR by container.preferences.translationFR.collectAsState(initial = false)
    val numberStyle by container.preferences.verseNumberStyle.collectAsState(initial = com.david.tehilim.core.model.VerseNumberStyle.HEBREW)
    val appLanguage by container.preferences.appLanguage.collectAsState(initial = com.david.tehilim.core.model.AppLanguage.SYSTEM)

    // Override local pour toggle la traduction sans toucher au global
    var localShowFR by remember { mutableStateOf<Boolean?>(null) }
    val showFR = localShowFR ?: translationFR

    LaunchedEffect(psalmId) {
        container.preferences.setLastReadPsalmId(psalmId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tehilim ${psalm.id} · ${psalm.hebrewNumber}") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        container.favorites.toggle(psalmId)
                    }) {
                        Icon(
                            if (isFav) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isFav) "Retirer des favoris" else "Ajouter aux favoris",
                            tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { localShowFR = !showFR }) {
                        Icon(
                            Icons.Outlined.RecordVoiceOver,
                            contentDescription = if (showFR) "Masquer la traduction" else "Afficher la traduction",
                            tint = if (showFR) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            item { IluyNishmatBanner() }

            // Inline translation toggle button (visible sur tablette, mirror V1.10.1 iOS)
            if (isTablet) {
                item {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = androidx.compose.ui.Alignment.CenterEnd) {
                        OutlinedButton(onClick = { localShowFR = !showFR }) {
                            Icon(Icons.Outlined.RecordVoiceOver, null,
                                tint = if (showFR) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurface)
                            Text(
                                if (showFR) "  Masquer la traduction" else "  Afficher la traduction",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            // Titre hébreu du psaume
            if (!psalm.hebrewTitle.isNullOrBlank()) {
                item {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)) {
                        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                text = psalm.hebrewTitle,
                                style = hebrewTitleStyle(textSizeHebrew.scale),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            item { HorizontalDivider() }

            items(psalm.verses) { verse ->
                VerseRow(
                    verse = verse,
                    showTranslation = showFR,
                    textMode = textMode,
                    textSizeHebrew = textSizeHebrew,
                    textSizeFR = textSizeFR,
                    numberStyle = numberStyle,
                    translationLang = appLanguage.translation,
                    sideBySideTranslation = isTablet && isLandscape && showFR,
                    onLongClick = {
                        scope.launch(Dispatchers.IO) {
                            VerseShareRenderer.renderAndShare(
                                context = context,
                                psalm = psalm,
                                verse = verse,
                                translationLang = appLanguage.translation
                            )
                        }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}
