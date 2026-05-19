package com.david.tehilim.features.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.david.tehilim.R
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
    var permissionDenied by remember { mutableStateOf(false) }

    // Android 13+ : on doit demander POST_NOTIFICATIONS au runtime.
    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            enabled = true
            permissionDenied = false
            NotificationScheduler.scheduleDaily(context, hour, minute)
        } else {
            enabled = false
            permissionDenied = true
        }
    }

    fun toggleEnabled(isOn: Boolean) {
        if (!isOn) {
            enabled = false
            NotificationScheduler.cancelDaily(context)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                enabled = true
                NotificationScheduler.scheduleDaily(context, hour, minute)
            } else {
                permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            enabled = true
            NotificationScheduler.scheduleDaily(context, hour, minute)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.label_daily_reminder), style = MaterialTheme.typography.bodyMedium)
            Switch(checked = enabled, onCheckedChange = ::toggleEnabled)
        }
        if (permissionDenied) {
            Text(
                stringResource(R.string.msg_permission_denied_notifications),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        if (enabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.label_hour), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { showTimePicker = true }) {
                    Text("%02d:%02d".format(hour, minute))
                }
            }
            Text(
                stringResource(R.string.msg_reminder_time_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.label_reminder_time)) },
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
                    Text(stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
