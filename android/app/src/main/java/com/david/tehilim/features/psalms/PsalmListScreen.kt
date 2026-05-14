package com.david.tehilim.features.psalms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.david.tehilim.AppContainer
import com.david.tehilim.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PsalmListScreen(
    container: AppContainer,
    book: Int?,
    favoritesOnly: Boolean = false,
    navController: NavController
) {
    val favorites by container.favorites.ids.collectAsState()
    val psalms = when {
        favoritesOnly -> favorites.sorted().mapNotNull { container.psalmRepository.psalm(it) }
        book != null -> container.psalmRepository.psalms(inBook = book)
        else -> container.psalmRepository.allPsalms
    }
    val title = when {
        favoritesOnly -> "Favoris"
        book != null -> "Livre $book"
        else -> "Tous les Tehilim"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(psalms) { psalm ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${psalm.id}",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier
                    )
                    Text(
                        psalm.hebrewNumber,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                        Text("Tehilim ${psalm.id}", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${psalm.verses.size} versets",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (favorites.contains(psalm.id)) {
                        Icon(
                            Icons.Outlined.Favorite,
                            contentDescription = "Favori",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    androidx.compose.material3.TextButton(onClick = {
                        navController.navigate(Routes.psalmDetail(psalm.id))
                    }) {
                        Text("Lire")
                    }
                }
                HorizontalDivider()
            }
        }
    }
}
