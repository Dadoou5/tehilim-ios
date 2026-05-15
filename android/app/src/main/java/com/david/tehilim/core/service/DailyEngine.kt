package com.david.tehilim.core.service

import com.david.tehilim.core.model.DailyMode
import com.david.tehilim.core.model.DailyRules
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Calcule les Tehilim du jour selon le mode (mensuel ou hebdomadaire).
 * Mirror du DailyEngine iOS.
 */
class DailyEngine(private val rules: DailyRules) {

    fun psalmsForToday(mode: DailyMode): List<Int> {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return when (mode) {
            DailyMode.MONTHLY -> {
                val day = today.dayOfMonth
                // Combine jour 30 sur 29 pour les mois de 29 jours (mirror règle JSON).
                rules.monthly(day).ifEmpty { rules.monthly(day - 1) }
            }
            DailyMode.WEEKLY -> rules.weekly(today.dayOfWeek.toEnglishKey())
        }
    }

    private fun DayOfWeek.toEnglishKey(): String = when (this) {
        DayOfWeek.SUNDAY    -> "sunday"
        DayOfWeek.MONDAY    -> "monday"
        DayOfWeek.TUESDAY   -> "tuesday"
        DayOfWeek.WEDNESDAY -> "wednesday"
        DayOfWeek.THURSDAY  -> "thursday"
        DayOfWeek.FRIDAY    -> "friday"
        DayOfWeek.SATURDAY  -> "saturday"
    }
}
