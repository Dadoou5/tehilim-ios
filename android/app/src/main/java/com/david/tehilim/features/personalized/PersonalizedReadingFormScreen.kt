package com.david.tehilim.features.personalized

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.david.tehilim.AppContainer
import com.david.tehilim.R
import com.david.tehilim.core.model.PrayerType
import com.david.tehilim.core.model.RelationType
import com.david.tehilim.core.model.SavedPrayerIntent
import com.david.tehilim.core.service.HebrewLetterMapper
import com.david.tehilim.core.service.LetterSequenceGenerator
import com.david.tehilim.core.service.MemorialCalculator
import com.david.tehilim.core.service.NotificationScheduler
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import com.david.tehilim.navigation.Routes
import com.david.tehilim.ui.theme.EzraSilFontFamily

/**
 * Point d'entrée du formulaire. En mode édition ([editIntentId] non-null), on
 * résout la prière depuis le store puis on délègue au contenu qui pré-remplit
 * ses champs. En création, [editIntentId] est null.
 */
@Composable
fun PersonalizedReadingFormScreen(
    container: AppContainer,
    navController: NavController,
    editIntentId: String? = null
) {
    val intents by container.savedPrayers.intents.collectAsState()
    val editIntent = editIntentId?.let { id -> intents.firstOrNull { it.id == id } }
    // En édition, attendre que la prière soit résolue avant de seeder les
    // champs (le store est déjà chargé quand on arrive depuis le détail, donc
    // ce cas « not found » est transitoire / quasi inexistant).
    if (editIntentId != null && editIntent == null) return
    PersonalizedReadingFormContent(container, navController, editIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonalizedReadingFormContent(
    container: AppContainer,
    navController: NavController,
    editIntent: SavedPrayerIntent?
) {
    val isEditing = editIntent != null

    // Pré-remplissage en mode édition (sinon valeurs par défaut de création).
    var relativeName by remember { mutableStateOf(editIntent?.relativeFirstName ?: "") }
    var motherName by remember { mutableStateOf(editIntent?.motherFirstName ?: "") }
    var relation by remember { mutableStateOf(editIntent?.relationType ?: RelationType.BEN) }

    // V1.4 — Commémoration : état local de la section azcara.
    var civilDateOfDeath by remember {
        mutableStateOf(editIntent?.civilDateOfDeathEpochMillis?.let { Date(it) })
    }
    var remindersEnabled by remember { mutableStateOf(editIntent?.remindersEnabled ?: false) }
    var notify7d by remember { mutableStateOf(editIntent?.notifySevenDaysBefore ?: true) }
    var notifyDay by remember { mutableStateOf(editIntent?.notifySameDay ?: true) }
    var showDatePicker by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val hasHebrewKeyboard = remember { isHebrewKeyboardInstalled(context) }

    val isValid = HebrewLetterMapper.isValidHebrewName(relativeName) &&
        HebrewLetterMapper.isValidHebrewName(motherName)

    // V1.4 — FocusRequester pour chaîner les deux champs hébreux :
    // tap "Suivant" sur le clavier du 1er champ → focus le 2e sans
    // refermer le clavier. Tap "Terminé" sur le 2e → ferme le clavier.
    val relativeFocus = remember { FocusRequester() }
    val motherFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (isEditing) R.string.title_edit_prayer
                            else R.string.title_lelouy_nichmat
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
                // imePadding pousse le contenu au-dessus du clavier soft
                // (couplé à `adjustResize` du manifest) → la zone visible
                // ne descend jamais sous le clavier.
                .imePadding()
                // Scroll vertical pour que les champs restent accessibles
                // même si le clavier ouvert réduit la hauteur disponible.
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bannière de conseil si pas de clavier hébreu installé
            if (!hasHebrewKeyboard) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.msg_enable_hebrew_keyboard_title),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            stringResource(R.string.msg_enable_hebrew_keyboard_body),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        TextButton(onClick = { openSystemKeyboardSettings(context) }) {
                            Text(stringResource(R.string.action_open_keyboard_settings))
                        }
                    }
                }
            }

            // Bannière Lelouy Nichmat — mirror V1.10.2 iOS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Outlined.LocalFireDepartment,
                    null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        stringResource(R.string.msg_lelouy_banner_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        stringResource(R.string.msg_lelouy_banner_body),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Section : le défunt — HebrewKeyboardTextField force le clavier
            // hébreu via imeHintLocales (équivalent du textInputMode iOS V1.10.1).
            // V1.4 — action IME "Suivant" → bascule sur le champ mère sans
            // refermer le clavier.
            HebrewKeyboardTextField(
                value = relativeName,
                onValueChange = { relativeName = it },
                label = stringResource(R.string.placeholder_relative_name),
                placeholder = stringResource(R.string.placeholder_relative_example),
                imeAction = HebrewFieldImeAction.Next,
                focusRequester = relativeFocus,
                onImeAction = { motherFocus.requestFocus() },
                modifier = Modifier.fillMaxWidth()
            )

            // Lien (Ben / Bat)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.label_link), style = MaterialTheme.typography.bodyMedium)
                FilterChip(
                    selected = relation == RelationType.BEN,
                    onClick = { relation = RelationType.BEN },
                    label = {
                        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text("בן", style = TextStyle(fontFamily = EzraSilFontFamily))
                        }
                    }
                )
                FilterChip(
                    selected = relation == RelationType.BAT,
                    onClick = { relation = RelationType.BAT },
                    label = {
                        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text("בת", style = TextStyle(fontFamily = EzraSilFontFamily))
                        }
                    }
                )
            }

            // Section : sa mère. V1.4 — action IME "Terminé" → ferme le
            // clavier et libère la vue pour la prévisualisation + bouton
            // Générer.
            HebrewKeyboardTextField(
                value = motherName,
                onValueChange = { motherName = it },
                label = stringResource(R.string.placeholder_mother_name),
                placeholder = stringResource(R.string.placeholder_mother_example),
                imeAction = HebrewFieldImeAction.Done,
                focusRequester = motherFocus,
                onImeAction = {
                    focusManager.clearFocus()
                    keyboard?.hide()
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Aperçu hébraïque
            if (isValid) {
                Text(
                    stringResource(R.string.label_preview),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "$relativeName ${relation.hebrew} $motherName · נשמה",
                    style = TextStyle(fontFamily = EzraSilFontFamily, textAlign = TextAlign.End),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ─── V1.4 — Section Commémoration ────────────────────────────
            HorizontalDivider()
            Text(
                stringResource(R.string.memorial_section_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Date du décès (optionnelle)
            val dateFormatter = remember {
                DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault())
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.memorial_date_label),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        civilDateOfDeath?.let { dateFormatter.format(it) }
                            ?: stringResource(R.string.memorial_date_pick),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = { showDatePicker = true }) {
                    Text(stringResource(R.string.memorial_date_pick))
                }
                if (civilDateOfDeath != null) {
                    TextButton(onClick = { civilDateOfDeath = null }) {
                        Text(stringResource(R.string.memorial_date_clear))
                    }
                }
            }

            // Toggle global rappel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.memorial_reminder_toggle),
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(checked = remindersEnabled, onCheckedChange = { remindersEnabled = it })
            }

            if (remindersEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.memorial_reminder_7d),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(checked = notify7d, onCheckedChange = { notify7d = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.memorial_reminder_day),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(checked = notifyDay, onCheckedChange = { notifyDay = it })
                }
            }

            // Aperçu "Prochaine azcara" — l'astérisque rappelle que le
            // Hebrew day commence au coucher du soleil de la veille civile,
            // explication dans le helper en dessous.
            val deathDate = civilDateOfDeath
            if (deathDate != null) {
                // V1.4 — Clé todayEpochDay : invalidation au passage à minuit.
                val todayEpochDay = java.time.LocalDate.now().toEpochDay()
                val next = remember(deathDate, todayEpochDay) {
                    MemorialCalculator.nextYahrzeit(deathDate)
                }
                if (next != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(R.string.memorial_next_azcara),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${dateFormatter.format(next)}*",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        stringResource(R.string.memorial_starts_previous_evening),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Helper + warnings inline
            Text(
                stringResource(R.string.memorial_helper),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (remindersEnabled && civilDateOfDeath == null) {
                Text(
                    stringResource(R.string.memorial_need_date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // DatePickerDialog M3
            if (showDatePicker) {
                val state = rememberDatePickerState(
                    initialSelectedDateMillis = civilDateOfDeath?.time
                        ?: System.currentTimeMillis()
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            state.selectedDateMillis?.let { civilDateOfDeath = Date(it) }
                            showDatePicker = false
                        }) { Text(stringResource(R.string.action_ok)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                ) {
                    DatePicker(state = state)
                }
            }
            // ───────────────────────────────────────────────────────────────

            // Bouton Générer + footer
            Button(
                onClick = {
                    val sequence = LetterSequenceGenerator.generate(
                        relativeName = relativeName,
                        relation = relation,
                        motherName = motherName,
                        prayerType = PrayerType.DEFUNT
                    )
                    val title = LetterSequenceGenerator.makeTitle(
                        prayerType = PrayerType.DEFUNT,
                        relativeName = relativeName,
                        relation = relation,
                        motherName = motherName
                    )
                    val hebrewDeath = civilDateOfDeath?.let { MemorialCalculator.hebrewYMD(it) }

                    if (isEditing) {
                        // Mode édition : mise à jour EN PLACE (conserve id +
                        // createdAt). Séquence + date hébraïque recalculées,
                        // progression de lecture bornée si la séquence raccourcit.
                        val clampedLastRead = editIntent!!.lastReadIndex?.let { lri ->
                            if (lri >= sequence.size) (sequence.size - 1).takeIf { it >= 0 } else lri
                        }
                        val updated = editIntent.copy(
                            title = title,
                            relativeFirstName = relativeName,
                            relationType = relation,
                            motherFirstName = motherName,
                            generatedLetters = sequence,
                            lastReadIndex = clampedLastRead,
                            civilDateOfDeathEpochMillis = civilDateOfDeath?.time,
                            hebrewDateOfDeath = hebrewDeath,
                            remindersEnabled = remindersEnabled,
                            notifySevenDaysBefore = notify7d,
                            notifySameDay = notifyDay
                        )
                        container.savedPrayers.update(updated)
                        // rescheduleMemorial annule d'abord les anciens rappels.
                        NotificationScheduler.rescheduleMemorial(context, updated)
                        // Retour au détail, qui relit le store réactivement.
                        navController.popBackStack()
                    } else {
                        val intent = SavedPrayerIntent(
                            title = title,
                            prayerType = PrayerType.DEFUNT,
                            relativeFirstName = relativeName,
                            relationType = relation,
                            motherFirstName = motherName,
                            generatedLetters = sequence,
                            // V1.4 — Commémoration
                            civilDateOfDeathEpochMillis = civilDateOfDeath?.time,
                            hebrewDateOfDeath = hebrewDeath,
                            remindersEnabled = remindersEnabled,
                            notifySevenDaysBefore = notify7d,
                            notifySameDay = notifyDay
                        )
                        val saved = container.savedPrayers.addOrFindExisting(intent)
                        // V1.4 — schedule (no-op si conditions pas réunies)
                        NotificationScheduler.rescheduleMemorial(context, saved)
                        navController.navigate(Routes.personalizedList(saved.id)) {
                            popUpTo(Routes.PERSONALIZED_FORM) { inclusive = true }
                        }
                    }
                },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(
                        if (isEditing) R.string.action_save
                        else R.string.action_generate_save
                    )
                )
            }

            Text(
                stringResource(R.string.msg_neshama_appended_form),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Détecte si l'utilisateur a au moins un IME hébreu installé/activé.
 * Inspecte la liste des input method subtypes via `InputMethodManager`.
 */
private fun isHebrewKeyboardInstalled(context: android.content.Context): Boolean {
    val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
    val imis = imm.enabledInputMethodList ?: return false
    for (imi in imis) {
        val subtypes = imm.getEnabledInputMethodSubtypeList(imi, true) ?: continue
        for (st in subtypes) {
            val locale = st.languageTag.ifBlank {
                @Suppress("DEPRECATION") st.locale
            }
            if (locale?.startsWith("he", ignoreCase = true) == true ||
                locale?.startsWith("iw", ignoreCase = true) == true) {
                return true
            }
        }
    }
    return false
}

/** Ouvre les paramètres clavier système Android. */
private fun openSystemKeyboardSettings(context: android.content.Context) {
    val intent = android.content.Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS)
    intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
    runCatching { context.startActivity(intent) }
}
