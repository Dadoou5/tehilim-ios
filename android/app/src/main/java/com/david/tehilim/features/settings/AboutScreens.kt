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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.david.tehilim.R

/** Page « Sources du contenu ». */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutContentScreen(navController: NavController) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.title_about_content)) },
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
                Section(
                    stringResource(R.string.about_content_hebrew_title),
                    stringResource(R.string.about_content_hebrew_body)
                )
                Section(
                    stringResource(R.string.about_content_fr_title),
                    stringResource(R.string.about_content_fr_body)
                )
                Section(
                    stringResource(R.string.about_content_en_title),
                    stringResource(R.string.about_content_en_body)
                )
                Section(
                    stringResource(R.string.about_content_font_title),
                    stringResource(R.string.about_content_font_body)
                )
                Section(
                    stringResource(R.string.about_content_calendar_title),
                    stringResource(R.string.about_content_calendar_body)
                )
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
            title = { Text(stringResource(R.string.title_about_privacy)) },
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
                Section(
                    stringResource(R.string.about_privacy_none_title),
                    stringResource(R.string.about_privacy_none_body)
                )
                Section(
                    stringResource(R.string.about_privacy_local_title),
                    stringResource(R.string.about_privacy_local_body)
                )
                Section(
                    stringResource(R.string.about_privacy_offline_title),
                    stringResource(R.string.about_privacy_offline_body)
                )
                Section(
                    stringResource(R.string.about_privacy_notifications_title),
                    stringResource(R.string.about_privacy_notifications_body)
                )
                Section(
                    stringResource(R.string.about_privacy_thirdparty_title),
                    stringResource(R.string.about_privacy_thirdparty_body)
                )
                Section(
                    stringResource(R.string.about_privacy_contact_title),
                    stringResource(R.string.about_privacy_contact_body)
                )
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
