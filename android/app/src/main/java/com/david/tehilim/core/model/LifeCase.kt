package com.david.tehilim.core.model

import kotlinx.serialization.Serializable

/**
 * Un « Cas de la vie » — catégorie thématique de lecture des Tehilim.
 * Ex. « Santé », « Voyage », « Parnassa »... Chaque cas a une liste de Tehilim
 * traditionnellement recommandés + un conseil.
 */
@Serializable
data class LifeCase(
    val id: String,
    val title: String,
    val titleEN: String? = null,
    val symbol: String,
    val note: String,
    val noteEN: String? = null,
    val section: String? = null,
    val sectionEN: String? = null,
    val psalms: List<Int>
) {
    /** Préfère la version EN si la langue active est EN. */
    fun localizedTitle(language: TranslationLanguage): String = when (language) {
        TranslationLanguage.EN -> titleEN ?: title
        TranslationLanguage.FR -> title
    }

    fun localizedNote(language: TranslationLanguage): String = when (language) {
        TranslationLanguage.EN -> noteEN ?: note
        TranslationLanguage.FR -> note
    }

    fun localizedSection(language: TranslationLanguage): String? = when (language) {
        TranslationLanguage.EN -> sectionEN ?: section
        TranslationLanguage.FR -> section
    }
}
