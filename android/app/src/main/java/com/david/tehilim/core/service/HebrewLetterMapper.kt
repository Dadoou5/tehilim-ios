package com.david.tehilim.core.service

/**
 * Mirror exact du HebrewLetterMapper iOS.
 *
 * 1. **Final → base** : les 5 lettres finales (ך, ם, ן, ף, ץ) mappent à leur
 *    forme de base (כ, מ, נ, פ, צ) pour pouvoir retrouver la section
 *    correspondante du Tehilim 119.
 * 2. **Validation Hebrew** : ne garde que les caractères Unicode hébreu
 *    (0x05D0–0x05EA).
 */
object HebrewLetterMapper {

    /** Table de correspondance lettres finales → lettre de base. */
    val finalToBase: Map<Char, Char> = mapOf(
        'ך' to 'כ',
        'ם' to 'מ',
        'ן' to 'נ',
        'ף' to 'פ',
        'ץ' to 'צ'
    )

    /** Plage Unicode des lettres hébraïques (base + finales). */
    private val hebrewLetterRange = 0x05D0..0x05EA

    /** Convertit un caractère en sa forme de base. Non-hébreu retournés tels quels. */
    fun toBase(c: Char): Char = finalToBase[c] ?: c

    /** Extrait les lettres hébraïques de base, ignorant tout le reste. */
    fun baseLetters(string: String): List<Char> =
        string.mapNotNull { c -> if (isHebrewLetter(c)) toBase(c) else null }

    fun isHebrewLetter(c: Char): Boolean = c.code in hebrewLetterRange

    /** Pour usage dans TextField onValueChange : filtre tout non-hébreu en live. */
    fun filterHebrew(string: String): String = string.filter { isHebrewLetter(it) }

    fun isValidHebrewName(string: String): Boolean {
        val trimmed = string.trim()
        return trimmed.isNotEmpty() && trimmed.all { isHebrewLetter(it) }
    }
}
