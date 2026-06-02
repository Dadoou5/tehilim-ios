package com.david.tehilim.features.chains

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.david.tehilim.AppContainer
import com.david.tehilim.R
import com.david.tehilim.core.model.ChainAssignment
import com.david.tehilim.core.model.TehilimChain
import com.david.tehilim.core.service.ChainShareLink
import com.david.tehilim.navigation.Routes
import com.david.tehilim.ui.components.AppCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChainDetailScreen(container: AppContainer, chainId: String, navController: NavController) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current

    val chain by remember(chainId) { container.chains.chainFlow(chainId) }.collectAsStateWithLifecycle(initialValue = null)
    val participants by remember(chainId) { container.chains.participantsFlow(chainId) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val assignments by remember(chainId) { container.chains.boardFlow(chainId) }.collectAsStateWithLifecycle(initialValue = emptyMap())
    val uid = container.chains.currentUid

    // UI optimiste : overlay local par-dessus la vérité realtime → le tap réagit
    // instantanément (clé absente = pas d'override ; valeur null = retrait ;
    // valeur = ajout perso). Réconcilié dès que le realtime confirme l'état.
    val optimistic = remember(chainId) { mutableStateMapOf<Int, ChainAssignment?>() }
    LaunchedEffect(assignments) {
        optimistic.keys.toList().forEach { k ->
            val want = optimistic[k]
            val truth = assignments[k]
            val settled = if (want == null) truth == null else (truth != null && truth.uid == want.uid)
            if (settled) optimistic.remove(k)
        }
    }
    val effectiveAssignments: Map<Int, ChainAssignment> = remember(assignments, optimistic.toMap()) {
        val m = assignments.toMutableMap()
        optimistic.forEach { (k, v) -> if (v == null) m.remove(k) else m[k] = v }
        m
    }

    var nowTick by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); nowTick = System.currentTimeMillis() } }
    LaunchedEffect(chainId) { container.chainArchive.remember(chainId) }

    // Notifs push (participants) : demande la permission (Android 13+) puis
    // enregistre le token FCM. Contextuel — seuls ceux qui ouvrent une chaîne.
    val pushPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        com.david.tehilim.core.service.PushRegistrar.registerToken(context, container)
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            pushPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            com.david.tehilim.core.service.PushRegistrar.registerToken(context, container)
        }
    }

    var showJoin by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val errTaken = stringResource(R.string.chain_error_taken)

    val isParticipant = uid != null && participants.any { it.uid == uid }
    val isCreator = uid != null && chain?.creatorUid == uid
    val myName = participants.firstOrNull { it.uid == uid }?.name ?: "—"
    val myIds = effectiveAssignments.values.filter { it.uid == uid }.map { it.psalmId }.sorted()
    val open = chain?.isSelectionOpen(nowTick) ?: false

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
                    chain?.let { c ->
                        IconButton(onClick = { shareText(context, ChainShareLink.shareMessage(context, c)) }) {
                            Icon(Icons.Outlined.Share, stringResource(R.string.chain_share_action))
                        }
                    }
                }
            )
        }
    ) { padding ->
        val c = chain
        if (c == null) {
            Column(Modifier.fillMaxSize().padding(padding), Arrangement.Center, Alignment.CenterHorizontally) {
                Text(stringResource(R.string.chain_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        // Finition tablette : on centre + plafonne le contenu à ~1100dp sur
        // grand écran (pleine largeur sur téléphone) via un padding latéral.
        val sidePad = (((LocalConfiguration.current.screenWidthDp - 1100) / 2).coerceAtLeast(16)).dp
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 64.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = sidePad, end = sidePad, top = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            fun fullSpan(content: @Composable () -> Unit) {
                item(span = { GridItemSpan(maxLineSpan) }) { content() }
            }

            fullSpan { HeaderCard(c) }
            fullSpan { CountdownCard(c, open, nowTick) }
            fullSpan { ParticipantsCard(participants.size, participants.joinToString(" · ") { it.name }) }
            fullSpan { ProgressCard(effectiveAssignments.size) }

            if (!isParticipant) {
                fullSpan {
                    Button(onClick = { showJoin = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.chain_join_chain))
                    }
                }
            } else {
                fullSpan {
                    Text(
                        if (open) stringResource(R.string.chain_select_hint_open)
                        else stringResource(R.string.chain_select_hint_locked),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                // Grille 150
                itemsIndexed((1..TehilimChain.TOTAL_PSALMS).toList()) { _, psalmId ->
                    val a = effectiveAssignments[psalmId]
                    val mine = a != null && a.uid == uid
                    val takenByOther = a != null && !mine
                    val minutes = container.psalmRepository.psalm(psalmId)?.estimatedReadingMinutes ?: 1
                    PsalmCell(psalmId, a?.name, minutes, mine, takenByOther, enabled = !(open && takenByOther)) {
                        if (open) {
                            if (takenByOther) return@PsalmCell
                            // Retour haptique + mise à jour optimiste : la case
                            // change tout de suite, le serveur réconcilie ensuite.
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                            if (mine) {
                                optimistic[psalmId] = null
                                scope.launch {
                                    runCatching { container.chains.deselect(chainId, psalmId) }
                                        .onFailure { optimistic.remove(psalmId); error = errTaken }
                                }
                            } else {
                                optimistic[psalmId] = ChainAssignment(psalmId, uid ?: "", myName, false, System.currentTimeMillis())
                                scope.launch {
                                    runCatching { container.chains.select(chainId, psalmId, myName) }
                                        .onFailure { optimistic.remove(psalmId); error = errTaken }
                                }
                            }
                        } else {
                            navController.navigate(Routes.psalmDetail(psalmId, myIds))
                        }
                    }
                }

                if (isCreator) {
                    fullSpan { CreatorControls(c, effectiveAssignments, open, container, chainId, context, navController) { error = it } }
                }
            }

            error?.let { fullSpan { Text(it, color = MaterialTheme.colorScheme.error) } }
        }
    }

    if (showJoin) {
        var nameInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showJoin = false },
            title = { Text(stringResource(R.string.chain_join_chain)) },
            text = {
                Column {
                    Text(stringResource(R.string.chain_name_visible_note))
                    OutlinedTextField(
                        value = nameInput, onValueChange = { nameInput = it },
                        label = { Text(stringResource(R.string.chain_your_name)) }, singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = nameInput.isNotBlank(),
                    onClick = {
                        val n = nameInput.trim()
                        showJoin = false
                        scope.launch { runCatching { container.chains.join(chainId, n) } }
                    }
                ) { Text(stringResource(R.string.chain_join)) }
            },
            dismissButton = { TextButton(onClick = { showJoin = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}

@Composable
private fun HeaderCard(c: TehilimChain) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(intentionLabel(c.intentionType)), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary)
            Text(c.subjectLine, style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.chain_created_by, c.creatorName), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CountdownCard(c: TehilimChain, open: Boolean, now: Long) {
    val df = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                if (open) stringResource(R.string.chain_countdown_selection)
                else if (c.distributed) stringResource(R.string.chain_countdown_distributed) else stringResource(R.string.chain_countdown_closed),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                if (open) remaining(c.selectionDeadlineMillis - now) else df.format(Date(c.readingDeadlineMillis)),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun ParticipantsCard(count: Int, names: String) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(stringResource(R.string.chain_participants), style = MaterialTheme.typography.titleSmall)
                Text("$count", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            if (names.isNotBlank()) {
                Text(names, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun ProgressCard(assigned: Int) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text(stringResource(R.string.chain_progress), style = MaterialTheme.typography.titleSmall)
                Text("$assigned/${TehilimChain.TOTAL_PSALMS}", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinearProgressIndicator(
                progress = { assigned.toFloat() / TehilimChain.TOTAL_PSALMS },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun PsalmCell(
    id: Int, name: String?, minutes: Int, mine: Boolean, takenByOther: Boolean,
    enabled: Boolean, onClick: () -> Unit
) {
    val bg = when {
        mine -> MaterialTheme.colorScheme.primary
        takenByOther -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    val fg = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick, enabled = enabled, color = bg, shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().height(46.dp)
    ) {
        Column(Modifier.padding(2.dp), Arrangement.Center, Alignment.CenterHorizontally) {
            Text("$id", style = MaterialTheme.typography.labelLarge, color = fg)
            // Pris → le nom remplace le temps de lecture ; libre → « ~X min ».
            if (name != null) {
                Text(name, style = MaterialTheme.typography.labelSmall, maxLines = 1,
                    color = if (mine) fg else MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("~$minutes min", style = MaterialTheme.typography.labelSmall, maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CreatorControls(
    c: TehilimChain, assignments: Map<Int, ChainAssignment>, open: Boolean,
    container: AppContainer, chainId: String, context: android.content.Context,
    navController: NavController,
    onError: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val assigned = assignments.size
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.chain_owner), style = MaterialTheme.typography.titleSmall)
        if (open && assigned < TehilimChain.TOTAL_PSALMS) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        runCatching { container.chains.assignRemaining(chainId, c.creatorName) }
                            .onFailure { onError("Attribution impossible.") }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.chain_assign_remaining, TehilimChain.TOTAL_PSALMS - assigned)) }
        }
        if (open && !c.distributed) {
            Button(
                onClick = {
                    scope.launch {
                        runCatching { container.chains.distribute(chainId) }
                        // Archive locale du compte rendu (conservée après TTL cloud).
                        container.chainArchive.saveArchive(
                            com.david.tehilim.core.persistence.ChainArchiveSnapshot(
                                id = c.id, name = c.name, intentionWire = c.intentionType.wire,
                                detail = c.intentionDetail, creatorName = c.creatorName,
                                readingDeadlineMillis = c.readingDeadlineMillis,
                                archivedAtMillis = System.currentTimeMillis(),
                                assignments = assignments.mapKeys { it.key.toString() }.mapValues { it.value.name }
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.chain_close_distribute)) }
        }
        OutlinedButton(
            onClick = { shareText(context, reportText(context, c, assignments)) },
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.chain_share_report)) }

        var showDelete by remember { mutableStateOf(false) }
        OutlinedButton(
            onClick = { showDelete = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) { Text(stringResource(R.string.chain_delete)) }

        if (showDelete) {
            AlertDialog(
                onDismissRequest = { showDelete = false },
                title = { Text(stringResource(R.string.chain_delete_confirm_title)) },
                text = { Text(stringResource(R.string.chain_delete_confirm_msg)) },
                confirmButton = {
                    TextButton(onClick = {
                        showDelete = false
                        scope.launch {
                            runCatching { container.chains.deleteChain(chainId) }
                                .onSuccess {
                                    container.chainArchive.forget(chainId)
                                    navController.popBackStack()
                                }
                                .onFailure { onError("Suppression impossible.") }
                        }
                    }) { Text(stringResource(R.string.chain_delete)) }
                },
                dismissButton = { TextButton(onClick = { showDelete = false }) { Text(stringResource(R.string.action_cancel)) } }
            )
        }
    }
}

// MARK: - Helpers

private fun remaining(ms: Long): String {
    val secs = (ms / 1000).coerceAtLeast(0)
    val d = secs / 86400; val h = (secs % 86400) / 3600; val m = (secs % 3600) / 60; val s = secs % 60
    return when {
        d > 0 -> "$d j $h h"
        h > 0 -> "$h h $m min"
        m > 0 -> "$m min $s s"
        else -> "$s s"
    }
}

private fun shareText(context: android.content.Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, null))
}

/** Compte rendu groupé par participant — partageable WhatsApp. */
private fun reportText(context: android.content.Context, c: TehilimChain, assignments: Map<Int, ChainAssignment>): String {
    val byUid = LinkedHashMap<String, Pair<String, MutableList<Int>>>()
    for ((psalmId, a) in assignments) {
        byUid.getOrPut(a.uid) { a.name to mutableListOf() }.second.add(psalmId)
    }
    val sb = StringBuilder(context.getString(R.string.chain_share_prefix) + c.subjectLine + "\n")
    val sep = context.getString(R.string.chain_range_to)
    byUid.values.sortedBy { it.first }.forEach { (name, ids) ->
        sb.append("\n• $name : ${TehilimChain.compressRanges(ids, sep)}")
    }
    val assigned = assignments.size
    if (assigned < TehilimChain.TOTAL_PSALMS) {
        sb.append("\n\n" + context.getString(R.string.chain_report_remaining, TehilimChain.TOTAL_PSALMS - assigned))
    }
    return sb.toString()
}
