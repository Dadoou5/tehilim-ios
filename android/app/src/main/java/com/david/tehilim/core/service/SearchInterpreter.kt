package com.david.tehilim.core.service

import com.david.tehilim.core.model.Psalm
import com.david.tehilim.core.model.TranslationLanguage
import com.david.tehilim.core.model.Verse

/** Occurrence d'un terme dans le texte d'un verset (V2.3 — recherche plein-texte). */
data class VerseTextMatch(
    val psalm: Psalm,
    val verse: Verse,
    val snippet: String
)

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
    val interpretation: String?,
    /** V2.3 — occurrences du terme dans le texte (hébreu + traduction). */
    val textMatches: List<VerseTextMatch> = emptyList()
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
        private const val MAX_TEXT_MATCHES = 60
    }

    fun interpret(
        rawQuery: String,
        translationLanguage: TranslationLanguage = TranslationLanguage.EN
    ): SearchQueryResult {
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

        // 4. Recherche plein-texte (V2.3) — dès 2 caractères.
        val textMatches = if (trimmed.length >= 2) searchText(rawQuery, translationLanguage) else emptyList()
        return SearchQueryResult(
            exactMatch = null,
            suggestions = if (textMatches.isEmpty()) defaultSuggestions() else emptyList(),
            interpretation = null,
            textMatches = textMatches
        )
    }

    private fun searchText(rawQuery: String, lang: TranslationLanguage): List<VerseTextMatch> {
        val hebQuery = stripHebrewDiacritics(rawQuery).trim()
        val latinQuery = foldLatin(rawQuery).trim()
        val hasHebrew = hebQuery.any { it.code in 0x05D0..0x05EA }
        val out = ArrayList<VerseTextMatch>()
        for (psalm in repository.allPsalms) {
            for (verse in psalm.verses) {
                var snippet: String? = null
                if (hasHebrew && hebQuery.isNotEmpty()) {
                    if (stripHebrewDiacritics(verse.hebrew).contains(hebQuery)) snippet = verse.hebrew
                }
                if (snippet == null && !hasHebrew && latinQuery.length >= 2) {
                    verse.translation(lang)?.let { t ->
                        if (foldLatin(t).contains(latinQuery)) snippet = t
                    }
                }
                if (snippet != null) {
                    out.add(VerseTextMatch(psalm, verse, trimSnippet(snippet!!)))
                    if (out.size >= MAX_TEXT_MATCHES) return out
                }
            }
        }
        return out
    }

    /** Retire nikud + téamim (bloc U+0591–U+05CF) pour une recherche tolérante. */
    private fun stripHebrewDiacritics(s: String): String =
        s.filter { it.code !in 0x0591..0x05CF }

    /** Minuscules + suppression des accents (recherche latine tolérante). */
    private fun foldLatin(s: String): String =
        java.text.Normalizer.normalize(s.lowercase(), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")

    private fun trimSnippet(s: String, limit: Int = 90): String =
        if (s.length <= limit) s else s.take(limit) + "…"

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
