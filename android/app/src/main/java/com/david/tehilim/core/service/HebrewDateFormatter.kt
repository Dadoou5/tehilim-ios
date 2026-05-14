package com.david.tehilim.core.service

import android.icu.text.DateFormat
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.icu.util.HebrewCalendar
import android.icu.util.ULocale
import java.util.Date
import java.util.Locale

/**
 * Mirror du HebrewDateFormatter iOS.
 *
 * Utilise **android.icu** (Unicode CLDR) qui supporte officiellement le
 * calendrier hébraïque et son formatage en hébreu (כ״ז באייר ה׳תשפ״ו)
 * et en translittération latine (27 Iyar 5786).
 *
 * Disponible depuis API 24, donc compatible avec notre min SDK 26.
 */
object HebrewDateFormatter {

    data class DisplayDate(
        val dayOfWeek: String,         // "Jeudi"
        val transliterated: String,    // "27 Iyar 5786"
        val hebrew: String             // "כ״ז באייר ה׳תשפ״ו"
    )

    fun formatted(date: Date = Date()): DisplayDate {
        // Jour de la semaine dans la locale FR (« lundi », « mardi »...)
        val dowFmt = SimpleDateFormat("EEEE", Locale.FRENCH)
        val day = dowFmt.format(date).replaceFirstChar { it.titlecase(Locale.FRENCH) }

        // Date hébraïque translittérée (anglais — « 27 Iyar 5786 »)
        val latinCal = HebrewCalendar()
        latinCal.time = date
        val latinFmt = DateFormat.getDateInstance(DateFormat.LONG, ULocale("en_US@calendar=hebrew"))
        latinFmt.calendar = latinCal
        val latin = latinFmt.format(date)

        // Date hébraïque en hébreu (« כ״ז באייר ה׳תשפ״ו »)
        val heCal = HebrewCalendar()
        heCal.time = date
        val heFmt = DateFormat.getDateInstance(DateFormat.LONG, ULocale("he_IL@calendar=hebrew"))
        heFmt.calendar = heCal
        val hebrew = heFmt.format(date)

        return DisplayDate(dayOfWeek = day, transliterated = latin, hebrew = hebrew)
    }

    /**
     * Pour le widget : version compacte « 27 Iyar » sans année.
     */
    fun compact(date: Date = Date()): String {
        val cal = HebrewCalendar()
        cal.time = date
        val fmt = SimpleDateFormat("d MMMM", Locale("en"))
        fmt.calendar = cal
        return fmt.format(date)
    }
}
