package com.david.tehilim.features.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

/** Page « Sources du contenu ». */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutContentScreen(navController: NavController) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Sources du contenu") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, null)
                }
            }
        )
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Section("Texte hébreu",
                    "Le texte hébreu vocalisé avec téamim provient de Sefaria (sefaria.org), distribué sous licence Creative Commons By 4.0.")
                Section("Traduction française",
                    "La traduction française provient de Beth Loubavitch (le-tehilim.online), utilisée avec leur autorisation.")
                Section("Traduction anglaise",
                    "La traduction anglaise est la JPS Tanakh 1917, dans le domaine public et fournie via Sefaria.")
                Section("Police hébraïque",
                    "La police d'affichage de l'hébreu est Ezra SIL SR, fournie par SIL International sous licence SIL OFL.")
                Section("Calendrier hébraïque",
                    "Les conversions de date utilisent le calendrier hébraïque officiel via ICU (Unicode CLDR).")
            }
        }
    }
}

/** Page « Confidentialité ». */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutPrivacyScreen(navController: NavController) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Confidentialité") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, null)
                }
            }
        )
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Section("Aucune donnée collectée",
                    "Tehilim ne collecte, ne transmet ni ne stocke aucune donnée personnelle.")
                Section("Stockage local",
                    "Tes préférences (langue, taille du texte, favoris, dernière lecture, Lelouy Nichmat) sont stockées localement sur ton appareil via Android DataStore, et sauvegardées par Android Auto Backup si tu actives la sauvegarde cloud Google.")
                Section("Hors-ligne",
                    "L'app fonctionne entièrement hors-ligne après installation — aucun appel réseau.")
                Section("Notifications",
                    "Les rappels quotidiens utilisent WorkManager Android et restent locaux. Aucun serveur push n'est utilisé.")
                Section("Tiers",
                    "Aucun tracker, aucune publicité, aucun service tiers de quelque nature que ce soit.")
                Section("Contact",
                    "Pour toute question : david.bouganim@gmail.com")
            }
        }
    }
}

@Composable
private fun Section(title: String, body: String) {
    Text(title, style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
    Text(body, style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
}
