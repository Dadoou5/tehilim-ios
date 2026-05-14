package com.david.tehilim.core.persistence

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.favoritesDataStore by preferencesDataStore(name = "tehilim_favorites")

/**
 * Stockage des Tehilim mis en favori — mirror du FavoritesStore iOS.
 *
 * V1.0 Android : DataStore Preferences (équivalent UserDefaults + JSON file).
 * iCloud sync n'existe pas sur Android — la sauvegarde Android Auto Backup
 * (déclarée dans le manifest) prend le relai automatiquement quand l'utilisateur
 * réinstalle ou change de device.
 */
class FavoritesStore(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val key = stringPreferencesKey("favorites.ids")

    private val _ids = MutableStateFlow<Set<Int>>(emptySet())
    val ids: StateFlow<Set<Int>> = _ids

    init {
        scope.launch { loadInitial() }
    }

    fun toggle(id: Int) {
        val current = _ids.value
        val next = if (current.contains(id)) current - id else current + id
        _ids.value = next
        scope.launch { persist(next) }
    }

    fun contains(id: Int): Boolean = _ids.value.contains(id)

    val sortedIds: List<Int> get() = _ids.value.sorted()

    // MARK: - I/O

    private suspend fun loadInitial() {
        val raw = context.favoritesDataStore.data.first()[key] ?: return
        val list = runCatching { Json.decodeFromString<List<Int>>(raw) }.getOrNull() ?: return
        _ids.value = list.toSet()
    }

    private suspend fun persist(values: Set<Int>) {
        val sorted: List<Int> = values.sorted()
        val json = Json.encodeToString(sorted)
        context.favoritesDataStore.edit { it[key] = json }
    }
}
