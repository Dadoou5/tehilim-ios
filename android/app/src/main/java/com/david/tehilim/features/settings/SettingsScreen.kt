package com.david.tehilim.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.david.tehilim.AppContainer
import com.david.tehilim.core.model.AppLanguage
import com.david.tehilim.core.model.AppTheme
import com.david.tehilim.core.model.DailyMode
import com.david.tehilim.core.model.TextMode
import com.david.tehilim.core.model.TextSize
import com.david.tehilim.core.model.VerseNumberStyle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(container: AppContainer) {
    val scope = rememberCoroutineScope()
    val prefs = container.preferences

    val appLanguage by prefs.appLanguage.collectAsState(initial = AppLanguage.SYSTEM)
    val theme by prefs.theme.collectAsState(initial = AppTheme.SYSTEM)
    val textMode by prefs.textMode.collectAsState(initial = TextMode.HEBREW)
    val textSizeHebrew by prefs.textSizeHebrew.collectAsState(initial = TextSize.MEDIUM)
    val textSizeFR by prefs.textSizeFR.collectAsState(initial = TextSize.MEDIUM)
    val translationFR by prefs.translationFR.collectAsState(initial = true)
    val verseNumStyle by prefs.verseNumberStyle.collectAsState(initial = VerseNumberStyle.HEBREW)
    val dailyMode by prefs.dailyMode.collectAsState(initial = DailyMode.MONTHLY)

    Scaffold(topBar = { TopAppBar(title = { Text("Réglages") }) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { SectionHeader("Langue") }
            item {
                EnumSettingRow("Langue de l'app", appLanguage, AppLanguage.entries) {
                    scope.launch { prefs.setAppLanguage(it) }
                }
            }
            item {
                SwitchRow("Afficher la traduction par défaut", translationFR) {
                    scope.launch { prefs.setTranslationFR(it) }
                }
            }

            item { HorizontalDivider() }
            item { SectionHeader("Affichage") }
            item {
                EnumSettingRow("Thème", theme, AppTheme.entries) {
                    scope.launch { prefs.setTheme(it) }
                }
            }
            item {
                EnumSettingRow("Mode du texte", textMode, TextMode.entries) {
                    scope.launch { prefs.setTextMode(it) }
                }
            }
            item {
                EnumSettingRow("Taille de l'hébreu", textSizeHebrew, TextSize.entries) {
                    scope.launch { prefs.setTextSizeHebrew(it) }
                }
            }
            item {
                EnumSettingRow("Taille de la traduction", textSizeFR, TextSize.entries) {
                    scope.launch { prefs.setTextSizeFR(it) }
                }
            }
            item {
                EnumSettingRow("Numérotation", verseNumStyle, VerseNumberStyle.entries) {
                    scope.launch { prefs.setVerseNumberStyle(it) }
                }
            }

            item { HorizontalDivider() }
            item { SectionHeader("Lecture quotidienne") }
            item {
                EnumSettingRow("Mode", dailyMode, DailyMode.entries) {
                    scope.launch { prefs.setDailyMode(it) }
                }
            }

            item { HorizontalDivider() }
            item { SectionHeader("À propos") }
            item {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("Version 1.0.0", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Texte hébreu : Sefaria · Traduction française : Beth Loubavitch · Traduction anglaise : JPS 1917",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun SwitchRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
private inline fun <reified T : Enum<T>> EnumSettingRow(
    label: String,
    current: T,
    options: Iterable<T>,
    crossinline onChange: (T) -> Unit
) {
    var expanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        androidx.compose.foundation.layout.Box {
            androidx.compose.material3.TextButton(onClick = { expanded = true }) {
                Text(humanLabel(current))
            }
            androidx.compose.material3.DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { opt ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(humanLabel(opt)) },
                        onClick = {
                            expanded = false
                            onChange(opt)
                        }
                    )
                }
            }
        }
    }
}

private fun <T : Enum<T>> humanLabel(value: T): String = when (value) {
    is AppLanguage -> when (value) {
        AppLanguage.SYSTEM -> "Système"
        AppLanguage.FR -> "Français"
        AppLanguage.EN -> "English"
    }
    is AppTheme -> value.label
    is TextMode -> value.label
    is TextSize -> value.label
    is VerseNumberStyle -> value.label
    is DailyMode -> value.label
    else -> value.name
}
