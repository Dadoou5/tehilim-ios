package com.david.tehilim.core.service

import android.icu.text.DateFormat
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.icu.util.HebrewCalendar
import android.icu.util.ULocale
import androidx.appcompat.app.AppCompatDelegate
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
        val dayOfWeek: String,         // « Jeudi » ou « Thursday »
        val transliterated: String,    // « 27 Iyar 5786 »
        val hebrew: String             // « כ״ז באייר ה׳תשפ״ו »
    )

    /**
     * Résout la locale courante de l'app (FR/EN) à partir d'AppCompatDelegate.
     * V1.3.8 — fixe le bug « Mercredi » en anglais : avant, on hardcodait
     * `Locale.FRENCH` au lieu de lire la locale active.
     */
    private fun currentLocale(): Locale {
        // Sur API 33+ l'app pilote la langue via LocaleManager, donc
        // getApplicationLocales() peut être vide → on retombe sur Locale.getDefault()
        // (qui reflète bien la langue de l'app). On normalise ensuite.
        val locales = AppCompatDelegate.getApplicationLocales()
        val base = if (!locales.isEmpty) (locales[0] ?: Locale.getDefault()) else Locale.getDefault()
        // `Locale.getLanguage()` rapporte l'hébreu sous l'ancien code « iw ».
        return when (base.language) {
            "en" -> Locale.ENGLISH
            "fr" -> Locale.FRENCH
            "he", "iw" -> Locale("he")
            else -> Locale.FRENCH
        }
    }

    fun formatted(date: Date = Date()): DisplayDate {
        val locale = currentLocale()
        // Jour de la semaine dans la locale courante (« lundi » ou « Monday »)
        val dowFmt = SimpleDateFormat("EEEE", locale)
        val day = dowFmt.format(date).replaceFirstChar { it.titlecase(locale) }

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
        val fmt = SimpleDateFormat("d MMMM", Locale.ENGLISH)
        fmt.calendar = cal
        return fmt.format(date)
    }
}
