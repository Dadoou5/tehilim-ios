package com.david.tehilim.core.model

import androidx.annotation.StringRes
import com.david.tehilim.R

/**
 * Prière avant ou après la lecture des Tehilim — mirror du modèle iOS.
 * Construit à partir de versets référencés par psaume + numéro.
 *
 * V1.3.0 — Les labels sont des @StringRes, résolus dans la locale active de l'app.
 */
data class Prayer(
    val kind: Kind,
    val verseRefs: List<VerseRef>
) {
    data class VerseRef(val psalmId: Int, val verseNumber: Int)

    enum class Kind(
        @StringRes val titleRes: Int,
        @StringRes val subtitleRes: Int,
        val symbol: String
    ) {
        BEFORE(R.string.prayer_before_title, R.string.prayer_before_subtitle, "PlayCircle"),
        AFTER(R.string.prayer_after_title, R.string.prayer_after_subtitle, "CheckCircle");
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
