package com.david.tehilim.core.service

import android.content.Context
import com.david.tehilim.core.model.DailyRules
import com.david.tehilim.core.model.LifeCase
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
        // Le JSON top-level a la forme { "psalms": [...] } comme iOS
        val wrapper = json.decodeFromString<PsalmsWrapper>(payload)
        return wrapper.psalms
    }

    fun loadLifeCases(): List<LifeCase> {
        val payload = readAsset("data/life_cases.json")
        val wrapper = json.decodeFromString<LifeCasesWrapper>(payload)
        return wrapper.cases
    }

    fun loadPsalm119Sections(): List<Psalm119Section> {
        val payload = readAsset("data/psalm119.json")
        return runCatching {
            val wrapper = json.decodeFromString<Psalm119Wrapper>(payload)
            wrapper.sections
        }.getOrElse {
            // Fallback : si le JSON 119 n'est pas exporté séparément,
            // on génère les 22 sections de manière déterministe (8 versets chacune).
            generateDefault119Sections()
        }
    }

    fun loadDailyRules(): DailyRules {
        val payload = readAsset("data/daily_reading_rules.json")
        return json.decodeFromString<DailyRules>(payload)
    }

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

    @kotlinx.serialization.Serializable
    private data class LifeCasesWrapper(val cases: List<LifeCase>)

    @kotlinx.serialization.Serializable
    private data class Psalm119Wrapper(val sections: List<Psalm119Section>)

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
