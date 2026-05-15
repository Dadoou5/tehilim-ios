package com.david.tehilim.core.service

import com.david.tehilim.core.model.Psalm

/**
 * Résultat structuré d'une recherche Tehilim.
 *
 * - [exactMatch] : Tehilim trouvé par numéro arabe ou gematria hébraïque.
 * - [suggestions] : Tehilim voisins (id-1, id+1) si match, sinon liste par défaut.
 * - [interpretation] : libellé court "Tehilim N · hebrewNumber" pour feedback UI.
 */
data class SearchQueryResult(
    val exactMatch: Psalm?,
    val suggestions: List<Psalm>,
    val interpretation: String?
)

/**
 * Interprète une requête utilisateur ("23", "psaume 23", "כג", "tehilim 91"…).
 * Mirror direct de `SearchInterpreter.swift` iOS.
 */
class SearchInterpreter(private val repository: PsalmRepository) {

    companion object {
        private val STRIP_WORDS = setOf(
            "tehilim", "tehillim", "psaume", "psaumes", "psalm", "תהילים", "תהלים"
        )

        private val DEFAULT_SUGGESTION_IDS = listOf(23, 27, 91, 121, 130, 150)
    }

    fun interpret(rawQuery: String): SearchQueryResult {
        val trimmed = rawQuery.trim().lowercase()
        if (trimmed.isEmpty()) {
            return SearchQueryResult(
                exactMatch = null,
                suggestions = defaultSuggestions(),
                interpretation = null
            )
        }

        // 1. Retirer les mots prefix ("psaume 23" → "23")
        var cleaned = trimmed
        for (word in STRIP_WORDS) {
            cleaned = cleaned.replace(word, " ")
        }
        cleaned = cleaned
            .replace("\"", "")
            .replace("'", "")
            .replace("״", "")
            .replace("׳", "")
            .trim()

        // 2. Numéro arabe
        extractArabicNumber(cleaned)?.let { n ->
            repository.psalm(n)?.let { psalm ->
                return SearchQueryResult(
                    exactMatch = psalm,
                    suggestions = nearby(psalm.id),
                    interpretation = "Tehilim ${psalm.id} · ${psalm.hebrewNumber}"
                )
            }
        }

        // 3. Numéro hébraïque (gematria)
        HebrewNumerals.toInt(cleaned)?.let { n ->
            repository.psalm(n)?.let { psalm ->
                return SearchQueryResult(
                    exactMatch = psalm,
                    suggestions = nearby(psalm.id),
                    interpretation = "Tehilim ${psalm.id} · ${psalm.hebrewNumber}"
                )
            }
        }

        // 4. Pas de match exact — suggestions par défaut
        return SearchQueryResult(
            exactMatch = null,
            suggestions = defaultSuggestions(),
            interpretation = null
        )
    }

    private fun extractArabicNumber(s: String): Int? {
        val digits = s.filter { it.isDigit() }
        if (digits.isEmpty()) return null
        val n = digits.toIntOrNull() ?: return null
        return if (n in 1..150) n else null
    }

    private fun nearby(id: Int): List<Psalm> =
        listOfNotNull(repository.psalm(id - 1), repository.psalm(id + 1))

    private fun defaultSuggestions(): List<Psalm> =
        DEFAULT_SUGGESTION_IDS.mapNotNull { repository.psalm(it) }
}
