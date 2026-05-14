package com.david.tehilim.core.model

import kotlinx.serialization.Serializable

/**
 * Règles de répartition des 150 Tehilim sur les jours de la semaine ou du mois.
 * Mirror du modèle iOS DailyRules.
 */
@Serializable
data class DailyRules(
    val weekly: Map<String, List<Int>> = emptyMap(),
    val monthly: Map<String, List<Int>> = emptyMap()
) {
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
