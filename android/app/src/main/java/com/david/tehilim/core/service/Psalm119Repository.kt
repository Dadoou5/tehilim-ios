package com.david.tehilim.core.service

import com.david.tehilim.core.model.Psalm119Section

class Psalm119Repository(sections: List<Psalm119Section>) {

    val sections: List<Psalm119Section> = sections.sortedBy { it.index }

    fun sectionAt(index: Int): Psalm119Section? =
        sections.firstOrNull { it.index == index }

    /** Retrouve la section par lettre de base (« א », « ב », etc.). */
    fun sectionByLetter(letter: String): Psalm119Section? =
        sections.firstOrNull { it.letter == letter }
}
