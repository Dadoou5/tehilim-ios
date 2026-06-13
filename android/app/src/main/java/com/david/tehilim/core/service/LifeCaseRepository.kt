package com.david.tehilim.core.service

import com.david.tehilim.core.model.ContentLanguage
import com.david.tehilim.core.model.LifeCase

class LifeCaseRepository(val cases: List<LifeCase>) {

    fun find(id: String): LifeCase? = cases.firstOrNull { it.id == id }

    /** Regroupement des cas par section (ordre canonique). Mirror iOS. */
    data class Group(
        val title: String,
        val titleEN: String,
        val titleHE: String,
        val cases: List<LifeCase>
    ) {
        fun localizedTitle(language: ContentLanguage) = when (language) {
            ContentLanguage.HE -> titleHE
            ContentLanguage.EN -> titleEN
            ContentLanguage.FR -> title
        }
    }

    fun grouped(language: ContentLanguage = ContentLanguage.FR): List<Group> {
        val order = listOf(
            Triple("Cycle de vie", "Life cycle", "מעגל החיים"),
            Triple("Santé et épreuves", "Health and trials", "בריאות וקשיים"),
            Triple("Spiritualité", "Spirituality", "רוחניות"),
            Triple("Communauté et calendrier", "Community and calendar", "קהילה ולוח השנה"),
            Triple("Autres", "Other", "אחר")
        )
        val byName: MutableMap<String, MutableList<LifeCase>> = mutableMapOf()
        for (c in cases) {
            val key = c.section ?: "Autres"
            byName.getOrPut(key) { mutableListOf() }.add(c)
        }
        return order.mapNotNull { (fr, en, he) ->
            val list = byName[fr] ?: return@mapNotNull null
            if (list.isEmpty()) null else Group(fr, en, he, list)
        }
    }
}
