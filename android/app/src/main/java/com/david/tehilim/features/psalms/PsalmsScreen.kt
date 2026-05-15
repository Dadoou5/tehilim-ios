package com.david.tehilim.features.psalms

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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.david.tehilim.AppContainer
import com.david.tehilim.navigation.Routes

/**
 * Onglet Tehilim — équivalent du PsalmsTabView iOS.
 *
 * 3 segments : Livres (5 livres) / Tous (150) / Favoris.
 * Sur Android, on utilise Material 3 PrimaryTabRow au lieu du SegmentedPicker iOS.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PsalmsScreen(
    container: AppContainer,
    navController: NavController,
    initialSegment: Int = 0
) {
    var segment by rememberSaveable(initialSegment) { mutableIntStateOf(initialSegment) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Tehilim") }) }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {

            PrimaryTabRow(selectedTabIndex = segment) {
                listOf("Livres", "Tous", "Favoris").forEachIndexed { i, label ->
                    Tab(
                        selected = segment == i,
                        onClick = { segment = i },
                        text = { Text(label) }
                    )
                }
            }

            when (segment) {
                0 -> BookListContent(container, navController)
                1 -> AllPsalmsContent(container, navController)
                else -> FavoritesContent(container, navController)
            }
        }
    }
}

// MARK: - Livres

@Composable
private fun BookListContent(container: AppContainer, navController: NavController) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items((1..5).toList()) { book ->
            val range = com.david.tehilim.core.model.Psalm.bookRanges[book]
            val count = if (range != null) range.last - range.first + 1 else 0
            BookRow(
                book = book,
                rangeLabel = "${range?.first}–${range?.last}",
                count = count,
                onClick = { navController.navigate(Routes.psalmListBook(book)) }
            )
        }
    }
}

@Composable
private fun BookRow(book: Int, rangeLabel: String, count: Int, onClick: () -> Unit) {
    com.david.tehilim.ui.components.AppCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Livre $book", style = MaterialTheme.typography.titleMedium)
                Text(rangeLabel, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "$count Tehilim",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// MARK: - Tous

@Composable
private fun AllPsalmsContent(container: AppContainer, navController: NavController) {
    var query by rememberSaveable { mutableStateOf("") }
    val all = container.psalmRepository.allPsalms

    // Filtre live partagé avec PsalmListScreen (livres 1..5)
    val filtered = remember(query, all) { filterPsalms(all, query) }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Rechercher (23, כג, mot du titre…)") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Effacer")
                    }
                }
            } else null,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(top = 32.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    "Aucun Tehilim ne correspond à « $query ».",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Column
        }

        val visibleIds = remember(filtered) { filtered.map { it.id } }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(filtered) { psalm ->
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // Siblings = liste filtrée visible (prev/next dans le sous-set)
                            navController.navigate(
                                com.david.tehilim.navigation.Routes.psalmDetail(psalm.id, visibleIds)
                            )
                        }
                ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${psalm.id}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        psalm.hebrewNumber,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tehilim ${psalm.id}", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${psalm.verses.size} versets",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider()
        }
        }
    }
}

// MARK: - Favoris

@Composable
private fun FavoritesContent(container: AppContainer, navController: NavController) {
    val favorites by container.favorites.ids.collectAsState()
    val sortedIds = remember(favorites) { favorites.sorted() }
    var presentedPrayer by remember { mutableStateOf<com.david.tehilim.core.model.Prayer.Kind?>(null) }

    if (favorites.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Aucun favori", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Tape ♡ sur un Tehilim pour l'ajouter ici.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        // Prière avant la lecture (mirror FavoritesScreen dédié)
        item {
            PrayerRow(
                title = "Prière avant la lecture",
                icon = Icons.Outlined.PlayCircle,
                onClick = { presentedPrayer = com.david.tehilim.core.model.Prayer.Kind.BEFORE }
            )
        }
        item {
            Text(
                "Tehilim favoris",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )
        }

        items(sortedIds) { id ->
            val psalm = container.psalmRepository.psalm(id) ?: return@items
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // Siblings = favoris triés → prev/next dans la liste.
                        navController.navigate(
                            com.david.tehilim.navigation.Routes.psalmDetail(psalm.id, sortedIds)
                        )
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tehilim ${psalm.id} · ${psalm.hebrewNumber}",
                        style = MaterialTheme.typography.bodyLarge)
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider()
        }

        // Prière après la lecture
        item {
            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 12.dp))
            PrayerRow(
                title = "Prière après la lecture",
                icon = Icons.Outlined.CheckCircle,
                onClick = { presentedPrayer = com.david.tehilim.core.model.Prayer.Kind.AFTER }
            )
        }
    }

    presentedPrayer?.let { kind ->
        com.david.tehilim.features.prayers.PrayerSheet(
            kind = kind,
            container = container,
            onDismiss = { presentedPrayer = null }
        )
    }
}

@Composable
private fun PrayerRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    com.david.tehilim.ui.components.AppCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

