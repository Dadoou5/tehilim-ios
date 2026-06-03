package com.david.tehilim.core.model

/**
 * Modèles de la feature « Chaîne de Tehilim » (lecture collective temps réel).
 * Mirror Android de TehilimChain.swift.
 *
 * Sans dépendance backend : simples data classes. Le mapping vers Supabase
 * (Postgres timestamptz ↔ epochMillis, colonnes snake_case) est dans ChainService.
 */

/** Type d'intention d'une chaîne (valeur « wire » identique à iOS rawValue). */
enum class ChainIntention(val wire: String) {
    LELOUY("lelouy"),       // Lelouy Nichmat
    REFOUA("refoua"),       // Refoua Chelema
    REUSSITE("reussite");   // Réussite / protection

    companion object {
        fun fromWire(s: String?): ChainIntention? = entries.firstOrNull { it.wire == s }
    }
}

/** Phase courante, dérivée du temps + flag distributed. */
enum class ChainPhase { SELECTING, LOCKED }

/** Les 5 livres des Tehilim — repères de section dans la grille. */
enum class TehilimBook(val range: IntRange) {
    ONE(1..41), TWO(42..72), THREE(73..89), FOUR(90..106), FIVE(107..150);

    companion object {
        fun bookFor(psalm: Int): TehilimBook = entries.first { psalm in it.range }
    }
}

/** Une chaîne (document `chains/{id}`). Dates en epochMillis. */
data class TehilimChain(
    val id: String,
    val name: String,
    val intentionType: ChainIntention,
    val intentionDetail: String,
    val creatorUid: String,
    val creatorName: String,
    val createdAtMillis: Long,
    val selectionDeadlineMillis: Long,
    val readingDeadlineMillis: Long,
    val distributed: Boolean,
    val expiresAtMillis: Long
) {
    val subjectLine: String
        get() {
            val d = intentionDetail.trim()
            return if (d.isEmpty()) name else "$name — $d"
        }

    fun phase(nowMillis: Long = System.currentTimeMillis()): ChainPhase =
        when {
            distributed -> ChainPhase.LOCKED
            nowMillis < selectionDeadlineMillis -> ChainPhase.SELECTING
            else -> ChainPhase.LOCKED
        }

    fun isSelectionOpen(nowMillis: Long = System.currentTimeMillis()): Boolean =
        phase(nowMillis) == ChainPhase.SELECTING

    companion object {
        /** Le livre entier. */
        const val TOTAL_PSALMS = 150

        /** Compresse une liste de numéros en plages pour le compte rendu :
         *  [1,2,3,5,8,9] → « 1 à 3, 5, 8 à 9 ». `sep` = « à »/« to ». */
        fun compressRanges(ids: List<Int>, sep: String): String {
            val s = ids.sorted()
            if (s.isEmpty()) return ""
            val parts = mutableListOf<String>()
            var start = s.first(); var prev = s.first()
            fun flush() { parts.add(if (start == prev) "$start" else "$start $sep $prev") }
            for (n in s.drop(1)) {
                if (n == prev + 1) prev = n else { flush(); start = n; prev = n }
            }
            flush()
            return parts.joinToString(", ")
        }
    }
}

/** Un participant (document `chains/{id}/participants/{uid}`). */
data class ChainParticipant(
    val uid: String,
    val name: String,
    val isCreator: Boolean,
    val joinedAtMillis: Long
)

/** L'attribution d'un Tehilim (document `chains/{id}/assignments/{psalmId}`). */
data class ChainAssignment(
    val psalmId: Int,        // 1..150
    val uid: String,
    val name: String,        // dénormalisé pour affichage
    val byCreator: Boolean,
    val assignedAtMillis: Long
)
