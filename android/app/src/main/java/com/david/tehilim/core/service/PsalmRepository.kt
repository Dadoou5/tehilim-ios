package com.david.tehilim.core.service

import com.david.tehilim.core.model.Psalm

class PsalmRepository(val allPsalms: List<Psalm>) {

    /** Cherche un Tehilim par son ID (1..150). */
    fun psalm(id: Int): Psalm? = allPsalms.firstOrNull { it.id == id }

    /** Liste des Tehilim du livre donné (1..5). */
    fun psalmsInBook(book: Int): List<Psalm> {
        val range = Psalm.bookRanges[book] ?: return emptyList()
        return allPsalms.filter { range.contains(it.id) }
    }

    /** Voisins (précédent / suivant) dans le corpus complet 1..150. */
    fun neighbors(id: Int): Pair<Int?, Int?> {
        val prev = if (id > 1) id - 1 else null
        val next = if (id < 150) id + 1 else null
        return prev to next
    }
}
