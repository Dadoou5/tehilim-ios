package com.david.tehilim.features.chains

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.david.tehilim.AppContainer
import com.david.tehilim.core.model.ChainIntention
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChainScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onCreated: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var intention by remember { mutableStateOf(ChainIntention.LELOUY) }
    var detail by remember { mutableStateOf("") }
    var creatorName by remember { mutableStateOf("") }
    var selectionHours by remember { mutableStateOf(24) }
    var readingDeadline by remember {
        mutableStateOf(System.currentTimeMillis() + 7L * 24 * 3600 * 1000)
    }
    var creating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val durations = listOf(1, 3, 6, 12, 24, 48, 72)
    val df = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }
    val canCreate = name.isNotBlank() && creatorName.isNotBlank() &&
        readingDeadline > System.currentTimeMillis() + selectionHours * 3600_000L && !creating

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nouvelle chaîne") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Nom de la chaîne") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            Text("Intention", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ChainIntention.entries.forEach { kind ->
                    FilterChip(
                        selected = intention == kind,
                        onClick = { intention = kind },
                        label = { Text(intentionLabel(kind)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            OutlinedTextField(
                value = detail, onValueChange = { detail = it },
                label = { Text(detailPlaceholder(intention)) },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            Text("Durée de sélection", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                durations.forEach { h ->
                    FilterChip(
                        selected = selectionHours == h,
                        onClick = { selectionHours = h },
                        label = { Text(if (h < 24) "$h h" else "${h / 24} j") }
                    )
                }
            }

            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Fin de lecture : ${df.format(Date(readingDeadline))}")
            }

            OutlinedTextField(
                value = creatorName, onValueChange = { creatorName = it },
                label = { Text("Ton nom (visible de tous)") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }

            Button(
                onClick = {
                    creating = true; error = null
                    scope.launch {
                        try {
                            val id = container.chains.createChain(
                                name = name.trim(),
                                intention = intention,
                                detail = detail.trim(),
                                selectionDurationMillis = selectionHours * 3600_000L,
                                readingDeadlineMillis = readingDeadline,
                                creatorName = creatorName.trim()
                            )
                            container.chainArchive.remember(id)
                            onCreated(id)
                        } catch (e: Exception) {
                            error = "Création impossible. Vérifie ta connexion."
                        } finally {
                            creating = false
                        }
                    }
                },
                enabled = canCreate,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Créer la chaîne") }

            Text(
                "Ton nom et l'intention sont enregistrés dans le cloud le temps de la chaîne, puis supprimés automatiquement après la lecture.",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start
            )
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = readingDeadline)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { readingDeadline = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Annuler") } }
        ) { DatePicker(state = state) }
    }
}

internal fun intentionLabel(kind: ChainIntention): String = when (kind) {
    ChainIntention.LELOUY -> "Lelouy Nichmat"
    ChainIntention.REFOUA -> "Refoua Chelema"
    ChainIntention.REUSSITE -> "Pour la réussite de"
}

private fun detailPlaceholder(kind: ChainIntention): String = when (kind) {
    ChainIntention.LELOUY -> "Prénom du défunt"
    ChainIntention.REFOUA -> "Prénom du malade"
    ChainIntention.REUSSITE -> "Am Israël, un proche…"
}
