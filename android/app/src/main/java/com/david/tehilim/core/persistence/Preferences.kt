package com.david.tehilim.core.persistence

import android.content.Context
import androidx.datastore.preferences.core.Preferences as DSPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.david.tehilim.core.model.AppLanguage
import com.david.tehilim.core.model.AppTheme
import com.david.tehilim.core.model.DailyMode
import com.david.tehilim.core.model.TextMode
import com.david.tehilim.core.model.TextSize
import com.david.tehilim.core.model.VerseNumberStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "tehilim_prefs")

/**
 * Mirror du Preferences.swift iOS — toutes les options globales de l'app.
 *
 * Utilise Jetpack DataStore (recommandé Android moderne) au lieu de SharedPreferences.
 * Chaque champ est exposé comme `Flow<T>` à consommer dans Compose via `collectAsState`.
 */
class Preferences(private val context: Context) {

    private object Keys {
        val APP_LANGUAGE = stringPreferencesKey("pref.app.language")
        val THEME = stringPreferencesKey("pref.theme")
        val TEXT_MODE = stringPreferencesKey("pref.textMode")
        val TEXT_SIZE_HEBREW = stringPreferencesKey("pref.textSizeHebrew")
        val TEXT_SIZE_FR = stringPreferencesKey("pref.textSizeFR")
        val TRANSLATION_FR = booleanPreferencesKey("pref.translationFR")
        val VERSE_NUMBER_STYLE = stringPreferencesKey("pref.verseNumberStyle")
        val DAILY_MODE = stringPreferencesKey("pref.dailyMode")
        val LAST_READ_PSALM_ID = intPreferencesKey("pref.lastReadPsalmId")
        val ONBOARDING_DONE = booleanPreferencesKey("pref.onboarding.done")
        val SEARCH_RECENTS = stringPreferencesKey("pref.search.recents")
    }

    val appLanguage: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        when (prefs[Keys.APP_LANGUAGE]) {
            "fr" -> AppLanguage.FR
            "en" -> AppLanguage.EN
            else -> AppLanguage.SYSTEM
        }
    }

    val theme: Flow<AppTheme> = context.dataStore.data.map { prefs ->
        runCatching { AppTheme.valueOf(prefs[Keys.THEME] ?: "SYSTEM") }.getOrElse { AppTheme.SYSTEM }
    }

    val textMode: Flow<TextMode> = context.dataStore.data.map { prefs ->
        runCatching { TextMode.valueOf(prefs[Keys.TEXT_MODE] ?: "HEBREW") }.getOrElse { TextMode.HEBREW }
    }

    val textSizeHebrew: Flow<TextSize> = context.dataStore.data.map { prefs ->
        runCatching { TextSize.valueOf(prefs[Keys.TEXT_SIZE_HEBREW] ?: "MEDIUM") }.getOrElse { TextSize.MEDIUM }
    }

    val textSizeFR: Flow<TextSize> = context.dataStore.data.map { prefs ->
        runCatching { TextSize.valueOf(prefs[Keys.TEXT_SIZE_FR] ?: "MEDIUM") }.getOrElse { TextSize.MEDIUM }
    }

    val translationFR: Flow<Boolean> = context.dataStore.data.map { it[Keys.TRANSLATION_FR] ?: false }

    val verseNumberStyle: Flow<VerseNumberStyle> = context.dataStore.data.map { prefs ->
        runCatching { VerseNumberStyle.valueOf(prefs[Keys.VERSE_NUMBER_STYLE] ?: "HEBREW") }
            .getOrElse { VerseNumberStyle.HEBREW }
    }

    val dailyMode: Flow<DailyMode> = context.dataStore.data.map { prefs ->
        runCatching { DailyMode.valueOf(prefs[Keys.DAILY_MODE] ?: "MONTHLY") }.getOrElse { DailyMode.MONTHLY }
    }

    val lastReadPsalmId: Flow<Int?> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_READ_PSALM_ID]?.takeIf { it > 0 }
    }

    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }

    /** Liste des IDs Tehilim récemment consultés via recherche (max 10, plus récent en tête). */
    val searchRecents: Flow<List<Int>> = context.dataStore.data.map { prefs ->
        prefs[Keys.SEARCH_RECENTS].orEmpty()
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
    }

    // MARK: - Setters

    suspend fun setAppLanguage(lang: AppLanguage) =
        context.dataStore.edit { it[Keys.APP_LANGUAGE] = lang.storageValue }

    suspend fun setTheme(value: AppTheme) =
        context.dataStore.edit { it[Keys.THEME] = value.name }

    suspend fun setTextMode(value: TextMode) =
        context.dataStore.edit { it[Keys.TEXT_MODE] = value.name }

    suspend fun setTextSizeHebrew(value: TextSize) =
        context.dataStore.edit { it[Keys.TEXT_SIZE_HEBREW] = value.name }

    suspend fun setTextSizeFR(value: TextSize) =
        context.dataStore.edit { it[Keys.TEXT_SIZE_FR] = value.name }

    suspend fun setTranslationFR(value: Boolean) =
        context.dataStore.edit { it[Keys.TRANSLATION_FR] = value }

    suspend fun setVerseNumberStyle(value: VerseNumberStyle) =
        context.dataStore.edit { it[Keys.VERSE_NUMBER_STYLE] = value.name }

    suspend fun setDailyMode(value: DailyMode) =
        context.dataStore.edit { it[Keys.DAILY_MODE] = value.name }

    suspend fun setLastReadPsalmId(value: Int) =
        context.dataStore.edit { it[Keys.LAST_READ_PSALM_ID] = value }

    suspend fun setOnboardingDone(value: Boolean) =
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = value }

    /** Ajoute [id] en tête de la liste des récents (déduplique, plafonne à 10). */
    suspend fun rememberSearchRecent(id: Int) = context.dataStore.edit { prefs ->
        val current = prefs[Keys.SEARCH_RECENTS].orEmpty()
            .split(",").mapNotNull { it.trim().toIntOrNull() }
        val updated = (listOf(id) + current.filter { it != id }).take(10)
        prefs[Keys.SEARCH_RECENTS] = updated.joinToString(",")
    }
}
