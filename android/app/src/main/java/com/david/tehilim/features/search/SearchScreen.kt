package com.david.tehilim.features.search

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.david.tehilim.AppContainer
import com.david.tehilim.navigation.Routes

/**
 * Recherche de Tehilim — par numéro arabe, hébreu, ou mot du titre.
 * Mirror simplifié du SearchView SwiftUI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(container: AppContainer, navController: NavController) {
    var query by remember { mutableStateOf("") }

    val results by remember {
        derivedStateOf {
            if (query.isBlank()) {
                emptyList()
            } else {
                val q = query.lowercase().trim()
                container.psalmRepository.allPsalms.filter { p ->
                    p.id.toString() == q ||
                        p.hebrewNumber.contains(query) ||
                        (p.hebrewTitle?.contains(query) == true)
                }.take(50)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rechercher") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Fermer")
                    }
                }
            )
        }
    ) { padding ->
        androidx.compose.foundation.layout.Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Numéro, hébreu ou mot-clé") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            if (query.isBlank()) {
                Text(
                    "Tape un numéro (« 23 », « כג »), une lettre hébraïque ou un mot du titre.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                return@Column
            }
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp)) {
                items(results) { psalm ->
                    androidx.compose.material3.TextButton(
                        onClick = {
                            navController.popBackStack()
                            navController.navigate(Routes.psalmDetail(psalm.id))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Tehilim ${psalm.id} · ${psalm.hebrewNumber}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
