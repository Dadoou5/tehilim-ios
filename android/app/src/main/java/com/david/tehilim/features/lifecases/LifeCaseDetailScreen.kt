package com.david.tehilim.features.lifecases

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.david.tehilim.AppContainer
import com.david.tehilim.navigation.Routes
import com.david.tehilim.ui.components.AppCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeCaseDetailScreen(container: AppContainer, caseId: String, navController: NavController) {
    val c = container.lifeCaseRepository.find(caseId) ?: run {
        Text("Catégorie introuvable")
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(c.title) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text(c.note, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp))
                Text("Tehilim à lire", style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp))
            }
            items(c.psalms) { id ->
                val p = container.psalmRepository.psalm(id) ?: return@items
                AppCard(
                    onClick = { navController.navigate(Routes.psalmDetail(id)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        "Tehilim ${p.id} · ${p.hebrewNumber}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            item {
                Text(
                    "Tradition. Ne remplace pas un avis professionnel.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}
