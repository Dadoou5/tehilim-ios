package com.david.tehilim.core.service

import android.icu.util.Calendar
import android.icu.util.HebrewCalendar
import com.david.tehilim.core.model.DailyMode
import com.david.tehilim.core.model.DailyRules
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.Date

/**
 * Calcule les Tehilim du jour selon le mode (mensuel ou hebdomadaire).
 * Mirror du DailyEngine iOS.
 *
 * **MONTHLY** utilise le calendrier **hébraïque** (mirror iOS qui utilise
 * `Calendar(identifier: .hebrew)`) — le cycle de lecture mensuel est calé
 * sur les mois hébreux (29 ou 30 jours), pas grégoriens. Avant V1.4, on
 * lisait `today.dayOfMonth` grégorien, ce qui décalait toute la table
 * d'environ 2-3 semaines selon la période.
 *
 * **WEEKLY** reste sur le calendrier grégorien — c'est le jour de la
 * semaine (dimanche → samedi) qui détermine les Tehilim.
 */
class DailyEngine(private val rules: DailyRules) {

    fun psalmsForToday(mode: DailyMode): List<Int> {
        return when (mode) {
            DailyMode.MONTHLY -> {
                val day = hebrewDayOfMonth(Date())
                // Combine jour 30 sur 29 pour les mois de 29 jours (mirror règle JSON).
                rules.monthly(day).ifEmpty { rules.monthly(day - 1) }
            }
            DailyMode.WEEKLY -> {
                val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                rules.weekly(today.dayOfWeek.toEnglishKey())
            }
        }
    }

    /** Jour du mois dans le calendrier hébraïque (1..30). */
    private fun hebrewDayOfMonth(date: Date): Int {
        val cal = HebrewCalendar()
        cal.time = date
        return cal.get(Calendar.DAY_OF_MONTH)
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
