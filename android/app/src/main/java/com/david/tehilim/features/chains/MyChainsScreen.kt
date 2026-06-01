package com.david.tehilim.features.chains

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.david.tehilim.AppContainer
import com.david.tehilim.core.persistence.ChainArchiveSnapshot
import com.david.tehilim.navigation.Routes
import com.david.tehilim.ui.components.AppCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyChainsScreen(container: AppContainer, navController: NavController) {
    val known by container.chainArchive.knownChainIds.collectAsState()
    val archives by container.chainArchive.archives.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chaînes de Tehilim") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.CHAIN_CREATE) }) {
                        Icon(Icons.Outlined.Add, "Créer une chaîne")
                    }
                }
            )
        }
    ) { padding ->
        if (!container.chains.isAvailable) {
            Column(Modifier.fillMaxSize().padding(padding).padding(32.dp),
                Arrangement.Center, androidx.compose.ui.Alignment.CenterHorizontally) {
                Text("Service indisponible sur cette build.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AppCard(onClick = { navController.navigate(Routes.CHAIN_CREATE) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Créer une chaîne", style = MaterialTheme.typography.titleMedium)
                        Text("Partage le lien sur WhatsApp : chacun choisit les Tehilim qu'il lira, en temps réel.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (known.isNotEmpty()) {
                item { Text("Mes chaînes", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp)) }
                items(known) { id ->
                    val title by produceState<String?>(initialValue = null, id) {
                        value = runCatching { container.chains.fetchChain(id)?.subjectLine }.getOrNull()
                    }
                    AppCard(onClick = { navController.navigate(Routes.chainDetail(id)) }, modifier = Modifier.fillMaxWidth()) {
                        Text(title ?: "Chaîne…", modifier = Modifier.padding(16.dp))
                    }
                }
            }

            if (archives.isNotEmpty()) {
                item { Text("Comptes rendus", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp)) }
                items(archives) { snap ->
                    AppCard(onClick = { shareArchive(context, snap) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(snap.subjectLine, style = MaterialTheme.typography.titleSmall)
                            Text("Touche pour partager le compte rendu",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

private fun shareArchive(context: android.content.Context, snap: ChainArchiveSnapshot) {
    val byName = LinkedHashMap<String, MutableList<Int>>()
    snap.assignments.forEach { (k, name) -> byName.getOrPut(name) { mutableListOf() }.add(k.toIntOrNull() ?: 0) }
    val sb = StringBuilder("Chaîne de Tehilim — ${snap.subjectLine}\n")
    byName.toSortedMap().forEach { (name, ids) -> sb.append("\n• $name : ${ids.sorted().joinToString(", ")}") }
    val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, sb.toString()) }
    context.startActivity(Intent.createChooser(intent, null))
}
