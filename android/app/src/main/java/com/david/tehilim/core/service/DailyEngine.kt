package com.david.tehilim.core.service

import com.david.tehilim.core.model.DailyMode
import com.david.tehilim.core.model.DailyRules
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Calcule les Tehilim du jour selon le mode (mensuel ou hebdomadaire).
 *
 * Mirror du DailyEngine iOS. Les règles viennent de `daily_reading_rules.json`,
 * partagé avec iOS.
 */
class DailyEngine(private val rules: DailyRules) {

    fun psalmsForToday(mode: DailyMode): List<Int> {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return when (mode) {
            DailyMode.MONTHLY -> {
                val day = today.dayOfMonth
                rules.monthly[day.toString()] ?: emptyList()
            }
            DailyMode.WEEKLY -> {
                val key = today.dayOfWeek.toEnglishKey()
                rules.weekly[key] ?: emptyList()
            }
        }
    }

    /** Mapping DayOfWeek vers la clé canonique utilisée dans daily_reading_rules.json. */
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
