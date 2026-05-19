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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.david.tehilim.AppContainer
import com.david.tehilim.R
import com.david.tehilim.core.model.Psalm
import com.david.tehilim.core.service.HebrewNumerals
import com.david.tehilim.navigation.Routes

/**
 * Liste des Tehilim d'un livre (1..5) avec recherche live, mirror de
 * l'UX du segment « Tous » de l'onglet Tehilim.
 *
 * V1.2.13 :
 *   - barre de recherche en tête (filtre arabe/hébreu/titre)
 *   - clic sur toute la ligne pour ouvrir le Tehilim (plus de bouton « Lire »)
 *   - chevron à droite + cœur si favori
 *   - siblings = liste filtrée visible → Précédent/Suivant contextuels
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PsalmListScreen(
    container: AppContainer,
    book: Int?,
    favoritesOnly: Boolean = false,
    navController: NavController
) {
    val favorites by container.favorites.ids.collectAsState()
    val basePsalms = when {
        favoritesOnly -> favorites.sorted().mapNotNull { container.psalmRepository.psalm(it) }
        book != null -> container.psalmRepository.psalmsInBook(book)
        else -> container.psalmRepository.allPsalms
    }
    val title = when {
        favoritesOnly -> stringResource(R.string.tab_favorites)
        book != null -> stringResource(R.string.label_book, book)
        else -> stringResource(R.string.title_all_psalms)
    }

    var query by rememberSaveable { mutableStateOf("") }

    val filtered = remember(query, basePsalms) {
        filterPsalms(basePsalms, query)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.placeholder_search_psalms)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.cd_clear))
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
                        if (query.isBlank()) stringResource(R.string.msg_no_psalm_in_book)
                        else stringResource(R.string.msg_no_psalm_matches, query),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                return@Column
            }

            // Siblings = liste filtrée → prev/next contextuels dans le sous-set.
            val visibleIds = remember(filtered) { filtered.map { it.id } }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(filtered) { psalm ->
                    PsalmRow(
                        psalm = psalm,
                        isFavorite = favorites.contains(psalm.id),
                        onClick = {
                            navController.navigate(Routes.psalmDetail(psalm.id, visibleIds))
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun PsalmRow(psalm: Psalm, isFavorite: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
            Text(stringResource(R.string.label_psalm_only_number, psalm.id), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.label_verses_count, psalm.verses.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isFavorite) {
            Icon(
                Icons.Outlined.Favorite,
                contentDescription = stringResource(R.string.cd_favorite),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Filtre live partagé entre `PsalmListScreen` (livre détaillé) et le
 * segment « Tous » de l'onglet Tehilim. Numéro arabe, gematria hébraïque,
 * sous-chaîne dans hebrewNumber ou hebrewTitle, préfixe par numéro arabe.
 * Strip-words « tehilim/psaume/... » côté query.
 */
internal fun filterPsalms(all: List<Psalm>, query: String): List<Psalm> {
    if (query.isBlank()) return all
    val raw = query.trim()
    val lower = raw.lowercase()
    val cleaned = listOf("tehilim", "tehillim", "psaume", "psaumes", "psalm")
        .fold(lower) { acc, w -> acc.replace(w, "") }
        .trim()
    val arabicNum = cleaned.filter { it.isDigit() }.toIntOrNull()
    val hebrewNum = HebrewNumerals.toInt(raw)
    val prefix = cleaned.takeIf { it.isNotEmpty() } ?: " "
    return all.filter { p ->
        (arabicNum != null && p.id == arabicNum) ||
            (hebrewNum != null && p.id == hebrewNum) ||
            p.hebrewNumber.contains(raw) ||
            (p.hebrewTitle?.contains(raw) == true) ||
            p.id.toString().startsWith(prefix)
    }
}
