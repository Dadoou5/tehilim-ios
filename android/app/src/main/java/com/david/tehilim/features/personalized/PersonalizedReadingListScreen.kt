package com.david.tehilim.features.personalized

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.david.tehilim.AppContainer
import com.david.tehilim.R
import com.david.tehilim.core.service.MemorialCalculator
import com.david.tehilim.core.service.NotificationScheduler
import com.david.tehilim.core.service.PendingMemorialReminder
import com.david.tehilim.navigation.Routes
import com.david.tehilim.ui.components.AppCard
import com.david.tehilim.ui.theme.EzraSilFontFamily
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalizedReadingListScreen(container: AppContainer, intentId: String, navController: NavController) {
    val intents by container.savedPrayers.intents.collectAsState()
    val intent = intents.firstOrNull { it.id == intentId } ?: run {
        Text(stringResource(R.string.msg_lelouy_nichmat_not_found))
        return
    }
    val context = LocalContext.current

    // V1.4 — Charge les rappels d'azcara actuellement plannifiés pour
    // cet intent. LaunchedEffect re-fire si l'intentId change (= navigation
    // vers une autre prière) — pas besoin de re-fetch sinon.
    var pendingReminders by remember {
        mutableStateOf<List<PendingMemorialReminder>>(emptyList())
    }
    LaunchedEffect(intent.id) {
        pendingReminders = NotificationScheduler.pendingMemorial(context, intent.id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(intent.title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.cd_back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header card avec badge Lelouy + sujet hébraïque (mirror SwiftUI V1.10.0)
            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Outlined.LocalFireDepartment,
                                null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                stringResource(R.string.title_lelouy_nichmat),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Text(
                            intent.hebrewSubject,
                            style = TextStyle(fontFamily = EzraSilFontFamily, fontSize = 24.sp),
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        )
                        Text(
                            "נשמה",
                            style = TextStyle(fontFamily = EzraSilFontFamily, fontSize = 18.sp),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // V1.4 — Card « Prochaine azcara » si la date du décès est
            // renseignée. Placée juste après le header sujet pour mettre
            // l'info en avant. Calcul `next` fait dans le scope `item {}`
            // (qui est @Composable) — sortir du `let` pour préserver le
            // contexte composable nécessaire à `remember`.
            val deathMillis = intent.civilDateOfDeathEpochMillis
            if (deathMillis != null) {
                item {
                    // V1.4 — Clé `todayEpochDay` : invalidation au passage
                    // à minuit pour que l'azcara passée laisse place à la
                    // suivante. Sinon le `remember(deathMillis)` cacherait
                    // indéfiniment (la date du décès ne change jamais).
                    // PAS de `remember` autour de todayEpochDay — il doit
                    // être recalculé à chaque recomposition pour qu'à la
                    // prochaine recomposition post-minuit il change.
                    val todayEpochDay = java.time.LocalDate.now().toEpochDay()
                    val next = remember(deathMillis, todayEpochDay) {
                        MemorialCalculator.nextYahrzeit(Date(deathMillis))
                    }
                    if (next != null) {
                        val dateFmt = remember {
                            DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault())
                        }
                        Column(modifier = Modifier.fillMaxWidth()) {
                            AppCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Event,
                                        null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            stringResource(R.string.memorial_next_azcara),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        // V1.4 — astérisque + footer note pour
                                        // rappeler que le Hebrew day commence
                                        // la veille au soir.
                                        Text(
                                            "${dateFmt.format(next)}*",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }
                                }
                            }
                            Text(
                                stringResource(R.string.memorial_starts_previous_evening),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }
                    }
                }
            }

            // V1.4 — Section diagnostic : rappels d'azcara plannifiés
            // (= WorkRequest ENQUEUED). Permet de vérifier visuellement
            // que les notifs sont bien posées sans devoir attendre le
            // déclenchement réel.
            if (pendingReminders.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.memorial_scheduled_reminders),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(pendingReminders) { reminder ->
                    val dateFmt = remember {
                        DateFormat.getDateTimeInstance(
                            DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault()
                        )
                    }
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Filled.Notifications,
                                null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(
                                        if (reminder.kind == PendingMemorialReminder.Kind.SevenDays)
                                            R.string.memorial_reminder_7d
                                        else R.string.memorial_reminder_day
                                    ),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (reminder.triggerEpochMillis > 0) {
                                    Text(
                                        dateFmt.format(Date(reminder.triggerEpochMillis)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bouton « Reprendre la lecture » si lastReadIndex défini
            val lastIdx = intent.lastReadIndex
            if (lastIdx != null && lastIdx in intent.generatedLetters.indices) {
                item {
                    val lastItem = intent.generatedLetters[lastIdx]
                    AppCard(
                        onClick = {
                            val section = container.psalm119Repository.sectionByLetter(lastItem.psalmLetterKey)
                            if (section != null) {
                                navController.navigate(
                                    "${Routes.psalm119Section(section.index)}?intentId=${intent.id}&pos=$lastIdx"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Outlined.PlayArrow,
                                null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.msg_resume_reading),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    stringResource(
                                        R.string.label_at_letter_n_of_m,
                                        lastIdx + 1,
                                        intent.generatedLetters.size
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null)
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.size(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.section_reading_sequence),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        stringResource(R.string.label_letters_count, intent.generatedLetters.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(intent.generatedLetters) { item ->
                val section = container.psalm119Repository.sectionByLetter(item.psalmLetterKey)
                AppCard(
                    onClick = {
                        if (section != null) {
                            navController.navigate(
                                "${Routes.psalm119Section(section.index)}?intentId=${intent.id}&pos=${item.orderIndex}"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "${item.orderIndex + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            item.character,
                            style = TextStyle(fontFamily = EzraSilFontFamily, fontSize = 28.sp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.section_label_source),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                item.source.localizedLabel(context),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null)
                    }
                }
            }

            item {
                Text(
                    stringResource(R.string.msg_neshama_appended_list),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}
