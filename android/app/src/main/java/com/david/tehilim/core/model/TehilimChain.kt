package com.david.tehilim.core.model

/**
 * Modèles de la feature « Chaîne de Tehilim » (lecture collective temps réel).
 * Mirror Android de TehilimChain.swift.
 *
 * Sans dépendance Firebase : simples data classes. La (dé)sérialisation
 * Firestore (Timestamp ↔ epochMillis) est faite dans ChainService.
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
