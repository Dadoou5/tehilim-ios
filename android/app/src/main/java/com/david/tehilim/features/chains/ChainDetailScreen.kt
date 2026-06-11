package com.david.tehilim.features.chains

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoveToInbox
import androidx.compose.material.icons.outlined.MoreTime
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
import com.david.tehilim.core.model.ChainIntention
import com.david.tehilim.core.model.ChainParticipant
import com.david.tehilim.core.model.TehilimBook
import com.david.tehilim.core.model.TehilimChain
import com.david.tehilim.core.service.ChainShareLink
import com.david.tehilim.navigation.Routes
import com.david.tehilim.ui.components.AppCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

private enum class GridFilter(val labelRes: Int) {
    ALL(R.string.chain_filter_all),
    FREE(R.string.chain_filter_free),
    MINE(R.string.chain_filter_mine)
}

private val segmentPalette = listOf(
    Color(0xFF6C63B5), Color(0xFF3B82F6), Color(0xFF22A06B), Color(0xFFE08D1A),
    Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF14B8A6), Color(0xFF6366F1),
    Color(0xFF9C6B4A), Color(0xFF10B981)
)

private fun intentionColor(kind: ChainIntention): Color = when (kind) {
    ChainIntention.LELOUY -> Color(0xFF73699E)   // violet — mémoire
    ChainIntention.REFOUA -> Color(0xFFD1556B)   // rose — guérison
    ChainIntention.REUSSITE -> Color(0xFFDB9E2E) // doré — réussite
}

/** Contenu standard d'un bouton « premium » : icône en tête + libellé. */
@Composable
private fun androidx.compose.foundation.layout.RowScope.BtnContent(icon: ImageVector, text: String) {
    Icon(icon, null, Modifier.size(18.dp))
    Text(text, modifier = Modifier.padding(start = 8.dp))
}

/** Titre de carte avec icône de tête (cohérence + lecture rapide). */
@Composable
private fun CardTitle(icon: ImageVector, text: String, tint: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, Modifier.size(18.dp), tint = tint)
        Text(text, style = MaterialTheme.typography.titleSmall)
    }
}

private fun bookLabelRes(book: TehilimBook): Int = when (book) {
    TehilimBook.ONE -> R.string.chain_book_1
    TehilimBook.TWO -> R.string.chain_book_2
    TehilimBook.THREE -> R.string.chain_book_3
    TehilimBook.FOUR -> R.string.chain_book_4
    TehilimBook.FIVE -> R.string.chain_book_5
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChainDetailScreen(container: AppContainer, chainId: String, navController: NavController) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current

    val chain by remember(chainId) { container.chains.chainFlow(chainId) }.collectAsStateWithLifecycle(initialValue = null)
    val participants by remember(chainId) { container.chains.participantsFlow(chainId) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val assignments by remember(chainId) { container.chains.boardFlow(chainId) }.collectAsStateWithLifecycle(initialValue = emptyMap())
    // uid réactif : on attend le chargement de la session persistée pour éviter
    // un faux état « non participant » (proposition de rejoindre) au retour
    // d'arrière-plan après un partage.
    // On garantit une session (sign-in anonyme si besoin) dès l'ouverture, sinon
    // `uid` reste null pour un nouvel appareil ouvrant un lien → « Rejoindre »
    // ne basculerait jamais en participant après le join.
    var uid by remember { mutableStateOf(container.chains.currentUid) }
    LaunchedEffect(Unit) {
        uid = runCatching { container.chains.ensureSignedIn() }.getOrNull() ?: container.chains.currentUid
    }

    // UI optimiste : overlay local par-dessus la vérité realtime (tap instantané).
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

    // Notifs push (participants) : permission Android 13+ puis enregistrement du token FCM.
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
    var showLeave by remember { mutableStateOf(false) }
    var showInvite by remember { mutableStateOf(false) }
    // Participant que le maître s'apprête à retirer (confirmation avant action).
    var participantToRemove by remember { mutableStateOf<ChainParticipant?>(null) }
    var editingChain by remember { mutableStateOf<TehilimChain?>(null) }
    var gridFilter by remember { mutableStateOf(GridFilter.ALL) }
    var error by remember { mutableStateOf<String?>(null) }
    val errTaken = stringResource(R.string.chain_error_taken)

    // Bascule optimiste : dès le join réussi on se considère participant (nom
    // saisi), sans attendre la confirmation realtime → l'écran quitte « Rejoindre »
    // immédiatement et la grille devient sélectionnable.
    var joinedName by remember(chainId) { mutableStateOf<String?>(null) }
    val isParticipant = uid != null && (joinedName != null || participants.any { it.uid == uid })
    val isCreator = uid != null && chain?.creatorUid == uid
    val myName = participants.firstOrNull { it.uid == uid }?.name ?: joinedName ?: "—"
    val myIds = effectiveAssignments.values.filter { it.uid == uid }.map { it.psalmId }.sorted()
    val open = chain?.isSelectionOpen(nowTick) ?: false

    // Une fois la chaîne distribuée, on en garde un instantané local (pour CHAQUE
    // participant) afin de pouvoir la consulter et lire ses Tehilim hors-ligne
    // (mode avion) depuis « Mes chaînes ».
    LaunchedEffect(chain?.distributed, isParticipant, effectiveAssignments.size, uid) {
        val c = chain
        if (c != null && c.distributed && isParticipant && effectiveAssignments.isNotEmpty()) {
            container.chainArchive.saveArchive(
                com.david.tehilim.core.persistence.ChainArchiveSnapshot(
                    id = c.id, name = c.name, intentionWire = c.intentionType.wire,
                    detail = c.intentionDetail, creatorName = c.creatorName,
                    readingDeadlineMillis = c.readingDeadlineMillis,
                    archivedAtMillis = System.currentTimeMillis(),
                    assignments = effectiveAssignments.mapKeys { it.key.toString() }.mapValues { it.value.name },
                    myPsalmIds = effectiveAssignments.values.filter { it.uid == uid }.map { it.psalmId }.sorted()
                )
            )
        }
    }

    fun countFor(pUid: String) = effectiveAssignments.values.count { it.uid == pUid }

    Box(Modifier.fillMaxSize()) {
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
                SkeletonContent(Modifier.padding(padding))
                return@Scaffold
            }

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
                fullSpan {
                    ParticipantsCard(
                        participants = participants,
                        countFor = ::countFor,
                        isCreator = isCreator,
                        // Retrait possible tant que non distribuée ; invitation seulement
                        // tant que la sélection est ouverte (un arrivant tardif ne pourrait
                        // plus rien choisir — le créateur peut « Prolonger » pour rouvrir).
                        canRemove = !c.distributed,
                        canInvite = open,
                        onInvite = { showInvite = true },
                        onRemove = { p -> participantToRemove = p }
                    )
                }
                // Invitation possible uniquement tant que la sélection est ouverte.
                if (open) {
                    fullSpan {
                        OutlinedButton(onClick = { showInvite = true }, modifier = Modifier.fillMaxWidth()) {
                            BtnContent(Icons.Outlined.PersonAdd, stringResource(R.string.chain_invite_participants))
                        }
                    }
                }
                fullSpan { ProgressCard(effectiveAssignments, participants) }
                if (effectiveAssignments.isNotEmpty()) {
                    fullSpan { BreakdownCard(effectiveAssignments, stringResource(R.string.chain_range_to)) }
                }

                if (!isParticipant) {
                    fullSpan {
                        if (c.distributed) {
                            // Chaîne distribuée → inscriptions closes, on ne peut plus rejoindre.
                            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Lock, null, Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(stringResource(R.string.chain_distributed_closed),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 8.dp))
                            }
                        } else {
                            Button(onClick = { showJoin = true }, modifier = Modifier.fillMaxWidth()) {
                                BtnContent(Icons.AutoMirrored.Outlined.Login, stringResource(R.string.chain_join_chain))
                            }
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
                    // Filtre + compteur
                    fullSpan {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            GridFilter.entries.forEach { f ->
                                FilterChip(
                                    selected = gridFilter == f,
                                    onClick = { gridFilter = f },
                                    label = { Text(stringResource(f.labelRes)) }
                                )
                            }
                            Text("${effectiveAssignments.size}/${TehilimChain.TOTAL_PSALMS}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Grille par livres (filtrée)
                    var anyShown = false
                    TehilimBook.entries.forEach { book ->
                        val ids = book.range.filter { id ->
                            when (gridFilter) {
                                GridFilter.ALL -> true
                                GridFilter.FREE -> effectiveAssignments[id] == null
                                GridFilter.MINE -> effectiveAssignments[id]?.uid == uid
                            }
                        }
                        if (ids.isNotEmpty()) {
                            anyShown = true
                            fullSpan {
                                Text(stringResource(bookLabelRes(book)),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 6.dp))
                            }
                            itemsIndexed(ids) { _, psalmId ->
                                val a = effectiveAssignments[psalmId]
                                val mine = a != null && a.uid == uid
                                val takenByOther = a != null && !mine
                                val minutes = container.psalmRepository.psalm(psalmId)?.estimatedReadingMinutes ?: 1
                                PsalmCell(psalmId, a?.name, minutes, mine, takenByOther, enabled = !(open && takenByOther)) {
                                    if (open) {
                                        if (takenByOther) return@PsalmCell
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
                        }
                    }
                    if (!anyShown) {
                        fullSpan {
                            Text(
                                stringResource(if (gridFilter == GridFilter.MINE) R.string.chain_filter_empty_mine else R.string.chain_filter_empty_free),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    }

                    if (isCreator) {
                        fullSpan {
                            CreatorControls(c, effectiveAssignments, open, container, chainId, context, navController,
                                onEdit = { editingChain = c }) { error = it }
                        }
                    } else {
                        fullSpan {
                            OutlinedButton(
                                onClick = { showLeave = true },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) { BtnContent(Icons.AutoMirrored.Outlined.Logout, stringResource(R.string.chain_leave)) }
                        }
                    }
                }

                error?.let { fullSpan { Text(it, color = MaterialTheme.colorScheme.error) } }
            }
        }

        // Overlay d'édition (créateur) — plein écran par-dessus le détail.
        editingChain?.let { ec ->
            CreateChainScreen(
                container = container, editing = ec,
                onBack = { editingChain = null },
                onCreated = { editingChain = null }
            )
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
                        scope.launch {
                            runCatching { container.chains.join(chainId, n) }
                                .onSuccess {
                                    uid = container.chains.currentUid
                                    joinedName = n   // bascule optimiste en participant
                                }
                                .onFailure {
                                    error = if ((it.message ?: "").contains("CHAIN_FULL"))
                                        "Chaîne complète." else "Impossible de rejoindre."
                                }
                        }
                    }
                ) { Text(stringResource(R.string.chain_join)) }
            },
            dismissButton = { TextButton(onClick = { showJoin = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }

    if (showLeave) {
        AlertDialog(
            onDismissRequest = { showLeave = false },
            title = { Text(stringResource(R.string.chain_leave_confirm_title)) },
            text = { Text(stringResource(R.string.chain_leave_confirm_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    showLeave = false
                    scope.launch {
                        runCatching { container.chains.leaveChain(chainId) }
                            .onSuccess { container.chainArchive.forget(chainId); navController.popBackStack() }
                    }
                }) { Text(stringResource(R.string.chain_leave)) }
            },
            dismissButton = { TextButton(onClick = { showLeave = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }

    if (showInvite) {
        chain?.let { c -> InviteDialog(c) { showInvite = false } }
    }

    // Confirmation de retrait d'un participant (maître, pendant la sélection).
    participantToRemove?.let { p ->
        AlertDialog(
            onDismissRequest = { participantToRemove = null },
            title = { Text(stringResource(R.string.chain_remove_confirm_title)) },
            text = { Text(stringResource(R.string.chain_remove_confirm_msg, p.name)) },
            confirmButton = {
                TextButton(onClick = {
                    participantToRemove = null
                    scope.launch {
                        runCatching { container.chains.removeParticipant(chainId, p.uid) }
                            .onFailure { error = errTaken }
                    }
                }) { Text(stringResource(R.string.chain_remove_action)) }
            },
            dismissButton = { TextButton(onClick = { participantToRemove = null }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}

@Composable
private fun InviteDialog(chain: TehilimChain, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val link = remember(chain.id) { ChainShareLink.uri(chain.id).toString() }
    val qr = remember(link) { generateQrBitmap(context, link) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) } },
        title = { Text(stringResource(R.string.chain_invite)) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.chain_invite_blurb),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center)
                qr?.let {
                    Image(it.asImageBitmap(), stringResource(R.string.chain_invite_qr), Modifier.size(220.dp))
                }
                Text(link, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Button(onClick = { shareText(context, ChainShareLink.shareMessage(context, chain)) },
                    modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.chain_share_link))
                }
                OutlinedButton(onClick = {
                    val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("link", link))
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.chain_copy_link))
                }
            }
        }
    )
}

/**
 * QR « marque » : modules bleu nuit (couleur de l'icône) sur fond blanc,
 * correction d'erreur maximale (H), et icône de lancement incrustée au centre
 * sur une pastille blanche arrondie. Le niveau H tolère la zone masquée.
 */
private fun generateQrBitmap(context: android.content.Context, content: String, size: Int = 600): android.graphics.Bitmap? {
    return try {
        val navy = android.graphics.Color.rgb(14, 20, 48)   // #0E1430, fond de l'icône
        val white = android.graphics.Color.WHITE
        val hints = mapOf(
            com.google.zxing.EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H,
            com.google.zxing.EncodeHintType.MARGIN to 1
        )
        val matrix = com.google.zxing.qrcode.QRCodeWriter()
            .encode(content, com.google.zxing.BarcodeFormat.QR_CODE, size, size, hints)
        val bmp = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val pixels = IntArray(size * size)
        for (y in 0 until size) for (x in 0 until size) {
            pixels[y * size + x] = if (matrix.get(x, y)) navy else white
        }
        bmp.setPixels(pixels, 0, size, 0, 0, size, size)

        // Incrustation du logo de lancement au centre.
        val canvas = android.graphics.Canvas(bmp)
        val logoSize = (size * 0.22f).toInt()           // ~22 % de la largeur
        val plate = (logoSize * 1.18f).toInt()          // pastille blanche un peu plus grande
        val cx = size / 2f; val cy = size / 2f
        val plateRect = android.graphics.RectF(cx - plate / 2f, cy - plate / 2f, cx + plate / 2f, cy + plate / 2f)
        val r = plate * 0.22f
        canvas.drawRoundRect(plateRect, r, r, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = white })

        val drawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.chain_brand_logo)
        if (drawable != null) {
            val logo = android.graphics.Bitmap.createBitmap(logoSize, logoSize, android.graphics.Bitmap.Config.ARGB_8888)
            val lc = android.graphics.Canvas(logo)
            drawable.setBounds(0, 0, logoSize, logoSize)
            drawable.draw(lc)
            // Coins arrondis du logo.
            val rounded = android.graphics.Bitmap.createBitmap(logoSize, logoSize, android.graphics.Bitmap.Config.ARGB_8888)
            val rc = android.graphics.Canvas(rounded)
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            val rectL = android.graphics.RectF(0f, 0f, logoSize.toFloat(), logoSize.toFloat())
            rc.drawRoundRect(rectL, logoSize * 0.2f, logoSize * 0.2f, paint)
            paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
            rc.drawBitmap(logo, 0f, 0f, paint)
            canvas.drawBitmap(rounded, cx - logoSize / 2f, cy - logoSize / 2f, null)
        }
        bmp
    } catch (e: Exception) { null }
}

@Composable
private fun HeaderCard(c: TehilimChain) {
    val color = intentionColor(c.intentionType)
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(color))
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(intentionIcon(c.intentionType), null, Modifier.size(16.dp), tint = color)
                    Text(stringResource(intentionLabel(c.intentionType)), style = MaterialTheme.typography.labelMedium,
                        color = color)
                }
                Text(c.subjectLine, style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.chain_created_by, c.creatorName), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CountdownCard(c: TehilimChain, open: Boolean, now: Long) {
    val df = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    // Trois états : sélection (compte à rebours), lecture en cours (compte à
    // rebours vers la fin de lecture), terminée (date).
    val reading = !open && now < c.readingDeadlineMillis
    val live = open || reading
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            CardTitle(
                if (live) Icons.Outlined.Timer else Icons.AutoMirrored.Outlined.MenuBook,
                when {
                    open -> stringResource(R.string.chain_countdown_selection)
                    reading -> stringResource(R.string.chain_countdown_reading)
                    else -> stringResource(R.string.chain_countdown_closed)
                }
            )
            Text(
                when {
                    open -> chainCountdown(c.selectionDeadlineMillis - now)
                    reading -> chainCountdown(c.readingDeadlineMillis - now)
                    else -> df.format(Date(c.readingDeadlineMillis))
                },
                style = if (live) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun ParticipantsCard(
    participants: List<ChainParticipant>,
    countFor: (String) -> Int,
    isCreator: Boolean,
    canRemove: Boolean,
    canInvite: Boolean,
    onInvite: () -> Unit,
    onRemove: (ChainParticipant) -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                CardTitle(Icons.Outlined.People, stringResource(R.string.chain_participants))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${participants.size}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    if (canInvite) {
                        IconButton(onClick = onInvite) {
                            Icon(Icons.Outlined.PersonAdd, stringResource(R.string.chain_invite),
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            if (isCreator) {
                participants.forEach { p ->
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(p.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        if (p.isCreator) {
                            Text(stringResource(R.string.chain_owner_tag), style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 8.dp))
                        }
                        Text("${countFor(p.uid)}", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!p.isCreator && canRemove) {
                            IconButton(onClick = { onRemove(p) }) {
                                Icon(Icons.Outlined.PersonRemove,
                                    stringResource(R.string.chain_remove_participant, p.name),
                                    tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            } else if (participants.isNotEmpty()) {
                Text(participants.joinToString(" · ") { it.name }, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun ProgressCard(assignments: Map<Int, ChainAssignment>, participants: List<ChainParticipant>) {
    val total = TehilimChain.TOTAL_PSALMS
    val pct = (assignments.size * 100) / total
    val animatedPct by animateIntAsState(pct, animationSpec = tween(500), label = "pct")
    val complete = assignments.size >= total
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                CardTitle(Icons.Outlined.Insights, stringResource(R.string.chain_progress))
                // Pourcentage animé, mis en avant (vert à 100 %).
                Text("$animatedPct%", style = MaterialTheme.typography.titleMedium,
                    color = if (complete) Color(0xFF22A06B) else MaterialTheme.colorScheme.primary)
            }
            Text("${assignments.size}/$total", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp))
            // Barre segmentée par participant (apparition fluide de chaque segment).
            Row(Modifier.fillMaxWidth().padding(top = 8.dp).height(10.dp).clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)) {
                participants.forEachIndexed { i, p ->
                    val c = assignments.values.count { it.uid == p.uid }
                    val w by animateFloatAsState(c.toFloat(), animationSpec = tween(400), label = "seg$i")
                    if (w > 0f) {
                        Box(Modifier.weight(w).fillMaxHeight()
                            .background(segmentPalette[i % segmentPalette.size]))
                    }
                }
                val remaining = (total - assignments.size).toFloat()
                val rw by animateFloatAsState(remaining.coerceAtLeast(0.0001f), animationSpec = tween(400), label = "rem")
                Box(Modifier.weight(rw).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun BreakdownCard(assignments: Map<Int, ChainAssignment>, sep: String) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CardTitle(Icons.AutoMirrored.Outlined.FormatListBulleted, stringResource(R.string.chain_breakdown))
            val byUid = LinkedHashMap<String, Pair<String, MutableList<Int>>>()
            for ((psalmId, a) in assignments) {
                byUid.getOrPut(a.uid) { a.name to mutableListOf() }.second.add(psalmId)
            }
            byUid.values.sortedBy { it.first }.forEach { (name, ids) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.4f))
                    Text(TehilimChain.compressRanges(ids, sep),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(0.6f))
                }
            }
        }
    }
}

@Composable
private fun PsalmCell(
    id: Int, name: String?, minutes: Int, mine: Boolean, takenByOther: Boolean,
    enabled: Boolean, onClick: () -> Unit
) {
    val targetBg = when {
        mine -> MaterialTheme.colorScheme.primary
        takenByOther -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    // Fondu de couleur fluide quand on prend / libère un Tehilim (UI optimiste).
    val bg by animateColorAsState(targetBg, animationSpec = tween(220), label = "cellBg")
    val fg by animateColorAsState(
        if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(220), label = "cellFg"
    )
    Surface(
        onClick = onClick, enabled = enabled, color = bg, shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().height(46.dp)
    ) {
        Column(Modifier.padding(2.dp), Arrangement.Center, Alignment.CenterHorizontally) {
            Text("$id", style = MaterialTheme.typography.labelLarge, color = fg)
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
private fun SkeletonContent(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(3) {
            Box(Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant))
        }
        com.david.tehilim.ui.components.AppCard(modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().height(120.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CreatorControls(
    c: TehilimChain, assignments: Map<Int, ChainAssignment>, open: Boolean,
    container: AppContainer, chainId: String, context: android.content.Context,
    navController: NavController,
    onEdit: () -> Unit,
    onError: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val assigned = assignments.size
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.chain_owner), style = MaterialTheme.typography.titleSmall)
        if (open && !c.distributed) {
            OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                BtnContent(Icons.Outlined.Edit, stringResource(R.string.chain_edit))
            }
        }
        // Sélection incomplète (ouverte ou close) → prolonger l'échéance + re-notifier.
        var showExtend by remember { mutableStateOf(false) }
        if (!c.distributed && assigned < TehilimChain.TOTAL_PSALMS) {
            OutlinedButton(
                onClick = { showExtend = true },
                modifier = Modifier.fillMaxWidth()
            ) { BtnContent(Icons.Outlined.MoreTime, stringResource(R.string.chain_extend_selection)) }
        }
        if (showExtend) {
            // Prolongation par durée (depuis l'échéance si future, sinon depuis maintenant).
            var extendHours by remember { mutableStateOf(24) }
            // Prolongation en heures uniquement, plafonnée à 48 h.
            val durations = listOf(1, 3, 6, 12, 24, 36, 48)
            AlertDialog(
                onDismissRequest = { showExtend = false },
                title = { Text(stringResource(R.string.chain_extend_selection)) },
                text = {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        durations.forEach { h ->
                            FilterChip(
                                selected = extendHours == h,
                                onClick = { extendHours = h },
                                label = { Text("$h h") }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showExtend = false
                        val base = maxOf(c.selectionDeadlineMillis, System.currentTimeMillis())
                        val newMs = base + extendHours * 3600_000L
                        scope.launch {
                            runCatching { container.chains.extendSelection(chainId, newMs) }
                                .onFailure { onError("Prolongation impossible.") }
                        }
                    }) { Text(stringResource(R.string.chain_extend_selection)) }
                },
                dismissButton = { TextButton(onClick = { showExtend = false }) { Text(stringResource(R.string.action_cancel)) } }
            )
        }
        if (open && assigned < TehilimChain.TOTAL_PSALMS) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        runCatching { container.chains.assignRemaining(chainId, c.creatorName) }
                            .onFailure { onError("Attribution impossible.") }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { BtnContent(Icons.Outlined.MoveToInbox, stringResource(R.string.chain_assign_remaining, TehilimChain.TOTAL_PSALMS - assigned)) }
        }
        if (open && !c.distributed) {
            Button(
                onClick = {
                    scope.launch {
                        runCatching { container.chains.distribute(chainId) }
                        container.chainArchive.saveArchive(
                            com.david.tehilim.core.persistence.ChainArchiveSnapshot(
                                id = c.id, name = c.name, intentionWire = c.intentionType.wire,
                                detail = c.intentionDetail, creatorName = c.creatorName,
                                readingDeadlineMillis = c.readingDeadlineMillis,
                                archivedAtMillis = System.currentTimeMillis(),
                                assignments = assignments.mapKeys { it.key.toString() }.mapValues { it.value.name },
                                myPsalmIds = assignments.values.filter { it.uid == c.creatorUid }.map { it.psalmId }.sorted()
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { BtnContent(Icons.Outlined.Verified, stringResource(R.string.chain_close_distribute)) }
        }
        OutlinedButton(
            onClick = { shareText(context, reportText(context, c, assignments)) },
            modifier = Modifier.fillMaxWidth()
        ) { BtnContent(Icons.Outlined.Share, stringResource(R.string.chain_share_report)) }

        // Action destructive isolée du reste (divider + tonalité « error »).
        androidx.compose.material3.HorizontalDivider(
            Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        var showDelete by remember { mutableStateOf(false) }
        OutlinedButton(
            onClick = { showDelete = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) { BtnContent(Icons.Outlined.Delete, stringResource(R.string.chain_delete)) }

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
