package com.david.tehilim.features.lifecases

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.david.tehilim.AppContainer
import com.david.tehilim.core.model.TranslationLanguage
import com.david.tehilim.navigation.Routes
import com.david.tehilim.ui.components.AppCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeCasesScreen(container: AppContainer, navController: NavController) {
    val groups = container.lifeCaseRepository.grouped(TranslationLanguage.FR)

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
                    Text(group.title, style = MaterialTheme.typography.titleMedium)
                }
                items(group.cases) { c ->
                    AppCard(
                        onClick = { navController.navigate(Routes.lifeCaseDetail(c.id)) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(c.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${c.psalms.size} Tehilim",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                c.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
