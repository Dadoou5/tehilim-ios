package com.david.tehilim.features.daily

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.david.tehilim.AppContainer
import com.david.tehilim.core.model.DailyMode
import com.david.tehilim.navigation.Routes
import com.david.tehilim.ui.components.AppCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyScreen(container: AppContainer, navController: NavController) {
    val mode by container.preferences.dailyMode.collectAsState(initial = DailyMode.MONTHLY)
    val ids = container.dailyEngine.psalmsForToday(mode)

    Scaffold(topBar = { TopAppBar(title = { Text("Tehilim du jour") }) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text(
                    text = "Mode : ${mode.label} · ${ids.size} Tehilim",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            items(ids) { id ->
                val p = container.psalmRepository.psalm(id) ?: return@items
                AppCard(onClick = { navController.navigate(Routes.psalmDetail(id)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)) {
                    Text(
                        "Tehilim ${p.id} · ${p.hebrewNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
