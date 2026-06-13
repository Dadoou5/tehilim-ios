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
    val titleHE: String? = null,
    val symbol: String,
    val note: String,
    val noteEN: String? = null,
    val noteHE: String? = null,
    val section: String? = null,
    val sectionEN: String? = null,
    val sectionHE: String? = null,
    val psalms: List<Int>
) {
    /** Localise avec repli HE → EN → FR. */
    fun localizedTitle(language: ContentLanguage): String = when (language) {
        ContentLanguage.HE -> titleHE ?: titleEN ?: title
        ContentLanguage.EN -> titleEN ?: title
        ContentLanguage.FR -> title
    }

    fun localizedNote(language: ContentLanguage): String = when (language) {
        ContentLanguage.HE -> noteHE ?: noteEN ?: note
        ContentLanguage.EN -> noteEN ?: note
        ContentLanguage.FR -> note
    }

    fun localizedSection(language: ContentLanguage): String? = when (language) {
        ContentLanguage.HE -> sectionHE ?: sectionEN ?: section
        ContentLanguage.EN -> sectionEN ?: section
        ContentLanguage.FR -> section
    }
}
