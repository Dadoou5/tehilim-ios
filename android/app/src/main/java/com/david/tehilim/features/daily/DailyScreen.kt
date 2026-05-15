package com.david.tehilim.features.daily

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.david.tehilim.AppContainer
import com.david.tehilim.core.model.DailyMode
import com.david.tehilim.core.model.Prayer
import com.david.tehilim.features.prayers.PrayerSheet
import com.david.tehilim.navigation.Routes
import com.david.tehilim.ui.components.AppCard

/**
 * Écran « Aujourd'hui » — mirror DailyView.swift iOS.
 *
 * V1.2.4 : ajout des sections Prière avant / Prière après autour de la liste
 * des Tehilim du jour, comme sur iOS.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyScreen(container: AppContainer, navController: NavController) {
    val mode by container.preferences.dailyMode.collectAsState(initial = DailyMode.MONTHLY)
    val ids = container.dailyEngine.psalmsForToday(mode)
    var presentedPrayer by remember { mutableStateOf<Prayer.Kind?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text("Aujourd'hui") }) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Mode : ${mode.label} · ${ids.size} Tehilim",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Section : Prière avant la lecture (mirror iOS)
            item {
                PrayerRow(
                    title = "Prière avant la lecture",
                    icon = Icons.Outlined.PlayCircle,
                    onClick = { presentedPrayer = Prayer.Kind.BEFORE }
                )
            }

            // Section : Au programme
            item {
                Text(
                    text = "Au programme",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(ids) { id ->
                val p = container.psalmRepository.psalm(id) ?: return@items
                AppCard(
                    // Siblings = liste des Tehilim du jour → prev/next dans le set.
                    onClick = { navController.navigate(Routes.psalmDetail(id, ids)) },
                    modifier = Modifier.fillMaxWidth()) {
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

            if (ids.isEmpty()) {
                item {
                    Text(
                        "Aucun Tehilim défini pour ce jour.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Section : Prière après la lecture (mirror iOS)
            item {
                PrayerRow(
                    title = "Prière après la lecture",
                    icon = Icons.Outlined.CheckCircle,
                    onClick = { presentedPrayer = Prayer.Kind.AFTER }
                )
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
