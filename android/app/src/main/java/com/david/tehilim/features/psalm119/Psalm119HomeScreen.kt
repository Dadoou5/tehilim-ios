package com.david.tehilim.features.psalm119

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.david.tehilim.AppContainer
import com.david.tehilim.R
import com.david.tehilim.navigation.Routes
import com.david.tehilim.ui.components.AppCard
import com.david.tehilim.ui.components.HebrewLetterTile

/**
 * AlphaBeta home — mirror du SwiftUI V1.10.0 :
 * - Section « Lelouy Nichmat » avec 2 cards (Nouvelle lecture + Mes prières)
 * - Header card explicatif sur tablette
 * - Grille des 22 lettres : 4 cols (compact) / 6 cols (tablette regular)
 * - HebrewLetterTile enrichie (lettre + nom phonétique + range versets)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Psalm119HomeScreen(container: AppContainer, navController: NavController) {
    val sections = container.psalm119Repository.sections
    val savedPrayers by container.savedPrayers.intents.collectAsState()

    val isTablet = LocalConfiguration.current.screenWidthDp >= 600
    val columnsCount = if (isTablet) 6 else 4

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_psalm_119_home)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.cd_back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(if (isTablet) 20.dp else 16.dp)
        ) {
            // Header card de contexte sur tablette uniquement
            if (isTablet) {
                item {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                stringResource(R.string.title_psalm_119_full),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                stringResource(R.string.msg_psalm_119_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }

            // Section Lelouy Nichmat — 2 cards (nouvelle lecture + mes prières)
            item {
                Text(
                    stringResource(R.string.title_lelouy_nichmat),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            item {
                val cardCount = if (isTablet) 2 else 1
                val rows = listOf(
                    ActionCardSpec(
                        symbol = Icons.Outlined.LocalFireDepartment,
                        title = stringResource(R.string.msg_new_reading),
                        subtitle = stringResource(R.string.msg_new_reading_subtitle),
                        accent = true,
                        onClick = { navController.navigate(Routes.PERSONALIZED_FORM) }
                    ),
                    ActionCardSpec(
                        symbol = Icons.Outlined.Inbox,
                        title = stringResource(R.string.title_my_prayers),
                        subtitle = savedCountLabel(savedPrayers.size),
                        accent = false,
                        onClick = { navController.navigate(Routes.SAVED_PRAYERS) }
                    )
                ).chunked(cardCount)

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    rows.forEach { rowCards ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            rowCards.forEach { card ->
                                Box(modifier = Modifier.weight(1f)) {
                                    ActionCard(card)
                                }
                            }
                            if (rowCards.size < cardCount) {
                                repeat(cardCount - rowCards.size) {
                                    Box(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Grille des 22 lettres — chunked en rows
            items(items = sections.chunked(columnsCount), key = { it.first().index }) { rowSections ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 12.dp)
                ) {
                    rowSections.forEach { section ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    navController.navigate(Routes.psalm119Section(section.index))
                                }
                        ) {
                            HebrewLetterTile(
                                letter = section.letter,
                                index = section.index,
                                name = section.name,
                                verseStart = section.verseStart,
                                verseEnd = section.verseEnd
                            )
                        }
                    }
                    repeat(columnsCount - rowSections.size) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private data class ActionCardSpec(
    val symbol: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val subtitle: String,
    val accent: Boolean,
    val onClick: () -> Unit
)

@Composable
private fun ActionCard(spec: ActionCardSpec) {
    AppCard(onClick = spec.onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(end = 2.dp)
            ) {
                Icon(
                    spec.symbol,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(spec.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    spec.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun savedCountLabel(count: Int): String = when (count) {
    0 -> stringResource(R.string.label_saved_none)
    1 -> stringResource(R.string.label_saved_singular)
    else -> stringResource(R.string.label_saved_plural, count)
}

