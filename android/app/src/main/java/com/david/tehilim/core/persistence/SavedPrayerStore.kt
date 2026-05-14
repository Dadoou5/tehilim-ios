package com.david.tehilim.core.persistence

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.david.tehilim.core.model.PrayerType
import com.david.tehilim.core.model.RelationType
import com.david.tehilim.core.model.SavedPrayerIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.savedPrayersDataStore by preferencesDataStore(name = "tehilim_saved_prayers")

/**
 * Persiste les Lelouy Nichmat sauvegardés.
 * Mirror du SavedPrayerStore iOS — auto-save + dédup intégrés.
 */
class SavedPrayerStore(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val key = stringPreferencesKey("saved_prayers")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _intents = MutableStateFlow<List<SavedPrayerIntent>>(emptyList())
    val intents: StateFlow<List<SavedPrayerIntent>> = _intents

    init {
        scope.launch { loadInitial() }
    }

    // MARK: - CRUD

    fun add(intent: SavedPrayerIntent) {
        _intents.value = listOf(intent) + _intents.value
        scope.launch { persist() }
    }

    /** Cherche un doublon par paramètres (prénom + lien + mère). */
    fun findExisting(
        relativeFirstName: String,
        relationType: RelationType,
        motherFirstName: String
    ): SavedPrayerIntent? = _intents.value.firstOrNull {
        it.relativeFirstName == relativeFirstName &&
            it.relationType == relationType &&
            it.motherFirstName == motherFirstName
    }

    /** Add ou retourne l'existant — garantit pas de doublon. */
    fun addOrFindExisting(intent: SavedPrayerIntent): SavedPrayerIntent {
        val existing = findExisting(
            relativeFirstName = intent.relativeFirstName,
            relationType = intent.relationType,
            motherFirstName = intent.motherFirstName
        )
        if (existing != null) return existing
        add(intent)
        return intent
    }

    fun delete(intent: SavedPrayerIntent) {
        _intents.value = _intents.value.filter { it.id != intent.id }
        scope.launch { persist() }
    }

    fun updateLastReadIndex(intentId: String, lastReadIndex: Int) {
        _intents.value = _intents.value.map {
            if (it.id == intentId) it.copy(lastReadIndex = lastReadIndex) else it
        }
        scope.launch { persist() }
    }

    fun filtered(type: PrayerType): List<SavedPrayerIntent> =
        _intents.value.filter { it.prayerType == type }

    // MARK: - I/O

    private suspend fun loadInitial() {
        val raw = context.savedPrayersDataStore.data.first()[key] ?: return
        val list = runCatching { json.decodeFromString<List<SavedPrayerIntent>>(raw) }.getOrNull() ?: return
        _intents.value = list
    }

    private suspend fun persist() {
        val list: List<SavedPrayerIntent> = _intents.value
        val payload = json.encodeToString(list)
        context.savedPrayersDataStore.edit { it[key] = payload }
    }
}
