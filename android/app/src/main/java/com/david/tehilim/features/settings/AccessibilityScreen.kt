package com.david.tehilim.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

/**
 * Déclaration d'accessibilité — mirror AccessibilityDeclarationView.swift V1.10.5.
 *
 * RGAA 4.1.2 + WCAG 2.1 AA. Auto-déclaration, conformité partielle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilityScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accessibilité") },
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Section("Engagement") {
                    Body("L'application Tehilim s'engage à rendre son contenu accessible conformément au Référentiel Général d'Amélioration de l'Accessibilité (RGAA 4.1.2) et au standard WCAG 2.1 niveau AA, applicables aux applications mobiles.")
                }
            }
            item {
                Section("État de conformité") {
                    Body("Statut : conformité partielle (auto-déclaration).", bold = true)
                    Body(
                        "Cette déclaration est établie sur la base d'un auto-audit. Un audit externe est prévu avant la mise en production.",
                        secondary = true
                    )
                }
            }
            item {
                Section("Caractéristiques d'accessibilité prises en charge") {
                    Bullet("Compatibilité complète avec TalkBack.")
                    Bullet("Respect de la taille de texte système (jusqu'à l'échelle accessibilité maximale).")
                    Bullet("Modes clair, sombre, et automatique.")
                    Bullet("Contrastes de texte conformes au niveau AA (≥ 4.5:1).")
                    Bullet("Cibles tactiles supérieures ou égales à 48 × 48 dp.")
                    Bullet("Aucune information transmise uniquement par la couleur.")
                    Bullet("Respect du paramètre « Supprimer les animations » du système.")
                    Bullet("Lecture TalkBack de l'hébreu en sens RTL et de la traduction en LTR, dans cet ordre.")
                }
            }
            item {
                Section("Limites connues") {
                    Bullet("Le texte hébreu est lu phonétiquement par TalkBack selon les capacités de la voix sélectionnée par l'utilisateur·rice.")
                    Bullet("La grille des 22 lettres du Tehilim 119 peut nécessiter un défilement avec une très grande taille de texte.")
                }
            }
            item {
                Section("Voies de recours") {
                    Body(
                        "Pour signaler un problème d'accessibilité : contact à définir avant publication Google Play.",
                        secondary = true
                    )
                }
            }
            item {
                HorizontalDivider()
                Text(
                    "RGAA 4.1.2 — version de l'application : ${com.david.tehilim.BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        content()
    }
}

@Composable
private fun Body(text: String, bold: Boolean = false, secondary: Boolean = false) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        color = if (secondary) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun Bullet(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            "•",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
