package com.david.tehilim.features.chains

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.david.tehilim.AppContainer
import com.david.tehilim.R
import com.david.tehilim.core.model.ChainIntention
import com.david.tehilim.ui.components.AppCard
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val errCreate = stringResource(R.string.chain_create_error)
    val canCreate = name.isNotBlank() && creatorName.isNotBlank() &&
        readingDeadline > System.currentTimeMillis() + selectionHours * 3600_000L && !creating

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.chain_new)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.cd_back))
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
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Hero ──
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()) {
                Box(
                    Modifier.size(56.dp).background(
                        MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Link, null,
                        tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.chain_create_blurb),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // ── Intention ──
            SectionTitle(stringResource(R.string.chain_intention))
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    ChainIntention.entries.forEachIndexed { i, kind ->
                        IntentionRow(
                            icon = intentionIcon(kind),
                            label = stringResource(intentionLabel(kind)),
                            selected = intention == kind,
                            onClick = { intention = kind }
                        )
                        if (i < ChainIntention.entries.lastIndex) {
                            androidx.compose.material3.HorizontalDivider(
                                Modifier.padding(start = 56.dp),
                                color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }

            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text(stringResource(R.string.chain_name)) },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = detail, onValueChange = { detail = it },
                label = { Text(stringResource(detailPlaceholder(intention))) },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            // Aperçu live du sujet
            val subject = if (detail.isBlank()) stringResource(intentionLabel(intention))
                          else "${stringResource(intentionLabel(intention))} — ${detail.trim()}"
            Text(subject,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth())

            // ── Délais ──
            SectionTitle(stringResource(R.string.chain_selection_duration))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                durations.forEach { h ->
                    FilterChip(
                        selected = selectionHours == h,
                        onClick = { selectionHours = h },
                        label = { Text(if (h < 24) "$h h" else "${h / 24} j") }
                    )
                }
            }

            AppCard(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.CalendarMonth, null,
                        tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.chain_reading_end_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(df.format(Date(readingDeadline)),
                            style = MaterialTheme.typography.titleSmall)
                    }
                }
            }

            // ── Toi ──
            OutlinedTextField(
                value = creatorName, onValueChange = { creatorName = it },
                label = { Text(stringResource(R.string.chain_your_name_all)) },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium)
            }

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
                            error = errCreate
                        } finally {
                            creating = false
                        }
                    }
                },
                enabled = canCreate,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.chain_create_button)) }

            // Note de consentement
            Row(
                Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.Info, null, Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.chain_consent),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) } }
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface)
}

@Composable
private fun IntentionRow(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selected = selected, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null,
            tint = if (selected) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge)
        RadioButton(selected = selected, onClick = onClick)
    }
}

internal fun intentionIcon(kind: ChainIntention): ImageVector = when (kind) {
    ChainIntention.LELOUY -> Icons.Outlined.LocalFireDepartment
    ChainIntention.REFOUA -> Icons.Outlined.Favorite
    ChainIntention.REUSSITE -> Icons.Outlined.Star
}

@StringRes
internal fun intentionLabel(kind: ChainIntention): Int = when (kind) {
    ChainIntention.LELOUY -> R.string.chain_intention_lelouy
    ChainIntention.REFOUA -> R.string.chain_intention_refoua
    ChainIntention.REUSSITE -> R.string.chain_intention_reussite
}

@StringRes
private fun detailPlaceholder(kind: ChainIntention): Int = when (kind) {
    ChainIntention.LELOUY -> R.string.chain_detail_deceased
    ChainIntention.REFOUA -> R.string.chain_detail_sick
    ChainIntention.REUSSITE -> R.string.chain_detail_success
}
