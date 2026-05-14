package com.david.tehilim.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Un Tehilim — psaume du roi David. Mirror du `Psalm.swift` iOS.
 *
 * Le JSON contient 150 entries, indexées 1..150. Chaque psaume porte sa
 * structure de versets + références bibliques + métadonnées.
 */
@Serializable
data class Psalm(
    val id: Int,
    val hebrewNumber: String,
    val hebrewTitle: String? = null,
    val verses: List<Verse> = emptyList()
) {
    companion object {
        /// Mêmes ranges que iOS — répartition canonique des 150 Tehilim en 5 livres.
        val bookRanges: Map<Int, IntRange> = mapOf(
            1 to 1..41,
            2 to 42..72,
            3 to 73..89,
            4 to 90..106,
            5 to 107..150
        )

        fun book(forId id: Int): Int? =
            bookRanges.entries.firstOrNull { it.value.contains(id) }?.key
    }
}

/**
 * Un verset au sein d'un psaume.
 *
 * `translationFR` et `translationEN` sont optionnels — Sefaria peut manquer la traduction
 * EN pour certains psaumes, et Beth Loubavitch est exhaustive pour le FR.
 */
@Serializable
data class Verse(
    val number: Int,
    val hebrew: String,
    val hebrewNumber: String,
    @SerialName("translationFR") val translationFR: String? = null,
    @SerialName("translationEN") val translationEN: String? = null
) {
    val id: String get() = "verse-$number"

    fun translation(language: TranslationLanguage): String? = when (language) {
        TranslationLanguage.FR -> translationFR
        TranslationLanguage.EN -> translationEN
    }
}

/**
 * Langue de la traduction affichée à côté de l'hébreu.
 * Pilotée par les Préférences (AppLanguage).
 */
enum class TranslationLanguage(val code: String, val sourceCredit: String) {
    FR("fr", "Beth Loubavitch — le-tehilim.online"),
    EN("en", "JPS 1917 via Sefaria.org");
}
