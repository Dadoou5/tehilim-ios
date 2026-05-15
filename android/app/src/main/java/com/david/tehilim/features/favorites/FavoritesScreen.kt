package com.david.tehilim.features.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PlayCircle
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.david.tehilim.AppContainer
import com.david.tehilim.core.model.Prayer
import com.david.tehilim.features.prayers.PrayerSheet
import com.david.tehilim.navigation.Routes
import com.david.tehilim.ui.components.AppCard

/**
 * Écran « Mes favoris » dédié — mirror FavoritesListView.swift V1.9.5.
 *
 * Sections (mirror iOS) :
 *   1. Prière avant la lecture
 *   2. Tehilim favoris (triés par id croissant)
 *   3. Prière après la lecture
 *
 * Si aucune favorite, on affiche un empty state avec icône cœur.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(container: AppContainer, navController: NavController) {
    val favorites by container.favorites.ids.collectAsState()
    val sortedIds = remember(favorites) { favorites.sorted() }
    var presentedPrayer by remember { mutableStateOf<Prayer.Kind?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mes favoris") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { padding ->
        if (sortedIds.isEmpty()) {
            EmptyState(modifier = Modifier.fillMaxSize().padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Prière avant
                item {
                    PrayerRow(
                        title = "Prière avant la lecture",
                        icon = Icons.Outlined.PlayCircle,
                        onClick = { presentedPrayer = Prayer.Kind.BEFORE }
                    )
                }

                item {
                    Text(
                        "Tehilim favoris",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(sortedIds) { id ->
                    val p = container.psalmRepository.psalm(id) ?: return@items
                    AppCard(
                        // Siblings = toute la liste de favoris (triés) →
                        // prev/next dans PsalmDetail navigue entre favoris.
                        onClick = { navController.navigate(Routes.psalmDetail(id, sortedIds)) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Tehilim ${p.id} · ${p.hebrewNumber}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null)
                        }
                    }
                }

                // Prière après
                item {
                    PrayerRow(
                        title = "Prière après la lecture",
                        icon = Icons.Outlined.CheckCircle,
                        onClick = { presentedPrayer = Prayer.Kind.AFTER }
                    )
                }
            }
        }

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
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.FavoriteBorder,
            null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Aucun favori",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 16.dp),
            textAlign = TextAlign.Center
        )
        Text(
            "Tape sur le cœur dans un Tehilim pour l'ajouter ici.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun PrayerRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    AppCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null)
        }
    }
}
