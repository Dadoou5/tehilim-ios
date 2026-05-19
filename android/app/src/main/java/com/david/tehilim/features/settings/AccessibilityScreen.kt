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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.david.tehilim.R

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
                title = { Text(stringResource(R.string.title_accessibility)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.cd_back))
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
                Section(stringResource(R.string.section_engagement)) {
                    Body(stringResource(R.string.a11y_engagement_body))
                }
            }
            item {
                Section(stringResource(R.string.section_compliance_status)) {
                    Body(stringResource(R.string.a11y_status_main), bold = true)
                    Body(stringResource(R.string.a11y_status_secondary), secondary = true)
                }
            }
            item {
                Section(stringResource(R.string.section_a11y_features)) {
                    Bullet(stringResource(R.string.a11y_feature_talkback))
                    Bullet(stringResource(R.string.a11y_feature_text_size))
                    Bullet(stringResource(R.string.a11y_feature_themes))
                    Bullet(stringResource(R.string.a11y_feature_contrast))
                    Bullet(stringResource(R.string.a11y_feature_targets))
                    Bullet(stringResource(R.string.a11y_feature_no_color_only))
                    Bullet(stringResource(R.string.a11y_feature_animations))
                    Bullet(stringResource(R.string.a11y_feature_rtl_ltr))
                }
            }
            item {
                Section(stringResource(R.string.section_known_limits)) {
                    Bullet(stringResource(R.string.a11y_limit_hebrew_tts))
                    Bullet(stringResource(R.string.a11y_limit_119_grid))
                }
            }
            item {
                Section(stringResource(R.string.section_recourse)) {
                    Body(stringResource(R.string.a11y_recourse_body), secondary = true)
                }
            }
            item {
                HorizontalDivider()
                Text(
                    stringResource(R.string.label_a11y_rgaa_version, com.david.tehilim.BuildConfig.VERSION_NAME),
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
