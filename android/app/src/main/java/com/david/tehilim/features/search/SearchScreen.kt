package com.david.tehilim.features.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.david.tehilim.AppContainer
import com.david.tehilim.R
import com.david.tehilim.core.model.Psalm
import com.david.tehilim.navigation.Routes
import kotlinx.coroutines.launch

/**
 * Recherche Tehilim — mirror SearchView.swift V1.10.5.
 *
 * Sections (dans l'ordre) :
 *   1. Résultat exact (si trouvé) — en évidence
 *   2. État vide (« Aucun Tehilim trouvé ») si query non vide sans match
 *   3. Suggestions (voisins ou liste par défaut)
 *   4. Récents (max 10, persistés DataStore)
 *
 * Le matching gère numéros arabes ("23"), hébraïques ("כג") et le préfixe
 * « tehilim/psaume » via SearchInterpreter.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(container: AppContainer, navController: NavController) {
    var query by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val recents by container.preferences.searchRecents.collectAsState(initial = emptyList())
    val appLanguage by container.preferences.appLanguage.collectAsState(
        initial = com.david.tehilim.core.model.AppLanguage.SYSTEM
    )

    val result by remember(query, appLanguage) {
        derivedStateOf { container.searchInterpreter.interpret(query, appLanguage.translation) }
    }

    fun openPsalm(id: Int) {
        scope.launch { container.preferences.rememberSearchRecent(id) }
        navController.popBackStack()
        navController.navigate(Routes.psalmDetail(id))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_search)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.cd_close))
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
                label = { Text(stringResource(R.string.placeholder_search_short)) },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 1) Résultat exact
                result.exactMatch?.let { exact ->
                    item { SectionHeader(stringResource(R.string.section_result)) }
                    item { ResultRow(psalm = exact, primary = true, onClick = { openPsalm(exact.id) }) }
                    item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }
                }

                // 2) Empty state quand query non vide sans aucun match
                if (result.exactMatch == null && result.textMatches.isEmpty() && query.isNotBlank()) {
                    item {
                        Text(
                            stringResource(R.string.msg_no_psalm_found),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }

                // 2b) Occurrences dans le texte (V2.3)
                if (result.textMatches.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.section_in_text, result.textMatches.size)) }
                    items(result.textMatches) { m ->
                        TextMatchRow(m) { openPsalm(m.psalm.id) }
                        HorizontalDivider()
                    }
                }

                // 3) Suggestions
                if (result.suggestions.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.section_suggestions)) }
                    items(result.suggestions) { p ->
                        ResultRow(psalm = p, primary = false, onClick = { openPsalm(p.id) })
                        HorizontalDivider()
                    }
                }

                // 4) Récents — uniquement quand query est vide pour ne pas polluer l'UI
                if (query.isBlank() && recents.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.section_recents), icon = Icons.Outlined.History) }
                    items(recents) { id ->
                        val p = container.psalmRepository.psalm(id) ?: return@items
                        ResultRow(psalm = p, primary = false, onClick = { openPsalm(p.id) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TextMatchRow(m: com.david.tehilim.core.service.VerseTextMatch, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.label_verse_ref, m.psalm.id, m.verse.number),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                m.snippet,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun ResultRow(psalm: Psalm, primary: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.label_psalm_with_hebrew, psalm.id, psalm.hebrewNumber),
                style = if (primary) MaterialTheme.typography.titleMedium
                        else MaterialTheme.typography.bodyLarge,
                fontWeight = if (primary) FontWeight.SemiBold else FontWeight.Normal,
                color = if (primary) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
            )
            psalm.hebrewTitle?.takeIf { it.isNotBlank() }?.let { title ->
                Text(
                    title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
