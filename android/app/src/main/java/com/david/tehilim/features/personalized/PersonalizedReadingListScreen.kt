package com.david.tehilim.features.personalized

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
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
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, null)
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
            item {
                Text(intent.hebrewSubject,
                    style = TextStyle(fontFamily = EzraSilFontFamily, fontSize = 22.sp))
                Text(
                    "${intent.generatedLetters.size} lettres · « נשמה » ajouté à la fin",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(intent.generatedLetters) { item ->
                val section = container.psalm119Repository.section(forLetter = item.psalmLetterKey)
                AppCard(
                    onClick = {
                        if (section != null) {
                            navController.navigate(Routes.psalm119Section(section.index))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("${item.orderIndex + 1}", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            item.character,
                            style = TextStyle(fontFamily = EzraSilFontFamily, fontSize = 28.sp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("SOURCE", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(item.source.labelFR,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary)
                        }
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null)
                    }
                }
            }
        }
    }
}
