package com.david.tehilim.core.service

import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.TimeZone
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/** Coordonnée géographique (degrés décimaux). */
data class GeoCoordinate(val latitude: Double, val longitude: Double)

/** Ville pré-enregistrée pour le repli « pas de GPS ». */
data class ShabbatCity(val id: String, val nameFR: String, val coordinate: GeoCoordinate)

/** État Chabbat à un instant donné (mirror iOS). */
data class ShabbatState(
    val isShabbat: Boolean,
    val endsAt: Date?,
    val nextStartsAt: Date?,
    /** Début du Chabbat (allumage des bougies) — renseigné en Chabbat ET en
     *  pré-Chabbat (pour afficher l'horaire d'entrée à l'avance). */
    val startedAt: Date? = null,
    /** Pré-Chabbat : dans l'heure qui précède l'entrée (écran informatif). */
    val isPreShabbat: Boolean = false
) {
    /** True si l'écran « Chabbat Chalom » doit s'afficher. */
    val shouldDisplay: Boolean get() = isShabbat || isPreShabbat
}

/**
 * Mirror Android du ShabbatCalculator.swift.
 *
 * Calcul du coucher du soleil (algorithme NOAA / Almanac) et de la fenêtre de
 * Chabbat. Pur (aucune dépendance Android) → partageable app ↔ widget Glance.
 *
 * - Début : vendredi, coucher du soleil − 18 min (allumage des bougies).
 * - Fin   : samedi, sortie des étoiles (Tzeit) = soleil à 8,5° sous l'horizon
 *   (défaut Hebcal « 3 étoiles moyennes »), calculée selon la position.
 */
object ShabbatCalculator {

    const val CANDLE_LIGHTING_OFFSET_MIN = 18.0
    /** Havdala = sortie des étoiles : angle de dépression solaire standard
     *  (8,5°, défaut Hebcal). Dépend de la position et de la date. */
    const val HAVDALAH_DEPRESSION_DEG = 8.5
    /** Repli quand le soleil n'atteint pas 8,5° (hautes latitudes en été). */
    const val HAVDALAH_FALLBACK_OFFSET_MIN = 72.0
    /** L'écran s'affiche dès 1 h avant l'entrée (pré-Chabbat). */
    const val PRE_SHABBAT_LEAD_MS = 3_600_000L

    val cities: List<ShabbatCity> = listOf(
        ShabbatCity("jerusalem", "Jérusalem", GeoCoordinate(31.7683, 35.2137)),
        ShabbatCity("telaviv", "Tel Aviv", GeoCoordinate(32.0853, 34.7818)),
        ShabbatCity("paris", "Paris", GeoCoordinate(48.8566, 2.3522)),
        ShabbatCity("marseille", "Marseille", GeoCoordinate(43.2965, 5.3698)),
        ShabbatCity("lyon", "Lyon", GeoCoordinate(45.7640, 4.8357)),
        ShabbatCity("nice", "Nice", GeoCoordinate(43.7102, 7.2620)),
        ShabbatCity("strasbourg", "Strasbourg", GeoCoordinate(48.5734, 7.7521)),
        ShabbatCity("toulouse", "Toulouse", GeoCoordinate(43.6047, 1.4442)),
        ShabbatCity("london", "Londres", GeoCoordinate(51.5074, -0.1278)),
        ShabbatCity("brussels", "Bruxelles", GeoCoordinate(50.8503, 4.3517)),
        ShabbatCity("geneva", "Genève", GeoCoordinate(46.2044, 6.1432)),
        ShabbatCity("montreal", "Montréal", GeoCoordinate(45.5019, -73.5674)),
        ShabbatCity("newyork", "New York", GeoCoordinate(40.7128, -74.0060)),
        ShabbatCity("losangeles", "Los Angeles", GeoCoordinate(34.0522, -118.2437))
    )

    fun city(id: String): ShabbatCity? = cities.firstOrNull { it.id == id }

    // MARK: - État Chabbat

    fun state(
        now: Date,
        coordinate: GeoCoordinate,
        timeZone: TimeZone = TimeZone.getDefault()
    ): ShabbatState {
        val cal = GregorianCalendar(timeZone)
        cal.time = now
        val weekday = cal.get(Calendar.DAY_OF_WEEK)

        if (weekday == Calendar.FRIDAY) {
            val friday = startOfDay(now, timeZone)
            val saturday = addDays(friday, 1, timeZone)
            val candle = candleLighting(friday, coordinate, timeZone)
            val havdalah = havdalah(saturday, coordinate, timeZone)
            if (candle != null && havdalah != null) {
                if (!now.before(candle)) return ShabbatState(true, havdalah, null, startedAt = candle)
                val preStart = Date(candle.time - PRE_SHABBAT_LEAD_MS)
                if (!now.before(preStart)) {
                    return ShabbatState(false, havdalah, null, startedAt = candle, isPreShabbat = true)
                }
                return ShabbatState(false, null, preStart)
            }
        } else if (weekday == Calendar.SATURDAY) {
            val saturday = startOfDay(now, timeZone)
            val friday = addDays(saturday, -1, timeZone)
            val candle = candleLighting(friday, coordinate, timeZone)
            val havdalah = havdalah(saturday, coordinate, timeZone)
            if (candle != null && havdalah != null && !now.after(havdalah)) {
                return ShabbatState(true, havdalah, null, startedAt = candle)
            }
        }
        val next = nextCandleLighting(now, coordinate, timeZone)
        return ShabbatState(false, null, next?.let { Date(it.time - PRE_SHABBAT_LEAD_MS) })
    }

    private fun nextCandleLighting(now: Date, coordinate: GeoCoordinate, tz: TimeZone): Date? {
        val today = startOfDay(now, tz)
        for (offset in 0..7) {
            val day = addDays(today, offset, tz)
            val cal = GregorianCalendar(tz).apply { time = day }
            if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
                val candle = candleLighting(day, coordinate, tz)
                if (candle != null && candle.after(now)) return candle
            }
        }
        return null
    }

    private fun candleLighting(friday: Date, coordinate: GeoCoordinate, tz: TimeZone): Date? {
        val s = sunset(friday, coordinate, tz) ?: return null
        return Date(s.time - (CANDLE_LIGHTING_OFFSET_MIN * 60_000).toLong())
    }

    private fun havdalah(saturday: Date, coordinate: GeoCoordinate, tz: TimeZone): Date? {
        // Sortie des étoiles : soleil à 8,5° sous l'horizon (zénith 98,5°).
        eveningEvent(saturday, coordinate, tz, 90.0 + HAVDALAH_DEPRESSION_DEG)?.let { return it }
        // Repli (le soleil ne descend pas à 8,5° près du solstice à haute
        // latitude) : coucher + 72 min.
        val s = sunset(saturday, coordinate, tz) ?: return null
        return Date(s.time + (HAVDALAH_FALLBACK_OFFSET_MIN * 60_000).toLong())
    }

    private fun startOfDay(date: Date, tz: TimeZone): Date {
        val cal = GregorianCalendar(tz).apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return cal.time
    }

    private fun addDays(date: Date, days: Int, tz: TimeZone): Date {
        val cal = GregorianCalendar(tz).apply { time = date; add(Calendar.DAY_OF_MONTH, days) }
        return cal.time
    }

    // MARK: - Coucher du soleil (NOAA / Almanac)

    fun sunset(day: Date, coordinate: GeoCoordinate, tz: TimeZone): Date? =
        eveningEvent(day, coordinate, tz, 90.833)

    /**
     * Événement solaire du soir pour un `zenith` donné (90,833 = coucher ;
     * > 90 = crépuscule / sortie des étoiles selon l'angle de dépression).
     */
    fun eveningEvent(day: Date, coordinate: GeoCoordinate, tz: TimeZone, zenith: Double): Date? {
        val cal = GregorianCalendar(tz).apply { time = day }
        return sunsetUTC(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1, // Calendar.MONTH is 0-based
            cal.get(Calendar.DAY_OF_MONTH),
            coordinate.latitude,
            coordinate.longitude,
            tz,
            zenith
        )
    }

    private fun deg2rad(d: Double) = d * Math.PI / 180.0
    private fun rad2deg(r: Double) = r * 180.0 / Math.PI
    private fun mod(a: Double, b: Double): Double { val r = a % b; return if (r < 0) r + b else r }

    private fun sunsetUTC(
        year: Int, month: Int, day: Int,
        latitude: Double, longitude: Double, timeZone: TimeZone, zenith: Double = 90.833
    ): Date? {
        val n1 = floor(275.0 * month / 9.0)
        val n2 = floor((month + 9.0) / 12.0)
        val n3 = 1.0 + floor((year - 4.0 * floor(year / 4.0) + 2.0) / 3.0)
        val n = n1 - (n2 * n3) + day - 30.0

        val lngHour = longitude / 15.0
        val t = n + ((18.0 - lngHour) / 24.0)
        val m = (0.9856 * t) - 3.289
        var l = m + (1.916 * sin(deg2rad(m))) + (0.020 * sin(deg2rad(2 * m))) + 282.634
        l = mod(l, 360.0)
        var ra = rad2deg(atan(0.91764 * tan(deg2rad(l))))
        ra = mod(ra, 360.0)
        val lQuadrant = floor(l / 90.0) * 90.0
        val raQuadrant = floor(ra / 90.0) * 90.0
        ra = (ra + (lQuadrant - raQuadrant)) / 15.0
        val sinDec = 0.39782 * sin(deg2rad(l))
        val cosDec = cos(asin(sinDec))
        // `zenith` passé en paramètre (90,833 = coucher, 98,5 = tzeit à 8,5°).
        val cosH = (cos(deg2rad(zenith)) - (sinDec * sin(deg2rad(latitude)))) /
            (cosDec * cos(deg2rad(latitude)))
        if (cosH > 1 || cosH < -1) return null
        val h = rad2deg(acos(cosH)) / 15.0
        val localT = h + ra - (0.06571 * t) - 6.622
        val ut = mod(localT - lngHour, 24.0)

        val hour = floor(ut).toInt()
        val minuteFull = (ut - hour) * 60.0
        val minute = floor(minuteFull).toInt()
        val second = ((minuteFull - minute) * 60.0).toInt()

        val utc = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month - 1, day, hour, minute, second)
        }
        val candidate = utc.time

        // Correction de bord : choisit le décalage (−1/0/+1 j) dont la date
        // LOCALE colle au jour visé (cas des longitudes traversant minuit UTC).
        val target = GregorianCalendar(timeZone)
        for (shift in intArrayOf(0, 1, -1)) {
            val c = Date(candidate.time + shift.toLong() * 86_400_000L)
            target.time = c
            if (target.get(Calendar.YEAR) == year &&
                target.get(Calendar.MONTH) + 1 == month &&
                target.get(Calendar.DAY_OF_MONTH) == day
            ) return c
        }
        return candidate
    }
}
