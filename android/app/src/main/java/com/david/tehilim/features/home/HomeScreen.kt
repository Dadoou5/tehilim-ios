package com.david.tehilim.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TextFields
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.david.tehilim.AppContainer
import com.david.tehilim.core.model.Prayer
import com.david.tehilim.features.prayers.PrayerSheet
import com.david.tehilim.navigation.Routes
import com.david.tehilim.navigation.TopLevelDestination
import com.david.tehilim.ui.components.AppCard
import com.david.tehilim.ui.components.HebrewDateBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(container: AppContainer, navController: NavController) {
    val favorites by container.favorites.ids.collectAsState()
    val lastReadId by container.preferences.lastReadPsalmId.collectAsState(initial = null)
    val dailyMode by container.preferences.dailyMode.collectAsState(initial = com.david.tehilim.core.model.DailyMode.MONTHLY)

    val todayPsalms = container.dailyEngine.psalmsForToday(dailyMode)
    var presentedPrayer by remember { mutableStateOf<Prayer.Kind?>(null) }

    val isTablet = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600
    val explorerCols = if (isTablet) 3 else 2

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tehilim") },
                actions = {
                    IconButton(onClick = { navController.navigate("search") }) {
                        Icon(Icons.Outlined.Search, "Rechercher")
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Date hébraïque
            item { HebrewDateBanner() }

            // Reprendre la lecture
            if (lastReadId != null) {
                item {
                    SectionHeader("Reprendre la lecture")
                    AppCard(onClick = { navController.navigate(Routes.psalmDetail(lastReadId!!)) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tehilim $lastReadId", style = MaterialTheme.typography.titleMedium)
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null)
                        }
                    }
                }
            }

            // Mes favoris
            item {
                SectionHeader("Mes favoris")
                AppCard(onClick = { navController.navigate(Routes.PSALM_LIST_FAVORITES) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Favorite, null, tint = MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when (favorites.size) {
                                    0 -> "Aucun favori"
                                    1 -> "1 Tehilim sauvegardé"
                                    else -> "${favorites.size} Tehilim sauvegardés"
                                },
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (favorites.isEmpty())
                                    "Tape ♡ sur un Tehilim pour l'ajouter ici"
                                else "Voir la liste",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null)
                    }
                }
            }

            // Tehilim du jour
            item {
                SectionHeader("Tehilim du jour", subtitle = dailyMode.label)
                AppCard(onClick = { navController.switchTopLevel(TopLevelDestination.Daily.route) }) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        if (todayPsalms.isEmpty()) {
                            Text("Aucun Tehilim défini pour ce mode.", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text(
                                todayPsalms.take(8).joinToString(" · "),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (todayPsalms.size > 8) {
                                Text(
                                    "+${todayPsalms.size - 8}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Explorer — grille 2 colonnes construite manuellement (pas de LazyVerticalGrid
            // dans un LazyColumn : ça casse les contraintes verticales).
            item { SectionHeader("Explorer") }
            item {
                val cards = exploreCards(navController) { presentedPrayer = it }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    cards.chunked(explorerCols).forEach { rowCards ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            rowCards.forEach { card ->
                                Box(modifier = Modifier.weight(1f)) {
                                    ExploreCard(
                                        symbol = card.icon,
                                        title = card.title,
                                        onClick = card.onClick
                                    )
                                }
                            }
                            // Pad pour équilibrer si dernière ligne incomplète
                            repeat(explorerCols - rowCards.size) {
                                Box(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Sheets prières
        presentedPrayer?.let { kind ->
            PrayerSheet(
                kind = kind,
                container = container,
                onDismiss = { presentedPrayer = null }
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ExploreCard(symbol: ImageVector, title: String, onClick: () -> Unit) {
    AppCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(symbol, null, tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleSmall)
        }
    }
}

private data class ExploreCardSpec(val icon: ImageVector, val title: String, val onClick: () -> Unit)

private fun exploreCards(
    nav: NavController,
    onPrayer: (Prayer.Kind) -> Unit
): List<ExploreCardSpec> = listOf(
    // V1.2.1 : pour les top-level destinations, on utilise switchTopLevel pour
    // que la navigation passe par le pattern tab proprement (et ne push pas
    // Psalms par-dessus Home, ce qui causait que tapping Accueil affiche Psalms).
    ExploreCardSpec(Icons.AutoMirrored.Outlined.MenuBook, "5 livres") { nav.switchTopLevel(TopLevelDestination.Psalms.route) },
    ExploreCardSpec(Icons.Outlined.Favorite, "Cas de la vie") { nav.switchTopLevel(TopLevelDestination.LifeCases.route) },
    ExploreCardSpec(Icons.Outlined.TextFields, "119 - AlphaBeta") { nav.navigate(Routes.PSALM_119_HOME) },
    ExploreCardSpec(Icons.Outlined.AutoStories, "Tous (1–150)") { nav.navigate(Routes.PSALM_LIST_ALL) },
    ExploreCardSpec(Icons.Outlined.PlayCircle, "Prière avant") { onPrayer(Prayer.Kind.BEFORE) },
    ExploreCardSpec(Icons.Outlined.CheckCircle, "Prière après") { onPrayer(Prayer.Kind.AFTER) }
)

/**
 * Helper pour passer d'un onglet à l'autre proprement (sans empiler).
 * Mirror du `router.go(.psalms)` iOS.
 */
private fun NavController.switchTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
            inclusive = false
        }
        launchSingleTop = true
        restoreState = true
    }
}
