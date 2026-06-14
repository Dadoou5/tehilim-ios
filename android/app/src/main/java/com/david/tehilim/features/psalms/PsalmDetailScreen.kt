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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.TextDecrease
import androidx.compose.material.icons.outlined.TextIncrease
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.david.tehilim.AppContainer
import com.david.tehilim.R
import com.david.tehilim.core.model.TextMode
import com.david.tehilim.features.sharing.VerseShareRenderer
import com.david.tehilim.navigation.Routes
import com.david.tehilim.ui.components.IluyNishmatBanner
import com.david.tehilim.ui.components.VerseRow
import com.david.tehilim.ui.theme.EzraSilFontFamily
import com.david.tehilim.ui.theme.hebrewTitleStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PsalmDetailScreen(
    container: AppContainer,
    psalmId: Int,
    navController: NavController,
    siblings: List<Int>? = null
) {
    val psalm = container.psalmRepository.psalm(psalmId)
    if (psalm == null) {
        Text(stringResource(R.string.msg_psalm_not_found))
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
    val showCommentaries by container.preferences.showCommentaries.collectAsState(initial = false)
    val numberStyle by container.preferences.verseNumberStyle.collectAsState(initial = com.david.tehilim.core.model.VerseNumberStyle.HEBREW)
    val appLanguage by container.preferences.appLanguage.collectAsState(initial = com.david.tehilim.core.model.AppLanguage.SYSTEM)

    // Override local pour toggle la traduction sans toucher au global
    var localShowFR by remember { mutableStateOf<Boolean?>(null) }
    val showFR = localShowFR ?: translationFR
    // Menu de taille du texte (contrôle A− / A+ en lecture).
    var showTextSize by remember { mutableStateOf(false) }

    LaunchedEffect(psalmId) {
        container.preferences.setLastReadPsalmId(psalmId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.label_psalm_with_hebrew, psalm.id, psalm.hebrewNumber)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        container.favorites.toggle(psalmId)
                    }) {
                        Icon(
                            if (isFav) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isFav)
                                stringResource(R.string.cd_remove_favorite)
                            else stringResource(R.string.cd_add_favorite),
                            tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { localShowFR = !showFR }) {
                        Icon(
                            Icons.Outlined.RecordVoiceOver,
                            contentDescription = if (showFR)
                                stringResource(R.string.cd_hide_translation)
                            else stringResource(R.string.cd_show_translation),
                            tint = if (showFR) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // Taille du texte : bouton « Aa » + menu A− / A+ (persisté).
                    Box {
                        IconButton(onClick = { showTextSize = true }) {
                            Icon(Icons.Outlined.FormatSize,
                                contentDescription = stringResource(R.string.reading_text_size))
                        }
                        DropdownMenu(expanded = showTextSize, onDismissRequest = { showTextSize = false }) {
                            Column(
                                Modifier.padding(horizontal = 16.dp, vertical = 8.dp).width(220.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(stringResource(R.string.reading_text_size),
                                    style = MaterialTheme.typography.titleSmall)
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                    IconButton(
                                        onClick = { scope.launch {
                                            container.preferences.setTextSizeHebrew(textSizeHebrew.stepped(-1))
                                            if (showFR) container.preferences.setTextSizeFR(textSizeFR.stepped(-1))
                                        } },
                                        enabled = !textSizeHebrew.isSmallest
                                    ) { Icon(Icons.Outlined.TextDecrease, stringResource(R.string.reading_text_smaller)) }

                                    Text("Aa", fontSize = (17 * textSizeHebrew.scale).sp,
                                        fontWeight = FontWeight.SemiBold)

                                    IconButton(
                                        onClick = { scope.launch {
                                            container.preferences.setTextSizeHebrew(textSizeHebrew.stepped(1))
                                            if (showFR) container.preferences.setTextSizeFR(textSizeFR.stepped(1))
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

            // Numéro du Tehilim toujours visible dans le contenu (le titre de la
            // barre peut être tronqué par les boutons sur petit écran).
            item {
                Column(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.label_psalm_number, psalm.id),
                        style = MaterialTheme.typography.titleMedium)
                    Text(psalm.hebrewNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

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
                                if (showFR) stringResource(R.string.cd_hide_translation_inline)
                                else stringResource(R.string.cd_show_translation_inline),
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

            // Temps de lecture approximatif.
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Schedule, null, modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.size(4.dp))
                    Text("~${psalm.estimatedReadingMinutes} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    // V2.4 — en mode étude : colonne unique pour que les
                    // commentaires se déplient en pleine largeur sous le verset.
                    sideBySideTranslation = isTablet && isLandscape && showFR && !showCommentaries,
                    commentaries = if (showCommentaries)
                        container.commentaryRepository.comments(psalm.id, verse.number) else emptyList(),
                    showCommentaries = showCommentaries,
                    commentaryCode = when (appLanguage.content) {
                        com.david.tehilim.core.model.ContentLanguage.FR -> "fr"
                        com.david.tehilim.core.model.ContentLanguage.EN -> "en"
                        com.david.tehilim.core.model.ContentLanguage.HE -> "he"
                    },
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
                // V1.3.12 — séparateur à peine visible (hairline + alpha 30 %)
                // pour ne pas alourdir visuellement la lecture.
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    thickness = androidx.compose.ui.unit.Dp.Hairline,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
                )
            }

            // Footer prev/next — mirror PsalmDetailView.navigation(prev:next:) iOS.
            // Si on a une liste de siblings (cas de vie, favoris, journée…), on
            // navigue dans cette liste. Sinon, dans le corpus complet 1..150.
            item {
                val (prev, next) = computeNeighbors(psalmId, siblings)
                PrevNextFooter(
                    prevId = prev,
                    nextId = next,
                    onClick = { targetId ->
                        // Pop l'entrée courante puis push la nouvelle pour éviter
                        // l'accumulation infinie dans la back stack.
                        navController.navigate(Routes.psalmDetail(targetId, siblings)) {
                            popUpTo(Routes.PSALM_DETAIL) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

private fun computeNeighbors(id: Int, siblings: List<Int>?): Pair<Int?, Int?> {
    if (siblings != null) {
        val idx = siblings.indexOf(id)
        if (idx >= 0) {
            val prev = if (idx > 0) siblings[idx - 1] else null
            val next = if (idx < siblings.size - 1) siblings[idx + 1] else null
            return prev to next
        }
    }
    val prev = if (id > 1) id - 1 else null
    val next = if (id < 150) id + 1 else null
    return prev to next
}

@Composable
private fun PrevNextFooter(prevId: Int?, nextId: Int?, onClick: (Int) -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (prevId != null) {
            OutlinedButton(onClick = { onClick(prevId) }) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, null)
                Text(stringResource(R.string.label_psalm_button_prev, prevId))
            }
        } else {
            androidx.compose.foundation.layout.Spacer(Modifier.padding(1.dp))
        }
        if (nextId != null) {
            OutlinedButton(onClick = { onClick(nextId) }) {
                Text(stringResource(R.string.label_psalm_button, nextId))
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null)
            }
        } else {
            androidx.compose.foundation.layout.Spacer(Modifier.padding(1.dp))
        }
    }
}
