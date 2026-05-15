package com.david.tehilim.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.david.tehilim.core.service.NotificationScheduler

/**
 * Section « Rappel quotidien » du Settings — mirror NotificationsSection iOS.
 *
 * V1.2 : ajout d'un TimePicker dialog pour choisir l'heure du rappel.
 * L'heure est sauvegardée dans la WorkRequest WorkManager (réécrite à chaque
 * changement via NotificationScheduler.scheduleDaily).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSettingsSection() {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(false) }
    var hour by remember { mutableIntStateOf(9) }
    var minute by remember { mutableIntStateOf(0) }
    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Rappel quotidien", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = enabled, onCheckedChange = { isOn ->
                enabled = isOn
                if (isOn) NotificationScheduler.scheduleDaily(context, hour, minute)
                else NotificationScheduler.cancelDaily(context)
            })
        }
        if (enabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Heure", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { showTimePicker = true }) {
                    Text("%02d:%02d".format(hour, minute))
                }
            }
            Text(
                "Tu peux changer l'heure plus tard. Les notifications restent locales (WorkManager).",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Heure du rappel") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TimePicker(state = state)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    hour = state.hour
                    minute = state.minute
                    NotificationScheduler.scheduleDaily(context, hour, minute)
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
