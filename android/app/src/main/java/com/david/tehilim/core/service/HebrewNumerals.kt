package com.david.tehilim.core.service

/**
 * Conversion entre entiers (1..400) et notation hébraïque par gematria.
 * Respecte les conventions standards : 15 = טו, 16 = טז (et non יה / יו).
 *
 * Mirror direct de `HebrewNumerals.swift` iOS.
 */
object HebrewNumerals {

    private val mapping: List<Pair<Int, String>> = listOf(
        400 to "ת", 300 to "ש", 200 to "ר", 100 to "ק",
        90 to "צ", 80 to "פ", 70 to "ע", 60 to "ס",
        50 to "נ", 40 to "מ", 30 to "ל", 20 to "כ",
        10 to "י",
        9 to "ט", 8 to "ח", 7 to "ז", 6 to "ו",
        5 to "ה", 4 to "ד", 3 to "ג", 2 to "ב", 1 to "א"
    )

    /** Convertit un entier en lettres hébraïques (sans gershayim). */
    fun toHebrew(n: Int): String {
        if (n <= 0) return ""
        var remainder = n
        val sb = StringBuilder()

        // Centaines
        if (remainder >= 100) {
            for ((value, letter) in mapping.filter { it.first >= 100 }) {
                while (remainder >= value) {
                    sb.append(letter)
                    remainder -= value
                }
            }
        }
        // Cas spéciaux 15 / 16
        if (remainder == 15) return sb.append("טו").toString()
        if (remainder == 16) return sb.append("טז").toString()

        for ((value, letter) in mapping.filter { it.first < 100 }) {
            while (remainder >= value) {
                sb.append(letter)
                remainder -= value
            }
        }
        return sb.toString()
    }

    /**
     * Convertit une chaîne hébraïque en entier (best-effort).
     * Ignore les caractères non-hébreu (gershayim, espaces, ponctuation).
     */
    fun toInt(s: String): Int? {
        val cleaned = s.filter { it.code in 0x05D0..0x05EA }
        if (cleaned.isEmpty()) return null

        val values: Map<Char, Int> = mapOf(
            'א' to 1, 'ב' to 2, 'ג' to 3, 'ד' to 4, 'ה' to 5,
            'ו' to 6, 'ז' to 7, 'ח' to 8, 'ט' to 9, 'י' to 10,
            'כ' to 20, 'ך' to 20, 'ל' to 30, 'מ' to 40, 'ם' to 40,
            'נ' to 50, 'ן' to 50, 'ס' to 60, 'ע' to 70,
            'פ' to 80, 'ף' to 80, 'צ' to 90, 'ץ' to 90,
            'ק' to 100, 'ר' to 200, 'ש' to 300, 'ת' to 400
        )

        var total = 0
        for (ch in cleaned) {
            val v = values[ch] ?: return null
            total += v
        }
        return if (total > 0) total else null
    }
}
