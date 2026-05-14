package com.david.tehilim.core.service

import com.david.tehilim.core.model.LifeCase
import com.david.tehilim.core.model.TranslationLanguage

class LifeCaseRepository(val cases: List<LifeCase>) {

    fun find(id: String): LifeCase? = cases.firstOrNull { it.id == id }

    /** Regroupement des cas par section (ordre canonique). Mirror iOS. */
    data class Group(
        val title: String,
        val titleEN: String,
        val cases: List<LifeCase>
    ) {
        fun localizedTitle(language: TranslationLanguage) = when (language) {
            TranslationLanguage.EN -> titleEN
            TranslationLanguage.FR -> title
        }
    }

    fun grouped(language: TranslationLanguage = TranslationLanguage.FR): List<Group> {
        val order = listOf(
            "Cycle de vie" to "Life cycle",
            "Santé et épreuves" to "Health and trials",
            "Spiritualité" to "Spirituality",
            "Communauté et calendrier" to "Community and calendar",
            "Autres" to "Other"
        )
        val byName: MutableMap<String, MutableList<LifeCase>> = mutableMapOf()
        for (c in cases) {
            val key = c.section ?: "Autres"
            byName.getOrPut(key) { mutableListOf() }.add(c)
        }
        return order.mapNotNull { (fr, en) ->
            val list = byName[fr] ?: return@mapNotNull null
            if (list.isEmpty()) null else Group(fr, en, list)
        }
    }
}
