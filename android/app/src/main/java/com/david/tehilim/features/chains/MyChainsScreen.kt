package com.david.tehilim.features.chains

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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.david.tehilim.AppContainer
import com.david.tehilim.R
import com.david.tehilim.core.model.TehilimChain
import com.david.tehilim.navigation.Routes
import com.david.tehilim.ui.components.AppCard
import kotlinx.coroutines.delay

private enum class ChainCategory { SELECTION, READING, CLOSED }

/** Élément unifié de la liste, dérivé du cloud (live) ou d'une archive locale. */
private data class ChainEntry(
    val id: String,
    val title: String,
    val category: ChainCategory,
    val countdownTarget: Long?,   // null si terminée
    val hasArchive: Boolean       // true → on peut ouvrir le lecteur hors-ligne
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyChainsScreen(container: AppContainer, navController: NavController) {
    val known by container.chainArchive.knownChainIds.collectAsState()
    val archives by container.chainArchive.archives.collectAsState()

    // Horloge : recalcule les catégories → bascule auto lecture → terminée.
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }

    // Résolution cloud des chaînes connues (live). Hors-ligne → liste vide, on
    // retombe sur les archives locales.
    val cloud by produceState(initialValue = emptyList<TehilimChain>(), known) {
        value = known.mapNotNull { runCatching { container.chains.fetchChain(it) }.getOrNull() }
    }

    var toDelete by remember { mutableStateOf<ChainEntry?>(null) }

    val archiveIds = archives.map { it.id }.toSet()
    val cloudIds = cloud.map { it.id }.toSet()

    val entries = buildList {
        // À partir du cloud (état live).
        cloud.forEach { c ->
            val cat = when {
                !c.distributed && now < c.selectionDeadlineMillis -> ChainCategory.SELECTION
                now < c.readingDeadlineMillis -> ChainCategory.READING
                else -> ChainCategory.CLOSED
            }
            val target = when (cat) {
                ChainCategory.SELECTION -> c.selectionDeadlineMillis
                ChainCategory.READING -> c.readingDeadlineMillis
                ChainCategory.CLOSED -> null
            }
            add(ChainEntry(c.id, c.subjectLine, cat, target, c.id in archiveIds))
        }
        // Archives dont le cloud a disparu (TTL) → lecture (si échéance future) ou terminée.
        archives.filter { it.id !in cloudIds }.forEach { a ->
            val cat = if (now < a.readingDeadlineMillis) ChainCategory.READING else ChainCategory.CLOSED
            add(ChainEntry(a.id, a.subjectLine, cat,
                if (cat == ChainCategory.READING) a.readingDeadlineMillis else null, true))
        }
    }

    val selection = entries.filter { it.category == ChainCategory.SELECTION }
    val reading = entries.filter { it.category == ChainCategory.READING }
    val closed = entries.filter { it.category == ChainCategory.CLOSED }

    fun open(e: ChainEntry) {
        // Sélection → détail live. Lecture/terminée → lecteur hors-ligne si dispo.
        if (e.category != ChainCategory.SELECTION && e.hasArchive) {
            navController.navigate(Routes.chainArchive(e.id))
        } else {
            navController.navigate(Routes.chainDetail(e.id))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.chain_list_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.CHAIN_CREATE) }) {
                        Icon(Icons.Outlined.Add, stringResource(R.string.chain_create))
                    }
                }
            )
        }
    ) { padding ->
        if (!container.chains.isAvailable) {
            Column(Modifier.fillMaxSize().padding(padding).padding(32.dp),
                Arrangement.Center, Alignment.CenterHorizontally) {
                Text(stringResource(R.string.chain_service_unavailable),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text(stringResource(R.string.chain_create), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.chain_create_blurb),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            section(R.string.chain_cat_selection, selection, now, ::open, onDelete = null)
            section(R.string.chain_cat_reading, reading, now, ::open, onDelete = { toDelete = it })
            section(R.string.chain_cat_closed, closed, now, ::open, onDelete = { toDelete = it })
        }
    }

    toDelete?.let { e ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.chain_remove_from_list_title)) },
            text = { Text(stringResource(R.string.chain_remove_from_list_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    container.chainArchive.forget(e.id)
                    container.chainArchive.deleteArchive(e.id)
                    toDelete = null
                }) { Text(stringResource(R.string.chain_remove)) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    titleRes: Int,
    entries: List<ChainEntry>,
    now: Long,
    onOpen: (ChainEntry) -> Unit,
    onDelete: ((ChainEntry) -> Unit)?
) {
    if (entries.isEmpty()) return
    item(key = "h$titleRes") {
        Text(stringResource(titleRes), style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp))
    }
    items(entries, key = { it.id }) { e ->
        ChainEntryCard(e, now, onOpen, onDelete)
    }
}

@Composable
private fun ChainEntryCard(
    e: ChainEntry, now: Long, onOpen: (ChainEntry) -> Unit, onDelete: ((ChainEntry) -> Unit)?
) {
    AppCard(onClick = { onOpen(e) }, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(e.title, style = MaterialTheme.typography.titleSmall)
                if (e.countdownTarget != null) {
                    val left = (e.countdownTarget - now).coerceAtLeast(0L)
                    Text(
                        chainCountdown(left),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        stringResource(
                            if (e.category == ChainCategory.SELECTION) R.string.chain_cat_selection_sub
                            else R.string.chain_cat_reading_sub
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (onDelete != null) {
                IconButton(onClick = { onDelete(e) }) {
                    Icon(Icons.Outlined.DeleteOutline, stringResource(R.string.chain_remove),
                        tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

