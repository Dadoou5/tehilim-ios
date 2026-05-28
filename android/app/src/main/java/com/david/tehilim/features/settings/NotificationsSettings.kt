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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.david.tehilim.R
import com.david.tehilim.core.persistence.Preferences
import com.david.tehilim.core.service.NotificationScheduler
import kotlinx.coroutines.launch

/**
 * Section « Rappel quotidien » du Settings — mirror NotificationsSection iOS.
 *
 * V1.2 : ajout d'un TimePicker dialog pour choisir l'heure du rappel.
 *
 * V1.4 build 17 — **fix bug user remonté** : le toggle et l'heure ne se
 * sauvegardaient pas. Avant : state local `remember { mutableStateOf(...) }`
 * qui meurt à chaque recomposition / relance de l'app. Maintenant : prefs
 * lues via Flow DataStore et écrites via setters → l'état est persisté
 * et reflète la réalité du scheduling WorkManager.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSettingsSection(prefs: Preferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // V1.4 — État lu depuis DataStore (persistant cross-sessions).
    val enabled by prefs.notificationEnabled.collectAsState(initial = false)
    val hour by prefs.notificationHour.collectAsState(initial = 9)
    val minute by prefs.notificationMinute.collectAsState(initial = 0)

    var showTimePicker by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }

    // Android 13+ : on doit demander POST_NOTIFICATIONS au runtime.
    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDenied = false
            scope.launch {
                prefs.setNotificationEnabled(true)
                NotificationScheduler.scheduleDaily(context, hour, minute)
            }
        } else {
            permissionDenied = true
            // Pas besoin de toucher la pref : elle reste false par défaut.
        }
    }

    fun toggleEnabled(isOn: Boolean) {
        if (!isOn) {
            scope.launch {
                prefs.setNotificationEnabled(false)
                NotificationScheduler.cancelDaily(context)
            }
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                scope.launch {
                    prefs.setNotificationEnabled(true)
                    NotificationScheduler.scheduleDaily(context, hour, minute)
                }
            } else {
                permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            scope.launch {
                prefs.setNotificationEnabled(true)
                NotificationScheduler.scheduleDaily(context, hour, minute)
            }
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
                    val newHour = state.hour
                    val newMinute = state.minute
                    scope.launch {
                        prefs.setNotificationTime(newHour, newMinute)
                        // Re-schedule avec la nouvelle heure si le rappel
                        // est activé (sinon pas besoin de WorkManager call).
                        if (enabled) {
                            NotificationScheduler.scheduleDaily(context, newHour, newMinute)
                        }
                    }
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
