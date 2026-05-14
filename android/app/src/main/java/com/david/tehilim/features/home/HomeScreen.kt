package com.david.tehilim.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.david.tehilim.AppContainer
import com.david.tehilim.navigation.Routes
import com.david.tehilim.navigation.TopLevelDestination
import com.david.tehilim.ui.components.AppCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(container: AppContainer, navController: NavController) {
    val favorites by container.favorites.ids.collectAsState()
    val lastReadId by container.preferences.lastReadPsalmId.collectAsState(initial = null)
    val dailyMode by container.preferences.dailyMode.collectAsState(initial = com.david.tehilim.core.model.DailyMode.MONTHLY)

    val todayPsalms = container.dailyEngine.psalmsForToday(dailyMode)

    Scaffold(
        topBar = { TopAppBar(title = { Text("Tehilim") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Date hébraïque — placeholder V1, mettre HebrewDateFormatter Kotlin port plus tard
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = java.time.LocalDate.now()
                            .format(java.time.format.DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", java.util.Locale.FRENCH)),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Calendrier hébraïque (V1)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Reprendre la lecture
            if (lastReadId != null) {
                item {
                    SectionHeader("Reprendre la lecture")
                    AppCard(onClick = { navController.navigate(Routes.psalmDetail(lastReadId!!)) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tehilim $lastReadId", style = MaterialTheme.typography.titleMedium)
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null)
                        }
                    }
                }
            }

            // Mes favoris
            item {
                SectionHeader("Mes favoris")
                AppCard(onClick = { navController.navigate(Routes.PSALM_LIST_FAVORITES) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null)
                    }
                }
            }

            // Tehilim du jour
            item {
                SectionHeader("Tehilim du jour", subtitle = dailyMode.label)
                AppCard(onClick = { navController.navigate(TopLevelDestination.Daily.route) }) {
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)) {
                        if (todayPsalms.isEmpty()) {
                            Text("Aucun Tehilim défini pour ce mode.", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text(
                                text = todayPsalms.take(8).joinToString(" · "),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (todayPsalms.size > 8) {
                                Spacer(Modifier.size(4.dp))
                                Text(
                                    text = "+${todayPsalms.size - 8}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Explorer
            item { SectionHeader("Explorer") }
            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(0.dp),
                    userScrollEnabled = false
                ) {
                    items(exploreCards(navController)) { card ->
                        ExploreCard(symbol = card.icon, title = card.title, onClick = card.onClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ExploreCard(symbol: ImageVector, title: String, onClick: () -> Unit) {
    AppCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(symbol, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleSmall)
        }
    }
}

private data class ExploreCardSpec(val icon: ImageVector, val title: String, val onClick: () -> Unit)

private fun exploreCards(nav: NavController): List<ExploreCardSpec> = listOf(
    ExploreCardSpec(Icons.Outlined.MenuBook, "5 livres") { nav.navigate(TopLevelDestination.Psalms.route) },
    ExploreCardSpec(Icons.Outlined.Favorite, "Cas de la vie") { nav.navigate(TopLevelDestination.LifeCases.route) },
    ExploreCardSpec(Icons.Outlined.TextFields, "119 - AlphaBeta") { nav.navigate(Routes.PSALM_119_HOME) },
    ExploreCardSpec(Icons.Outlined.AutoStories, "Tous (1–150)") { nav.navigate(Routes.PSALM_LIST_ALL) },
    ExploreCardSpec(Icons.Outlined.PlayCircle, "Prière avant") { /* TODO: prayer sheet */ },
    ExploreCardSpec(Icons.Outlined.CheckCircle, "Prière après") { /* TODO: prayer sheet */ }
)
