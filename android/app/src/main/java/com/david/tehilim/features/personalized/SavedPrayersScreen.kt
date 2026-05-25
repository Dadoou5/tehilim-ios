package com.david.tehilim.features.personalized

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.ui.Alignment
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.david.tehilim.core.service.NotificationScheduler
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.david.tehilim.AppContainer
import com.david.tehilim.R
import com.david.tehilim.core.service.MemorialCalculator
import com.david.tehilim.navigation.Routes
import com.david.tehilim.ui.components.AppCard
import com.david.tehilim.ui.components.EmptyState
import com.david.tehilim.ui.theme.EzraSilFontFamily
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPrayersScreen(container: AppContainer, navController: NavController) {
    val rawIntents by container.savedPrayers.intents.collectAsState()
    val context = LocalContext.current

    // V1.4 — État de la confirmation de suppression. Un seul AlertDialog
    // partagé pour toutes les cards : on stocke juste l'intent ciblé.
    var intentToDelete by remember {
        mutableStateOf<com.david.tehilim.core.model.SavedPrayerIntent?>(null)
    }

    // V1.4 — Clé de cache jour-courant. Inclure cette clé dans tous les
    // `remember(...)` qui dépendent de `nextYahrzeit` garantit l'invalidation
    // au passage à minuit (sans elle, le résultat restait figé indéfiniment
    // car la date du décès ne change jamais après création).
    val todayEpochDay = java.time.LocalDate.now().toEpochDay()

    // V1.4 — Tri : (1) prières avec date du décès → triées par prochaine
    // azcara croissante (les commémorations à venir en premier),
    // (2) prières sans date → triées par date de création décroissante,
    // ajoutées après le premier groupe.
    val intents = remember(rawIntents, todayEpochDay) {
        val now = Date()
        val withAzcara = rawIntents.mapNotNull { intent ->
            val millis = intent.civilDateOfDeathEpochMillis ?: return@mapNotNull null
            val next = MemorialCalculator.nextYahrzeit(Date(millis), now) ?: return@mapNotNull null
            intent to next
        }.sortedWith(
            compareBy<Pair<com.david.tehilim.core.model.SavedPrayerIntent, Date>> { it.second }
                .thenByDescending { it.first.createdAtEpochMillis }
        ).map { it.first }

        val withoutAzcara = rawIntents
            .filter { it.civilDateOfDeathEpochMillis == null }
            .sortedByDescending { it.createdAtEpochMillis }

        withAzcara + withoutAzcara
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_my_prayers)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        if (intents.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.Inbox,
                title = stringResource(R.string.msg_no_saved_lelouy_nichmat),
                message = stringResource(R.string.msg_generate_to_find_here),
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(intents) { intent ->
                // V1.4 — `remember` doit être dans le scope @Composable des
                // items, pas dans LazyColumn { } qui est un LazyListScope DSL.
                val dateFormatter = remember {
                    DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
                }
                AppCard(
                    onClick = { navController.navigate(Routes.personalizedList(intent.id)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    // V1.4 — Row pour réserver l'espace cloche en haut-droite
                    // si les rappels sont effectivement plannifiés.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(intent.hebrewSubject,
                                style = TextStyle(fontFamily = EzraSilFontFamily))
                            Text(
                                stringResource(R.string.label_letters_count, intent.generatedLetters.size),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            // V1.4 — Prochaine azcara si date du décès renseignée.
                            // L'astérisque rappelle que le Hebrew day commence
                            // au coucher du soleil de la veille civile —
                            // explication en bas de l'écran.
                            val millis = intent.civilDateOfDeathEpochMillis
                            if (millis != null) {
                                // Clé `todayEpochDay` : invalidation au
                                // passage à minuit pour que l'azcara passée
                                // laisse place à la suivante.
                                val next = remember(millis, todayEpochDay) {
                                    MemorialCalculator.nextYahrzeit(Date(millis))
                                }
                                if (next != null) {
                                    Text(
                                        "${stringResource(R.string.memorial_next_azcara)} : ${dateFormatter.format(next)}*",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }

                        // Badge cloche : visible UNIQUEMENT si rappels
                        // effectivement plannifiés (mêmes conditions que
                        // NotificationScheduler.rescheduleMemorial).
                        val hasActiveReminders = intent.remindersEnabled &&
                            intent.civilDateOfDeathEpochMillis != null &&
                            (intent.notifySevenDaysBefore || intent.notifySameDay)
                        if (hasActiveReminders) {
                            Icon(
                                Icons.Filled.Notifications,
                                contentDescription = stringResource(R.string.memorial_reminder_toggle),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // V1.4 — IconButton trash visible en haut-droite
                        // pour rendre la suppression découvrable (Android
                        // n'a pas le pattern swipe-to-delete attendu comme
                        // iOS). Couleur onSurfaceVariant pour rester discret
                        // tant qu'il n'est pas pressé. Confirmation via
                        // AlertDialog pour éviter les suppressions
                        // accidentelles d'une intention sacrée.
                        IconButton(
                            onClick = { intentToDelete = intent },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.cd_delete_prayer),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // V1.4 — Footer : explication de l'astérisque, affichée seule-
            // ment si au moins une prière a une azcara à venir affichée.
            if (intents.any { it.civilDateOfDeathEpochMillis != null }) {
                item {
                    Text(
                        stringResource(R.string.memorial_starts_previous_evening),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }

    // V1.4 — AlertDialog de confirmation pour la suppression d'un Lelouy
    // Nichmat. Placé en dehors du Scaffold pour overlay correct.
    val toDelete = intentToDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { intentToDelete = null },
            title = { Text(stringResource(R.string.confirm_delete_prayer_title)) },
            text = { Text(stringResource(R.string.confirm_delete_prayer_body)) },
            confirmButton = {
                TextButton(onClick = {
                    // Annule les rappels avant la suppression : sinon les
                    // OneTimeWorkRequest restent en file et fireraient
                    // pour une prière qui n'existe plus.
                    NotificationScheduler.cancelMemorial(context, toDelete.id)
                    container.savedPrayers.delete(toDelete)
                    intentToDelete = null
                }) {
                    Text(
                        stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { intentToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
