package com.david.tehilim.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.david.tehilim.AppContainer
import com.david.tehilim.core.model.AppLanguage
import com.david.tehilim.core.model.AppTheme
import com.david.tehilim.core.model.DailyMode
import com.david.tehilim.core.model.TextMode
import com.david.tehilim.core.model.TextSize
import com.david.tehilim.core.model.VerseNumberStyle
import com.david.tehilim.ui.theme.frenchBodyStyle
import com.david.tehilim.ui.theme.hebrewBodyStyle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(container: AppContainer, navController: androidx.navigation.NavController? = null) {
    val scope = rememberCoroutineScope()
    val prefs = container.preferences

    val appLanguage by prefs.appLanguage.collectAsState(initial = AppLanguage.SYSTEM)
    val theme by prefs.theme.collectAsState(initial = AppTheme.SYSTEM)
    val textMode by prefs.textMode.collectAsState(initial = TextMode.HEBREW)
    val textSizeHebrew by prefs.textSizeHebrew.collectAsState(initial = TextSize.MEDIUM)
    val textSizeFR by prefs.textSizeFR.collectAsState(initial = TextSize.MEDIUM)
    val translationFR by prefs.translationFR.collectAsState(initial = false)
    val verseNumStyle by prefs.verseNumberStyle.collectAsState(initial = VerseNumberStyle.HEBREW)
    val dailyMode by prefs.dailyMode.collectAsState(initial = DailyMode.MONTHLY)

    var showRestartAlert by remember { mutableStateOf(false) }

    if (showRestartAlert) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRestartAlert = false },
            title = { Text("Redémarrage requis") },
            text = {
                Text(
                    "La traduction des Tehilim a basculé immédiatement. Pour que l'interface change aussi, ferme l'app puis rouvre-la."
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showRestartAlert = false }) {
                    Text("OK")
                }
            }
        )
    }

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
                    scope.launch {
                        prefs.setAppLanguage(it)
                        showRestartAlert = true
                    }
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

            // Aperçus texte (mirror PrimaryPreviewRow / FrenchPreviewRow iOS)
            item {
                HebrewPreviewRow(mode = textMode, size = textSizeHebrew)
            }
            item {
                FrenchPreviewRow(size = textSizeFR)
            }

            item { HorizontalDivider() }
            item { SectionHeader("Lecture quotidienne") }
            item {
                EnumSettingRow("Mode", dailyMode, DailyMode.entries) {
                    scope.launch { prefs.setDailyMode(it) }
                }
            }

            item { HorizontalDivider() }
            item { SectionHeader("Rappel quotidien") }
            item { NotificationsSettingsSection() }

            item { HorizontalDivider() }
            item { SectionHeader("Accessibilité") }
            item {
                androidx.compose.material3.TextButton(
                    onClick = { navController?.navigate("about/accessibility") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Déclaration d'accessibilité",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null)
                }
            }

            item { HorizontalDivider() }
            item { SectionHeader("À propos") }
            item {
                androidx.compose.material3.TextButton(
                    onClick = { navController?.navigate("about/content") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Sources du contenu",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null)
                }
            }
            item {
                androidx.compose.material3.TextButton(
                    onClick = { navController?.navigate("about/privacy") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Confidentialité",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null)
                }
            }
            item {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        "Version ${com.david.tehilim.BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium
                    )
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
private fun HebrewPreviewRow(mode: TextMode, size: TextSize) {
    val sample = "שִׁיר לַמַּעֲלוֹת אֶשָּׂא עֵינַי"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            "Aperçu",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (mode == TextMode.HEBREW) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
            ) {
                Text(
                    sample,
                    style = hebrewBodyStyle(size.scale),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
        } else {
            Text(
                "Chir lamaalot essa enaï",
                style = hebrewBodyStyle(size.scale).copy(
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun FrenchPreviewRow(size: TextSize) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            "Aperçu traduction",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Cantique des degrés. Je lève mes yeux vers les montagnes…",
            style = frenchBodyStyle(size.scale),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
    var expanded by remember { mutableStateOf(false) }
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
