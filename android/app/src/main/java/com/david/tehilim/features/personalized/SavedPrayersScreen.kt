package com.david.tehilim.features.personalized

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Inbox
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
import androidx.compose.runtime.remember
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

    // V1.4 — Tri : (1) prières avec date du décès → triées par prochaine
    // azcara croissante (les commémorations à venir en premier),
    // (2) prières sans date → triées par date de création décroissante,
    // ajoutées après le premier groupe.
    val intents = remember(rawIntents) {
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
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(intent.hebrewSubject,
                            style = TextStyle(fontFamily = EzraSilFontFamily))
                        Text(
                            stringResource(R.string.label_letters_count, intent.generatedLetters.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // V1.4 — Prochaine azcara si date du décès renseignée.
                        val millis = intent.civilDateOfDeathEpochMillis
                        if (millis != null) {
                            val next = remember(millis) {
                                MemorialCalculator.nextYahrzeit(Date(millis))
                            }
                            if (next != null) {
                                Text(
                                    "${stringResource(R.string.memorial_next_azcara)} : ${dateFormatter.format(next)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
