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
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.chainsDataStore by preferencesDataStore(name = "tehilim_chains")

/** Snapshot local d'une chaîne (compte rendu), conservé par le créateur. */
@Serializable
data class ChainArchiveSnapshot(
    val id: String,
    val name: String,
    val intentionWire: String,
    val detail: String,
    val creatorName: String,
    val readingDeadlineMillis: Long,
    val archivedAtMillis: Long,
    /** psalmId ("1".."150") → nom du lecteur. */
    val assignments: Map<String, String>
) {
    val subjectLine: String
        get() = if (detail.isBlank()) name else "$name — $detail"
}

/**
 * Persiste localement : (1) les ids de chaînes connues (créées/rejointes) pour
 * « Mes chaînes » ; (2) les archives créateur. Mirror Android de
 * ChainArchiveStore.swift. DataStore + JSON (kotlinx.serialization).
 */
class ChainArchiveStore(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val knownKey = stringPreferencesKey("chains.known.ids")
    private val archivesKey = stringPreferencesKey("chains.archives")
    private val json = Json { ignoreUnknownKeys = true }

    private val _knownChainIds = MutableStateFlow<List<String>>(emptyList())
    val knownChainIds: StateFlow<List<String>> = _knownChainIds

    private val _archives = MutableStateFlow<List<ChainArchiveSnapshot>>(emptyList())
    val archives: StateFlow<List<ChainArchiveSnapshot>> = _archives

    init {
        scope.launch { loadInitial() }
    }

    private suspend fun loadInitial() {
        val prefs = context.chainsDataStore.data.first()
        prefs[knownKey]?.let { raw ->
            runCatching { json.decodeFromString<List<String>>(raw) }.getOrNull()?.let {
                _knownChainIds.value = it
            }
        }
        prefs[archivesKey]?.let { raw ->
            runCatching { json.decodeFromString<List<ChainArchiveSnapshot>>(raw) }.getOrNull()?.let {
                _archives.value = it
            }
        }
    }

    fun remember(chainId: String) {
        if (chainId.isBlank()) return
        val next = (listOf(chainId) + _knownChainIds.value.filter { it != chainId })
        _knownChainIds.value = next
        scope.launch { persistKnown(next) }
    }

    fun forget(chainId: String) {
        val next = _knownChainIds.value.filter { it != chainId }
        _knownChainIds.value = next
        scope.launch { persistKnown(next) }
    }

    fun saveArchive(snapshot: ChainArchiveSnapshot) {
        val next = listOf(snapshot) + _archives.value.filter { it.id != snapshot.id }
        _archives.value = next
        scope.launch { persistArchives(next) }
    }

    fun deleteArchive(id: String) {
        val next = _archives.value.filter { it.id != id }
        _archives.value = next
        scope.launch { persistArchives(next) }
    }

    private suspend fun persistKnown(ids: List<String>) {
        context.chainsDataStore.edit { it[knownKey] = json.encodeToString(ids) }
    }

    private suspend fun persistArchives(list: List<ChainArchiveSnapshot>) {
        context.chainsDataStore.edit { it[archivesKey] = json.encodeToString(list) }
    }
}
