package com.david.tehilim.features.personalized

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.david.tehilim.R
import com.david.tehilim.core.service.MemorialCalculator
import com.david.tehilim.core.service.PrayerShareLink
import com.david.tehilim.ui.theme.EzraSilFontFamily
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * Aperçu + confirmation d'import d'une prière reçue par lien partagé
 * (`tehilim://prayer?...`). Mirror du PrayerImportView.swift iOS.
 *
 * Affiche le sujet hébraïque, le type, et la prochaine azcara si une date du
 * décès est transmise — puis confirme l'import (ou annule). Évite les imports
 * accidentels et rend explicite ce qui sera ajouté à « Mes prières ».
 */
@Composable
fun PrayerImportDialog(
    payload: PrayerShareLink.Payload,
    alreadyExists: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormatter = remember {
        DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
    }
    val nextAzcara = remember(payload.civilDateOfDeathEpochMillis) {
        payload.civilDateOfDeathEpochMillis?.let {
            MemorialCalculator.nextYahrzeit(Date(it))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Filled.LocalFireDepartment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text(stringResource(R.string.import_prayer_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    payload.prayerType.saveActionTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    payload.hebrewSubject,
                    style = TextStyle(fontFamily = EzraSilFontFamily),
                )
                if (nextAzcara != null) {
                    Text(
                        "${stringResource(R.string.memorial_next_azcara)} : ${dateFormatter.format(nextAzcara)}*",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        stringResource(R.string.memorial_starts_previous_evening),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (alreadyExists) {
                    Text(
                        stringResource(R.string.import_prayer_already),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(
                        if (alreadyExists) R.string.action_open else R.string.action_import
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
