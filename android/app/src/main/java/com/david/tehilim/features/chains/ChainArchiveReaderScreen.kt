package com.david.tehilim.features.chains

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.david.tehilim.AppContainer
import com.david.tehilim.R
import com.david.tehilim.core.model.TehilimChain
import com.david.tehilim.navigation.Routes
import com.david.tehilim.ui.components.AppCard
import java.text.DateFormat
import java.util.Date

/**
 * Lecture **hors-ligne** d'une chaîne distribuée, à partir de l'instantané local
 * (aucun appel réseau). Permet de continuer à lire ses Tehilim en mode avion.
 * Le texte des Tehilim est déjà 100 % embarqué dans l'app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChainArchiveReaderScreen(container: AppContainer, chainId: String, navController: NavController) {
    val archives by container.chainArchive.archives.collectAsState()
    val snap = archives.firstOrNull { it.id == chainId }
    val context = LocalContext.current
    val df = remember(snap?.readingDeadlineMillis) { DateFormat.getDateInstance(DateFormat.MEDIUM) }
    // Horloge → compte à rebours du temps de lecture restant.
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.chain_title), maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    snap?.let { s ->
                        IconButton(onClick = { shareArchiveSnapshot(context, s) }) {
                            Icon(Icons.Outlined.Share, stringResource(R.string.chain_share_action))
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (snap == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(32.dp),
                Arrangement.Center, Alignment.CenterHorizontally) {
                Text(stringResource(R.string.chain_loading),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        val myIds = snap.myPsalmIds
        val sep = stringResource(R.string.chain_range_to)

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 64.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            fun full(content: @Composable () -> Unit) =
                item(span = { GridItemSpan(maxLineSpan) }) { content() }

            full {
                AppCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(snap.subjectLine, style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.chain_created_by, snap.creatorName),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val readingLeft = snap.readingDeadlineMillis - now
                        if (readingLeft > 0) {
                            Text(stringResource(R.string.chain_countdown_reading),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(chainCountdown(readingLeft),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary)
                        } else {
                            Text(stringResource(R.string.chain_countdown_closed) + " · " + df.format(Date(snap.readingDeadlineMillis)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (myIds.isNotEmpty()) {
                full {
                    Row(stringResource(R.string.chain_archive_my_tehilim))
                }
                items(myIds) { id ->
                    val minutes = container.psalmRepository.psalm(id)?.estimatedReadingMinutes ?: 1
                    ReaderCell(id, minutes) {
                        navController.navigate(Routes.psalmDetail(id, myIds))
                    }
                }
            } else {
                full {
                    Text(stringResource(R.string.chain_archive_no_mine),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp))
                }
            }

            // Répartition complète (lecture seule).
            if (snap.assignments.isNotEmpty()) {
                full {
                    AppCard(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(stringResource(R.string.chain_breakdown))
                            val byName = LinkedHashMap<String, MutableList<Int>>()
                            snap.assignments.forEach { (k, name) ->
                                byName.getOrPut(name) { mutableListOf() }.add(k.toIntOrNull() ?: 0)
                            }
                            byName.toSortedMap().forEach { (name, ids) ->
                                androidx.compose.foundation.layout.Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(name, style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(0.4f))
                                    Text(TehilimChain.compressRanges(ids.sorted(), sep),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(0.6f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Row(text: String) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.AutoMirrored.Outlined.MenuBook, null, Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun ReaderCell(id: Int, minutes: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick, shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().height(46.dp)
    ) {
        Column(Modifier.padding(2.dp), Arrangement.Center, Alignment.CenterHorizontally) {
            Text("$id", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary)
            Text("~$minutes min", style = MaterialTheme.typography.labelSmall, maxLines = 1,
                color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

private fun shareArchiveSnapshot(context: android.content.Context, snap: com.david.tehilim.core.persistence.ChainArchiveSnapshot) {
    val byName = LinkedHashMap<String, MutableList<Int>>()
    snap.assignments.forEach { (k, name) -> byName.getOrPut(name) { mutableListOf() }.add(k.toIntOrNull() ?: 0) }
    val sb = StringBuilder(context.getString(R.string.chain_share_prefix) + snap.subjectLine + "\n")
    val sep = context.getString(R.string.chain_range_to)
    byName.toSortedMap().forEach { (name, ids) -> sb.append("\n• $name : ${TehilimChain.compressRanges(ids.sorted(), sep)}") }
    val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, sb.toString()) }
    context.startActivity(Intent.createChooser(intent, null))
}
