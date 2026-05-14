package com.david.tehilim.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.david.tehilim.core.service.NotificationScheduler

/**
 * Section « Rappel quotidien » de l'écran Réglages.
 * Mirror du NotificationsSection iOS.
 */
@Composable
fun NotificationsSettingsSection() {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(false) }
    var hour by remember { mutableIntStateOf(9) }
    var minute by remember { mutableIntStateOf(0) }

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
                if (isOn) {
                    NotificationScheduler.scheduleDaily(context, hour, minute)
                } else {
                    NotificationScheduler.cancelDaily(context)
                }
            })
        }
        if (enabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Heure", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { /* TODO TimePicker dialog */ }) {
                    Text("%02d:%02d".format(hour, minute))
                }
            }
            Text(
                "Tu peux changer l'heure plus tard depuis Réglages → Notifications de l'app dans le système.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
