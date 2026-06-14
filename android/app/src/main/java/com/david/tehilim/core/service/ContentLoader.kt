package com.david.tehilim.core.service

import android.content.Context
import com.david.tehilim.core.model.CommentaryItem
import com.david.tehilim.core.model.CommentaryKind
import com.david.tehilim.core.model.CommentaryRepository
import com.david.tehilim.core.model.DailyRules
import com.david.tehilim.core.model.LifeCase
import com.david.tehilim.core.model.VerseCommentary
import com.david.tehilim.core.model.Psalm
import com.david.tehilim.core.model.Psalm119Section
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * Charge les fichiers JSON embarqués dans `assets/data/`.
 *
 * Le dossier `assets/data/` est rempli automatiquement au build par la task
 * Gradle `copySharedAssets` qui copie depuis `../data/` (même source que iOS).
 *
 * Format JSON identique à iOS — kotlinx.serialization avec
 * `ignoreUnknownKeys = true` pour tolérer toute évolution future.
 */
class ContentLoader(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = false
    }

    fun loadPsalms(): List<Psalm> {
        val payload = readAsset("data/psalms.json")
        // Structure réelle : { "version": ..., "source": ..., "language": ..., "psalms": [...] }
        val wrapper = json.decodeFromString<PsalmsWrapper>(payload)
        return wrapper.psalms
    }

    fun loadLifeCases(): List<LifeCase> {
        val payload = readAsset("data/life_cases.json")
        // Structure réelle : { "$schema": ..., "version": ..., "categories": [...] }
        val wrapper = json.decodeFromString<LifeCasesWrapper>(payload)
        return wrapper.categories
    }

    fun loadPsalm119Sections(): List<Psalm119Section> {
        return runCatching {
            // Le fichier s'appelle psalm_119_sections.json côté repo.
            // Structure : { "$schema": ..., "version": ..., "psalmId": 119, "sections": [...] }
            val payload = readAsset("data/psalm_119_sections.json")
            val wrapper = json.decodeFromString<Psalm119Wrapper>(payload)
            wrapper.sections
        }.getOrElse {
            // Fallback : 22 sections déterministes si le JSON est absent ou cassé.
            generateDefault119Sections()
        }
    }

    fun loadDailyRules(): DailyRules {
        val payload = readAsset("data/daily_reading_rules.json")
        return json.decodeFromString<DailyRules>(payload)
    }

    /** V2.4 — commentaires (Rashi, Metzudat David). Repo vide si absent/cassé. */
    fun loadCommentaries(): CommentaryRepository = runCatching {
        val payload = readAsset("data/commentaries.json")
        val wrapper = json.decodeFromString<CommentariesWrapper>(payload)
        val byKey = HashMap<String, List<VerseCommentary>>()
        for ((p, verses) in wrapper.byPsalm) {
            for ((v, sources) in verses) {
                val list = ArrayList<VerseCommentary>()
                for (kind in CommentaryKind.values()) {
                    sources[kind.key]?.forEach { item ->
                        list.add(VerseCommentary(kind, item.lemma, item.he, item.en, item.fr))
                    }
                }
                if (list.isNotEmpty()) byKey["$p:$v"] = list
            }
        }
        CommentaryRepository(byKey)
    }.getOrElse { CommentaryRepository(emptyMap()) }

    // MARK: - Internals

    private fun readAsset(path: String): String {
        return try {
            context.assets.open(path).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            throw IllegalStateException("Impossible de lire l'asset $path", e)
        }
    }

    @kotlinx.serialization.Serializable
    private data class PsalmsWrapper(val psalms: List<Psalm>)

    /// Le fichier life_cases.json utilise « categories » comme clé top-level
    /// (pas « cases »). On garde la dénomination JSON canonique du repo.
    @kotlinx.serialization.Serializable
    private data class LifeCasesWrapper(val categories: List<LifeCase>)

    @kotlinx.serialization.Serializable
    private data class Psalm119Wrapper(
        val psalmId: Int = 119,
        val sections: List<Psalm119Section>
    )

    @kotlinx.serialization.Serializable
    private data class CommentariesWrapper(
        val byPsalm: Map<String, Map<String, Map<String, List<CommentaryItem>>>> = emptyMap()
    )

    private fun generateDefault119Sections(): List<Psalm119Section> {
        val letters = listOf(
            "א" to "Aleph", "ב" to "Beth", "ג" to "Gimel", "ד" to "Dalet",
            "ה" to "Hé", "ו" to "Vav", "ז" to "Zayin", "ח" to "Heth",
            "ט" to "Tet", "י" to "Yod", "כ" to "Kaf", "ל" to "Lamed",
            "מ" to "Mem", "נ" to "Noun", "ס" to "Samekh", "ע" to "Ayin",
            "פ" to "Pé", "צ" to "Tsadé", "ק" to "Qof", "ר" to "Resh",
            "ש" to "Shin", "ת" to "Tav"
        )
        return letters.mapIndexed { idx, (letter, name) ->
            Psalm119Section(
                index = idx + 1,
                letter = letter,
                name = name,
                verseStart = idx * 8 + 1,
                verseEnd = idx * 8 + 8
            )
        }
    }
}
