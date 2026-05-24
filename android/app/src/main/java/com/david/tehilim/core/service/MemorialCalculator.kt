package com.david.tehilim.core.service

import android.icu.util.Calendar as ICUCalendar
import android.icu.util.HebrewCalendar
import com.david.tehilim.core.model.HebrewYMD
import java.util.Date

/**
 * Calcule la prochaine azcara (anniversaire hébraïque du décès) à partir
 * d'une date civile, en appliquant les règles traditionnelles. Mirror
 * exact de `MemorialCalculator.swift` (iOS) — même algorithme.
 *
 * Indexation mois ICU (différente d'Apple Hebrew !) :
 * - 0 = TISHRI
 * - 1 = HESHVAN
 * - 2 = KISLEV
 * - 3 = TEVET
 * - 4 = SHEVAT
 * - 5 = ADAR_1  (n'existe QUE dans les années embolismiques)
 * - 6 = ADAR    (= Adar unique en année commune, = Adar II en embolismique)
 * - 7 = NISAN
 * - ...
 * - 12 = ELUL
 *
 * Règles métier :
 * - Adar (année commune) → Adar II en année cible embolismique
 * - Adar I leap → Adar unique en année cible commune
 * - Adar II leap → Adar unique en année cible commune
 * - 30 Heshvan / 30 Kislev avec mois cible 29 jours → 1er du mois suivant
 * - 30 Adar I leap → 30 Shevat en année cible commune
 *
 * V1.4 — feature Commémoration.
 */
object MemorialCalculator {

    /** API publique : civile → civile. */
    fun nextYahrzeit(deathCivil: Date, now: Date = Date()): Date? {
        val death = hebrewYMD(deathCivil)
        val today = hebrewYMD(now)
        val sourceLeap = isLeap(death.year)

        // Borne de sécurité : 1 itération en pratique, max 2 si déjà passé.
        for (offset in 0..2) {
            val targetYear = today.year + offset
            val targetLeap = isLeap(targetYear)
            val tm = adjustedMonth(death.month, sourceLeap, targetLeap)
            val (fm, fd) = adjustedDay(
                sourceDay = death.day,
                sourceMonth = death.month,
                sourceLeap = sourceLeap,
                targetMonth = tm,
                targetYear = targetYear
            )
            val candidate = civilDate(targetYear, fm, fd) ?: continue
            if (candidate.after(now)) return candidate
        }
        return null
    }

    /** Convertit une date civile en composantes hébraïques (ICU-indexées). */
    fun hebrewYMD(date: Date): HebrewYMD {
        val cal = HebrewCalendar()
        cal.time = date
        return HebrewYMD(
            year = cal.get(ICUCalendar.YEAR),
            month = cal.get(ICUCalendar.MONTH),
            day = cal.get(ICUCalendar.DAY_OF_MONTH)
        )
    }

    /** Convertit une date hébraïque en date civile (midi local). */
    fun civilDate(year: Int, month: Int, day: Int): Date? {
        return try {
            val cal = HebrewCalendar()
            cal.clear()
            cal.set(ICUCalendar.YEAR, year)
            cal.set(ICUCalendar.MONTH, month)
            cal.set(ICUCalendar.DAY_OF_MONTH, day)
            cal.set(ICUCalendar.HOUR_OF_DAY, 12)
            cal.set(ICUCalendar.MINUTE, 0)
            cal.set(ICUCalendar.SECOND, 0)
            cal.time
        } catch (e: Throwable) {
            null
        }
    }

    /** Cycle de Méton : 7 années embolismiques sur 19. */
    fun isLeap(year: Int): Boolean = ((7 * year + 1) % 19) < 7

    /** Nombre de jours dans `month` pour `year` (ICU-aware). */
    fun daysInMonth(month: Int, year: Int): Int {
        val cal = HebrewCalendar()
        cal.clear()
        cal.set(ICUCalendar.YEAR, year)
        cal.set(ICUCalendar.MONTH, month)
        cal.set(ICUCalendar.DAY_OF_MONTH, 1)
        return cal.getActualMaximum(ICUCalendar.DAY_OF_MONTH)
    }

    // Règles métier (exposées pour les tests)

    /**
     * Adaptation du mois entre année source et année cible. Indexation ICU.
     *
     * - sourceMonth == 5 (ADAR_1) : ne peut arriver qu'en source leap.
     *   Cible leap → ADAR_1 (5). Cible non-leap → ADAR (6, unique).
     * - sourceMonth == 6 (ADAR) : sémantique dépend de sourceLeap.
     *   - sourceLeap = true → c'est Adar II. Cible leap → ADAR (6) = Adar II.
     *     Cible non-leap → ADAR (6) = Adar unique.
     *   - sourceLeap = false → c'est Adar unique. Cible leap → ADAR (6) = Adar II
     *     (tradition Ashkenazi). Cible non-leap → ADAR (6) = inchangé.
     * - autres mois : inchangés (ICU n'a pas de décalage d'indexation, contraire-
     *   ment à Apple Hebrew).
     */
    fun adjustedMonth(sourceMonth: Int, sourceLeap: Boolean, targetLeap: Boolean): Int {
        return when {
            sourceMonth == 5 && sourceLeap -> if (targetLeap) 5 else 6
            // Tous les autres cas Adar (et tous les autres mois) → 6 ou inchangé.
            // ICU est "stable" : ADAR (6) reste 6 que l'année soit leap ou non.
            // Donc rien à transformer pour les mois post-Adar (7..12).
            else -> sourceMonth
        }
    }

    /** Adaptation du jour quand le mois cible est plus court (29 vs 30). */
    fun adjustedDay(
        sourceDay: Int,
        sourceMonth: Int,
        sourceLeap: Boolean,
        targetMonth: Int,
        targetYear: Int
    ): Pair<Int, Int> {
        val daysAvail = daysInMonth(targetMonth, targetYear)
        if (sourceDay <= daysAvail) return targetMonth to sourceDay

        // sourceDay == 30 mais targetMonth a 29 jours.
        // Cas spécial : 30 Adar I leap (ICU mois 5) + cible non-leap →
        // 30 Shevat (ICU mois 4) — tradition.
        if (sourceLeap && sourceMonth == 5 && sourceDay == 30) {
            return 4 to 30
        }
        // 30 Heshvan (1) → 1 Kislev (2). 30 Kislev (2) → 1 Tevet (3).
        return (targetMonth + 1) to 1
    }
}
