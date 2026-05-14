package com.david.tehilim.core.model

/**
 * Prière avant ou après la lecture des Tehilim — mirror du modèle iOS.
 * Construit à partir de versets référencés par psaume + numéro.
 */
data class Prayer(
    val kind: Kind,
    val verseRefs: List<VerseRef>
) {
    data class VerseRef(val psalmId: Int, val verseNumber: Int)

    enum class Kind(val titleFR: String, val subtitleFR: String, val symbol: String) {
        BEFORE("Prière avant la lecture", "À dire avant de commencer", "PlayCircle"),
        AFTER("Prière après la lecture", "À dire à la fin", "CheckCircle");
    }

    companion object {
        // V1 : références minimales (équivalent du fichier iOS Prayer.swift).
        // À étoffer avec le contenu liturgique complet.
        fun of(kind: Kind): Prayer = when (kind) {
            Kind.BEFORE -> Prayer(
                kind = Kind.BEFORE,
                verseRefs = (1..3).map { VerseRef(psalmId = 95, verseNumber = it) }
            )
            Kind.AFTER -> Prayer(
                kind = Kind.AFTER,
                verseRefs = listOf(
                    VerseRef(psalmId = 14, verseNumber = 7),
                    VerseRef(psalmId = 37, verseNumber = 39),
                    VerseRef(psalmId = 37, verseNumber = 40)
                )
            )
        }
    }
}
