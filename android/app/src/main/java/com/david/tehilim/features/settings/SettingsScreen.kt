package com.david.tehilim.features.settings

import android.app.Activity
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.david.tehilim.AppContainer
import com.david.tehilim.R
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
    // V1.3.6 — Activity pour pouvoir forcer recreate() au changement de langue
    val activity = LocalContext.current as? Activity

    val appLanguage by prefs.appLanguage.collectAsState(initial = AppLanguage.SYSTEM)
    val theme by prefs.theme.collectAsState(initial = AppTheme.SYSTEM)
    val textMode by prefs.textMode.collectAsState(initial = TextMode.HEBREW)
    val textSizeHebrew by prefs.textSizeHebrew.collectAsState(initial = TextSize.MEDIUM)
    val textSizeFR by prefs.textSizeFR.collectAsState(initial = TextSize.MEDIUM)
    val translationFR by prefs.translationFR.collectAsState(initial = false)
    val verseNumStyle by prefs.verseNumberStyle.collectAsState(initial = VerseNumberStyle.HEBREW)
    val dailyMode by prefs.dailyMode.collectAsState(initial = DailyMode.MONTHLY)


    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.title_settings)) }) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { SectionHeader(stringResource(R.string.section_language)) }
            item {
                EnumSettingRow(stringResource(R.string.label_app_language), appLanguage, AppLanguage.entries) { newLang ->
                    scope.launch {
                        Log.i("TehilimLang", "click: $newLang (sdk=${Build.VERSION.SDK_INT})")
                        prefs.setAppLanguage(newLang)
                        Log.i("TehilimLang", "datastore written")
                        val tag = when (newLang) {
                            AppLanguage.FR -> "fr"
                            AppLanguage.EN -> "en"
                            AppLanguage.SYSTEM -> ""
                        }
                        val newLocales = if (tag.isEmpty()) {
                            LocaleListCompat.getEmptyLocaleList()
                        } else {
                            LocaleListCompat.forLanguageTags(tag)
                        }
                        AppCompatDelegate.setApplicationLocales(newLocales)
                        Log.i("TehilimLang", "AppCompat set tag=$tag, now=${AppCompatDelegate.getApplicationLocales()}")
                        // V1.3.9 — recreate() uniquement sur API < 33. Sur API
                        // 33+, LocaleManager recrée l'Activity automatiquement
                        // (configChanges retiré dans le manifest) ; un recreate
                        // manuel ferait double action et peut racer avec le
                        // refresh des Resources OS.
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            Log.i("TehilimLang", "manual recreate (SDK < 33)")
                            activity?.recreate()
                        } else {
                            Log.i("TehilimLang", "OS handles recreate (SDK >= 33)")
                        }
                    }
                }
            }
            item {
                SwitchRow(stringResource(R.string.label_show_translation_default), translationFR) {
                    scope.launch { prefs.setTranslationFR(it) }
                }
            }

            item { HorizontalDivider() }
            item { SectionHeader(stringResource(R.string.section_display)) }
            item {
                EnumSettingRow(stringResource(R.string.label_theme), theme, AppTheme.entries) {
                    scope.launch { prefs.setTheme(it) }
                }
            }
            item {
                EnumSettingRow(stringResource(R.string.label_text_mode), textMode, TextMode.entries) {
                    scope.launch { prefs.setTextMode(it) }
                }
            }
            item {
                EnumSettingRow(stringResource(R.string.label_hebrew_size), textSizeHebrew, TextSize.entries) {
                    scope.launch { prefs.setTextSizeHebrew(it) }
                }
            }
            item {
                EnumSettingRow(stringResource(R.string.label_translation_size), textSizeFR, TextSize.entries) {
                    scope.launch { prefs.setTextSizeFR(it) }
                }
            }
            item {
                EnumSettingRow(stringResource(R.string.label_numbering), verseNumStyle, VerseNumberStyle.entries) {
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
            item { SectionHeader(stringResource(R.string.section_daily_reading)) }
            item {
                EnumSettingRow(stringResource(R.string.label_mode), dailyMode, DailyMode.entries) {
                    scope.launch { prefs.setDailyMode(it) }
                }
            }

            item { HorizontalDivider() }
            item { SectionHeader(stringResource(R.string.section_daily_reminder)) }
            item { NotificationsSettingsSection() }

            item { HorizontalDivider() }
            item { SectionHeader(stringResource(R.string.section_accessibility)) }
            item {
                androidx.compose.material3.TextButton(
                    onClick = { navController?.navigate("about/accessibility") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.title_accessibility_declaration),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null)
                }
            }

            item { HorizontalDivider() }
            item { SectionHeader(stringResource(R.string.section_about)) }
            item {
                androidx.compose.material3.TextButton(
                    onClick = { navController?.navigate("about/content") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.title_about_content),
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
                        stringResource(R.string.title_about_privacy),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null)
                }
            }
            item {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        stringResource(R.string.label_version, com.david.tehilim.BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        stringResource(R.string.msg_sources_footer),
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
            stringResource(R.string.label_preview),
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
                stringResource(R.string.msg_preview_hebrew_phonetic),
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
            stringResource(R.string.label_translation_preview),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            stringResource(R.string.msg_preview_french),
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

/**
 * Résout le libellé localisé d'un enum de préférence. Tous les enums migrés
 * V1.3.0 exposent un `labelRes` (R.string.*) qu'on résout via stringResource.
 */
@Composable
private fun <T : Enum<T>> humanLabel(value: T): String = when (value) {
    is AppLanguage -> stringResource(value.labelRes)
    is AppTheme -> stringResource(value.labelRes)
    is TextMode -> stringResource(value.labelRes)
    is TextSize -> stringResource(value.labelRes)
    is VerseNumberStyle -> stringResource(value.labelRes)
    is DailyMode -> stringResource(value.labelRes)
    else -> value.name
}
