package com.david.tehilim.features.psalm119

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.david.tehilim.AppContainer
import com.david.tehilim.R
import com.david.tehilim.core.model.TextMode
import com.david.tehilim.navigation.Routes
import com.david.tehilim.ui.components.IluyNishmatBanner
import com.david.tehilim.ui.components.VerseRow

/**
 * Section du Tehilim 119 — mirror du Psalm119SectionView SwiftUI V1.10.0.
 *
 * Si un `sequenceContext` est fourni (navigation depuis un Lelouy Nichmat) :
 * - Bannière de progression « Lettre X sur N » + source du caractère
 * - Footer prev/next dans la séquence (au lieu de l'alphabet 1..22)
 * - Désactivés aux bornes
 *
 * Sinon : navigation alphabet classique (1 → 22).
 *
 * Note : pour ne pas alourdir la signature, le sequenceContext est passé via
 * `savedStateHandle` (NavController arguments) en V1.2 — pour l'instant on
 * lit le savedIntentId du back stack si présent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Psalm119SectionScreen(
    container: AppContainer,
    index: Int,
    navController: NavController,
    savedIntentId: String? = null,
    sequencePosition: Int? = null
) {
    val section = container.psalm119Repository.sectionAt(index) ?: return
    val psalm = container.psalmRepository.psalm(119) ?: return
    val verses = psalm.verses.filter { it.number in section.versesRange }

    val textMode by container.preferences.textMode.collectAsState(initial = TextMode.HEBREW)
    val textSizeHebrew by container.preferences.textSizeHebrew.collectAsState(initial = com.david.tehilim.core.model.TextSize.MEDIUM)
    val textSizeFR by container.preferences.textSizeFR.collectAsState(initial = com.david.tehilim.core.model.TextSize.MEDIUM)
    val translationFR by container.preferences.translationFR.collectAsState(initial = false)
    val numberStyle by container.preferences.verseNumberStyle.collectAsState(initial = com.david.tehilim.core.model.VerseNumberStyle.HEBREW)
    val appLanguage by container.preferences.appLanguage.collectAsState(initial = com.david.tehilim.core.model.AppLanguage.SYSTEM)

    // Override local pour toggle la traduction sans toucher au global —
    // mirror PsalmDetailScreen. V1.3.12 : avant, la section 119 n'avait pas
    // de bouton et obligeait à passer par Réglages.
    var localShowFR by remember { mutableStateOf<Boolean?>(null) }
    val showFR = localShowFR ?: translationFR

    // Si on lit dans une séquence Lelouy Nichmat sauvegardée, on récupère le contexte.
    val savedIntent = savedIntentId?.let { id ->
        container.savedPrayers.intents.collectAsState().value.firstOrNull { it.id == id }
    }
    val sequenceItems = savedIntent?.generatedLetters
    val sequenceCtx = if (sequenceItems != null && sequencePosition != null) {
        PsalmSequenceContext(items = sequenceItems, currentPosition = sequencePosition, savedIntentId = savedIntentId)
    } else null

    // Mise à jour du lastReadIndex à chaque fois qu'on rentre dans une section depuis une séquence.
    LaunchedEffect(sequenceCtx?.currentPosition) {
        sequenceCtx?.savedIntentId?.let { id ->
            container.savedPrayers.updateLastReadIndex(id, sequenceCtx.currentPosition)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.label_section_letter_name, section.letter, section.name)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { localShowFR = !showFR }) {
                        Icon(
                            Icons.Outlined.RecordVoiceOver,
                            contentDescription = if (showFR)
                                stringResource(R.string.cd_hide_translation)
                            else stringResource(R.string.cd_show_translation),
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
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            item { IluyNishmatBanner() }

            // Bannière de progression séquence (Lelouy Nichmat)
            if (sequenceCtx != null) {
                item { SequenceProgressBanner(sequenceCtx) }
            }

            item {
                Text(
                    stringResource(
                        R.string.label_section_letter_range,
                        section.letter, section.name, section.verseStart, section.verseEnd
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }

            items(verses) { v ->
                VerseRow(
                    verse = v,
                    showTranslation = showFR,
                    textMode = textMode,
                    textSizeHebrew = textSizeHebrew,
                    textSizeFR = textSizeFR,
                    numberStyle = numberStyle,
                    translationLang = appLanguage.translation
                )
                // V1.3.12 — séparateur à peine visible (hairline + alpha 30 %)
                // pour ne pas alourdir visuellement la lecture.
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    thickness = androidx.compose.ui.unit.Dp.Hairline,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
                )
            }

            // Footer prev/next
            item {
                Spacer(Modifier.size(16.dp))
                if (sequenceCtx != null) {
                    SequenceFooter(container, sequenceCtx, savedIntentId, navController)
                } else {
                    AlphabetFooter(index, navController)
                }
            }
        }
    }
}

@Composable
private fun SequenceProgressBanner(ctx: PsalmSequenceContext) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp)
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            Icons.Outlined.FormatListNumbered,
            null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(ctx.progressLabel(context), style = MaterialTheme.typography.titleSmall)
        ctx.currentItem?.let { item ->
            Text(
                stringResource(R.string.label_dot_source, item.source.localizedLabel(context)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SequenceFooter(
    container: AppContainer,
    ctx: PsalmSequenceContext,
    savedIntentId: String?,
    navController: NavController
) {
    val context = LocalContext.current
    val prev = ctx.previousItem()
    val next = ctx.nextItem()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (prev != null) {
            OutlinedButton(onClick = {
                val sectionIdx = container.psalm119Repository.sectionByLetter(prev.psalmLetterKey)?.index ?: 1
                navController.navigate(buildPsalm119Route(sectionIdx, savedIntentId, ctx.currentPosition - 1))
            }) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, null)
                Text(stringResource(R.string.action_previous))
            }
        } else {
            Spacer(Modifier.size(1.dp))
        }

        Text(ctx.progressLabel(context), style = MaterialTheme.typography.labelMedium)

        if (next != null) {
            OutlinedButton(onClick = {
                val sectionIdx = container.psalm119Repository.sectionByLetter(next.psalmLetterKey)?.index ?: 1
                navController.navigate(buildPsalm119Route(sectionIdx, savedIntentId, ctx.currentPosition + 1))
            }) {
                Text(stringResource(R.string.action_next))
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null)
            }
        } else {
            Spacer(Modifier.size(1.dp))
        }
    }
}

@Composable
private fun AlphabetFooter(index: Int, navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (index > 1) {
            OutlinedButton(onClick = {
                navController.navigate(Routes.psalm119Section(index - 1))
            }) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, null)
                Text(stringResource(R.string.action_previous))
            }
        } else {
            Spacer(Modifier.size(1.dp))
        }
        if (index < 22) {
            OutlinedButton(onClick = {
                navController.navigate(Routes.psalm119Section(index + 1))
            }) {
                Text(stringResource(R.string.action_next))
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null)
            }
        } else {
            Spacer(Modifier.size(1.dp))
        }
    }
}

/** Helper pour construire la route Psalm119Section avec query params optionnels. */
private fun buildPsalm119Route(index: Int, savedIntentId: String?, position: Int): String =
    if (savedIntentId != null) {
        "${Routes.psalm119Section(index)}?intentId=$savedIntentId&pos=$position"
    } else {
        Routes.psalm119Section(index)
    }
