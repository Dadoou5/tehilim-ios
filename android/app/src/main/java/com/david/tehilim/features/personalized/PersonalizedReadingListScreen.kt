package com.david.tehilim.features.personalized

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.david.tehilim.AppContainer
import com.david.tehilim.navigation.Routes
import com.david.tehilim.ui.components.AppCard
import com.david.tehilim.ui.theme.EzraSilFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalizedReadingListScreen(container: AppContainer, intentId: String, navController: NavController) {
    val intents by container.savedPrayers.intents.collectAsState()
    val intent = intents.firstOrNull { it.id == intentId } ?: run {
        Text("Lelouy Nichmat introuvable")
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(intent.title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header card avec badge Lelouy + sujet hébraïque (mirror SwiftUI V1.10.0)
            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Outlined.LocalFireDepartment,
                                null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                "Lelouy Nichmat",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Text(
                            intent.hebrewSubject,
                            style = TextStyle(fontFamily = EzraSilFontFamily, fontSize = 24.sp),
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        )
                        Text(
                            "נשמה",
                            style = TextStyle(fontFamily = EzraSilFontFamily, fontSize = 18.sp),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Bouton « Reprendre la lecture » si lastReadIndex défini
            val lastIdx = intent.lastReadIndex
            if (lastIdx != null && lastIdx in intent.generatedLetters.indices) {
                item {
                    val lastItem = intent.generatedLetters[lastIdx]
                    AppCard(
                        onClick = {
                            val section = container.psalm119Repository.sectionByLetter(lastItem.psalmLetterKey)
                            if (section != null) {
                                navController.navigate(
                                    "${Routes.psalm119Section(section.index)}?intentId=${intent.id}&pos=$lastIdx"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Outlined.PlayArrow,
                                null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Reprendre la lecture", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "À la lettre ${lastIdx + 1} sur ${intent.generatedLetters.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null)
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.size(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Séquence de lecture",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        "${intent.generatedLetters.size} lettres",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(intent.generatedLetters) { item ->
                val section = container.psalm119Repository.sectionByLetter(item.psalmLetterKey)
                AppCard(
                    onClick = {
                        if (section != null) {
                            navController.navigate(
                                "${Routes.psalm119Section(section.index)}?intentId=${intent.id}&pos=${item.orderIndex}"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "${item.orderIndex + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            item.character,
                            style = TextStyle(fontFamily = EzraSilFontFamily, fontSize = 28.sp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "SOURCE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                item.source.labelFR,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null)
                    }
                }
            }

            item {
                Text(
                    "« נשמה » a été ajouté automatiquement à la fin de la séquence.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}
