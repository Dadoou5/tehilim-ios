package com.david.tehilim.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.david.tehilim.R
import com.david.tehilim.core.persistence.Preferences
import com.david.tehilim.core.service.ShabbatCalculator
import kotlinx.coroutines.launch

/**
 * Section « Mode Chabbat » des Réglages — toggle + choix de la position
 * (automatique GPS, ou une ville). Mirror du ShabbatSection iOS.
 */
@Composable
fun ShabbatSettingsSection(prefs: Preferences) {
    val scope = rememberCoroutineScope()
    val enabled by prefs.shabbatEnabled.collectAsState(initial = true)
    val cityId by prefs.shabbatCityId.collectAsState(initial = "")
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.shabbat_enable), style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = enabled,
                onCheckedChange = { scope.launch { prefs.setShabbatEnabled(it) } }
            )
        }

        if (enabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.shabbat_position), style = MaterialTheme.typography.bodyMedium)
                Box {
                    TextButton(onClick = { menuExpanded = true }) {
                        Text(currentCityLabel(cityId))
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.shabbat_position_auto)) },
                            onClick = {
                                scope.launch { prefs.setShabbatCityId("") }
                                menuExpanded = false
                            }
                        )
                        ShabbatCalculator.cities.forEach { city ->
                            DropdownMenuItem(
                                text = { Text(city.nameFR) },
                                onClick = {
                                    scope.launch { prefs.setShabbatCityId(city.id) }
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Text(
            stringResource(R.string.shabbat_footer),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun currentCityLabel(cityId: String): String =
    if (cityId.isEmpty()) stringResource(R.string.shabbat_position_auto)
    else ShabbatCalculator.city(cityId)?.nameFR ?: stringResource(R.string.shabbat_position_auto)
