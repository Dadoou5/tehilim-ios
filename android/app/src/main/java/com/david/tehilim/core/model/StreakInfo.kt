package com.david.tehilim.core.model

import java.time.LocalDate

/**
 * Série de lecture (V2.3) — dérivée de la liste des jours de lecture.
 * Mirror de ReadingStreakStore.swift.
 */
data class StreakInfo(
    val current: Int,   // jours consécutifs se terminant aujourd'hui (ou hier, tolérance)
    val best: Int,      // meilleure série historique
    val total: Int      // nombre total de jours de lecture
) {
    companion object {
        val EMPTY = StreakInfo(0, 0, 0)

        /** [raw] = jours « yyyy-MM-dd » séparés par virgule. */
        fun from(raw: String): StreakInfo {
            val days = raw.split(",").mapNotNull {
                runCatching { LocalDate.parse(it.trim()) }.getOrNull()
            }.toSortedSet()
            if (days.isEmpty()) return EMPTY

            val today = LocalDate.now()
            val yesterday = today.minusDays(1)

            // Série courante : part d'aujourd'hui si lu, sinon d'hier (tolérance).
            var cursor: LocalDate? = when {
                today in days -> today
                yesterday in days -> yesterday
                else -> null
            }
            var current = 0
            while (cursor != null && cursor in days) {
                current++
                cursor = cursor.minusDays(1)
            }

            // Meilleure série : plus longue suite de dates consécutives.
            var best = 0
            var run = 0
            var prev: LocalDate? = null
            for (d in days) {
                run = if (prev != null && prev.plusDays(1) == d) run + 1 else 1
                if (run > best) best = run
                prev = d
            }

            return StreakInfo(current = current, best = maxOf(best, current), total = days.size)
        }
    }
}
