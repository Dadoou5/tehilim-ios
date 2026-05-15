package com.david.tehilim.core.model

import kotlinx.serialization.Serializable

/**
 * Règles de répartition des 150 Tehilim sur les jours.
 * Structure réelle du JSON :
 *   modes.monthly.days.{1..30} = List<Int>
 *   modes.weekly.days.{sunday..saturday} = List<Int>
 */
@Serializable
data class DailyRules(
    val modes: Modes = Modes()
) {
    @Serializable
    data class Modes(
        val monthly: ModeBlock = ModeBlock(),
        val weekly: ModeBlock = ModeBlock(),
        val custom: ModeBlock = ModeBlock()
    )

    @Serializable
    data class ModeBlock(
        val description: String = "",
        val days: Map<String, List<Int>> = emptyMap()
    )

    /** Tehilim pour un jour donné, en mode mensuel (clé = jour numérique 1..30 en String). */
    fun monthly(day: Int): List<Int> = modes.monthly.days[day.toString()] ?: emptyList()

    /** Tehilim pour un jour donné, en mode hebdomadaire (clé = nom anglais minuscule). */
    fun weekly(dayKey: String): List<Int> = modes.weekly.days[dayKey] ?: emptyList()

    companion object {
        val empty = DailyRules()
    }
}

/**
 * Mode de cycle quotidien.
 */
enum class DailyMode(val label: String) {
    MONTHLY("Cycle mensuel"),
    WEEKLY("Jour de la semaine");
}
