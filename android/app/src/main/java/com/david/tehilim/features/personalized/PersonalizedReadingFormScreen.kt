package com.david.tehilim.features.personalized

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.david.tehilim.AppContainer
import com.david.tehilim.core.model.PrayerType
import com.david.tehilim.core.model.RelationType
import com.david.tehilim.core.model.SavedPrayerIntent
import com.david.tehilim.core.service.HebrewLetterMapper
import com.david.tehilim.core.service.LetterSequenceGenerator
import com.david.tehilim.navigation.Routes
import com.david.tehilim.ui.theme.EzraSilFontFamily

/**
 * Formulaire de génération Lelouy Nichmat — mirror du PersonalizedReadingFormView iOS.
 *
 * Au tap « Générer » : auto-save dans le store + navigation vers la liste.
 * Dédup via `addOrFindExisting` — pas de doublon possible.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalizedReadingFormScreen(container: AppContainer, navController: NavController) {
    var relativeName by remember { mutableStateOf("") }
    var motherName by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf(RelationType.BEN) }

    val isValid = HebrewLetterMapper.isValidHebrewName(relativeName) &&
        HebrewLetterMapper.isValidHebrewName(motherName)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lelouy Nichmat") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Pour l'élévation de l'âme. La séquence générée correspond aux lettres du prénom du défunt, du lien (בן/בת), du prénom de sa mère, puis de נשמה.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Prénom du défunt — TextField avec filtre Hebrew live
            OutlinedTextField(
                value = relativeName,
                onValueChange = { relativeName = HebrewLetterMapper.filterHebrew(it) },
                label = { Text("Prénom du défunt (hébreu)") },
                placeholder = { Text("ex. יוסף", textAlign = TextAlign.End) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontFamily = EzraSilFontFamily, textAlign = TextAlign.End),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            // Lien
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text("Lien :", style = MaterialTheme.typography.bodyMedium)
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

            // Prénom de la mère
            OutlinedTextField(
                value = motherName,
                onValueChange = { motherName = HebrewLetterMapper.filterHebrew(it) },
                label = { Text("Prénom de la mère (hébreu)") },
                placeholder = { Text("ex. שרה", textAlign = TextAlign.End) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontFamily = EzraSilFontFamily, textAlign = TextAlign.End),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )

            // Preview
            if (isValid) {
                Text(
                    "$relativeName ${relation.hebrew} $motherName · נשמה",
                    style = TextStyle(fontFamily = EzraSilFontFamily, textAlign = TextAlign.End),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Bouton Générer = auto-save + navigation
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
                    val intent = SavedPrayerIntent(
                        title = title,
                        prayerType = PrayerType.DEFUNT,
                        relativeFirstName = relativeName,
                        relationType = relation,
                        motherFirstName = motherName,
                        generatedLetters = sequence
                    )
                    val saved = container.savedPrayers.addOrFindExisting(intent)
                    navController.navigate(Routes.personalizedList(saved.id))
                },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Générer et sauvegarder")
            }

            Text(
                "« נשמה » sera ajouté automatiquement à la fin de la séquence.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
