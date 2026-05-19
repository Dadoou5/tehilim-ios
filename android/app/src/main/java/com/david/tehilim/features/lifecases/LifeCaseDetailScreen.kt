package com.david.tehilim.features.lifecases

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.TextSnippet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.david.tehilim.AppContainer
import com.david.tehilim.R
import com.david.tehilim.core.model.LifeCase
import com.david.tehilim.core.model.Prayer
import com.david.tehilim.features.prayers.PrayerSheet
import com.david.tehilim.navigation.Routes
import com.david.tehilim.ui.components.AppCard

/**
 * Détail d'un cas de la vie — mirror du LifeCaseDetailView SwiftUI V1.8.1.
 * Header card avec icône + extrait + compteur, boutons prière en cards,
 * liste de Tehilim cliquables.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeCaseDetailScreen(container: AppContainer, caseId: String, navController: NavController) {
    val c = container.lifeCaseRepository.find(caseId) ?: run {
        Text(stringResource(R.string.msg_category_not_found)); return
    }
    var presentedPrayer by remember { mutableStateOf<Prayer.Kind?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(c.title) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header card avec icône colorée + note + compteur
            item { LifeCaseHeaderCard(c) }

            // Bouton prière avant
            item {
                PrayerButtonCard(
                    kind = Prayer.Kind.BEFORE,
                    onClick = { presentedPrayer = Prayer.Kind.BEFORE }
                )
            }

            // Section Tehilim à lire
            item {
                Text(
                    stringResource(R.string.section_psalms_to_read),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(c.psalms) { id ->
                val p = container.psalmRepository.psalm(id) ?: return@items
                AppCard(
                    // Passe la liste de psaumes du cas de la vie comme siblings —
                    // ça active prev/next contextuel sur PsalmDetailScreen.
                    onClick = { navController.navigate(Routes.psalmDetail(id, c.psalms)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                stringResource(R.string.label_psalm_only_number, p.id),
                                style = MaterialTheme.typography.titleSmall,
                                textAlign = TextAlign.Start
                            )
                            Text(
                                p.hebrewNumber,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Start
                            )
                        }
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Bouton prière après
            item {
                PrayerButtonCard(
                    kind = Prayer.Kind.AFTER,
                    onClick = { presentedPrayer = Prayer.Kind.AFTER }
                )
            }

            // Disclaimer footer
            item {
                Text(
                    stringResource(R.string.msg_tradition_disclaimer),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 16.dp)
                )
            }
        }

        // Sheet prière
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
private fun LifeCaseHeaderCard(c: LifeCase) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = LifeCaseSymbolMap.iconFor(c.symbol),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(c.note, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = if (c.psalms.size > 1)
                        stringResource(R.string.label_psalms_recommended_plural, c.psalms.size)
                    else
                        stringResource(R.string.label_psalms_recommended_singular, c.psalms.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun PrayerButtonCard(kind: Prayer.Kind, onClick: () -> Unit) {
    val icon = when (kind) {
        Prayer.Kind.BEFORE -> Icons.Outlined.PlayCircle
        Prayer.Kind.AFTER -> Icons.Outlined.CheckCircle
    }
    val title = stringResource(kind.titleRes)
    val subtitle = stringResource(kind.subtitleRes)
    AppCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
