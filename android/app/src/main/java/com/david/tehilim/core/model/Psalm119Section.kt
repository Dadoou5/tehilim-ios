package com.david.tehilim.core.model

import kotlinx.serialization.Serializable

/**
 * Une section du Tehilim 119 (AlphaBeta) — chacune des 22 lettres
 * de l'alphabet hébreu correspond à 8 versets consécutifs.
 *
 * `index` 1..22, `letter` un caractère hébreu (« א » … « ת »).
 */
@Serializable
data class Psalm119Section(
    val index: Int,
    val letter: String,
    val name: String,
    val verseStart: Int,
    val verseEnd: Int
) {
    val versesRange: IntRange get() = verseStart..verseEnd
}
