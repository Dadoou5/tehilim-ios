package com.david.tehilim.features.lifecases

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.david.tehilim.AppContainer
import com.david.tehilim.core.model.LifeCase
import com.david.tehilim.core.model.TranslationLanguage
import com.david.tehilim.navigation.Routes

/**
 * Cas de la vie — grille adaptive 2 cols (compact) / 3 cols (tablette).
 * Mirror du LifeCasesListView SwiftUI V1.8.1.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeCasesScreen(container: AppContainer, navController: NavController) {
    val groups = container.lifeCaseRepository.grouped(TranslationLanguage.FR)
    val columnsCount = if (LocalConfiguration.current.screenWidthDp >= 600) 3 else 2

    Scaffold(topBar = { TopAppBar(title = { Text("Cas de la vie") }) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            groups.forEach { group ->
                item {
                    Text(
                        group.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                // Chunked rows pour la grille adaptive — pas de LazyVerticalGrid imbriqué
                items(group.cases.chunked(columnsCount)) { rowCases ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowCases.forEach { c ->
                            Box(modifier = Modifier.weight(1f)) {
                                LifeCaseCard(
                                    lifeCase = c,
                                    onClick = { navController.navigate(Routes.lifeCaseDetail(c.id)) }
                                )
                            }
                        }
                        // Pad pour aligner si dernière ligne incomplète
                        repeat(columnsCount - rowCases.size) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

