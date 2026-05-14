package com.david.tehilim.core.service

/**
 * Mirror exact du HebrewTransliterator iOS — translittération sépharade
 * pour audience francophone.
 *
 * Approche : règles algorithmiques caractère-par-caractère, pré-passe sur
 * les noms divins (Tétragramme יהוה → "Adonaï").
 *
 * Limites connues (mêmes que iOS) :
 * - Le ḥiriq-yod génère parfois "iy" au lieu de "i"
 * - Le shva est par défaut silencieux
 * - Le qamats qatan non détecté contextuellement
 */
object HebrewTransliterator {

    fun transliterate(source: String): String {
        val pre = replaceDivineNames(source)
        val walked = walk(pre)
        return walked.replace(Regex(" +"), " ").trim()
    }

    // MARK: - Walk

    private fun walk(s: String): String {
        val output = StringBuilder()
        val codePoints = s.codePoints().toArray()
        var i = 0
        var lastVowel = ""

        while (i < codePoints.size) {
            val sc = codePoints[i]
            when {
                isLetter(sc) -> {
                    val diacritics = mutableListOf<Int>()
                    var j = i + 1
                    while (j < codePoints.size && isDiacritic(codePoints[j])) {
                        diacritics += codePoints[j]
                        j++
                    }
                    // Mater lectionis : yod nu après ḥiriq → muet
                    if (sc == 0x05D9 && diacritics.isEmpty() && lastVowel == "i") {
                        i = j
                        continue
                    }
                    output.append(transliterateLetter(sc, diacritics))
                    lastVowel = vowel(diacritics)
                    i = j
                }
                sc == 0x05BE -> { output.append("-"); lastVowel = ""; i++ }                // maqqef
                sc == 0x05C0 || sc == 0x05C3 -> { output.append(" "); lastVowel = ""; i++ } // paseq, sof pasuq
                isWhitespace(sc) -> { output.append(" "); lastVowel = ""; i++ }
                isDiacritic(sc) -> { i++ }
                else -> { output.appendCodePoint(sc); lastVowel = ""; i++ }
            }
        }
        return output.toString()
    }

    // MARK: - Single-letter mapping

    private fun transliterateLetter(letter: Int, diacritics: List<Int>): String {
        val hasSinDot = diacritics.contains(0x05C2)
        val hasDagesh = diacritics.contains(0x05BC)
        val hasHolam = diacritics.contains(0x05B9)

        // Vav vocalique
        if (letter == 0x05D5) {
            if (hasHolam) return "o"
            if (hasDagesh && diacritics.none { isVowelMark(it) && it != 0x05BC }) {
                return "ou"
            }
        }

        val consonant: String = when (letter) {
            0x05D0 -> ""                                       // alef
            0x05D1 -> if (hasDagesh) "b" else "v"
            0x05D2 -> "g"
            0x05D3 -> "d"
            0x05D4 -> "h"
            0x05D5 -> "v"
            0x05D6 -> "z"
            0x05D7 -> "h"
            0x05D8 -> "t"
            0x05D9 -> "y"
            0x05DA, 0x05DB -> if (hasDagesh) "k" else "kh"
            0x05DC -> "l"
            0x05DD, 0x05DE -> "m"
            0x05DF, 0x05E0 -> "n"
            0x05E1 -> "s"
            0x05E2 -> ""                                       // ayin silencieux
            0x05E3, 0x05E4 -> if (hasDagesh) "p" else "f"
            0x05E5, 0x05E6 -> "ts"
            0x05E7 -> "k"
            0x05E8 -> "r"
            0x05E9 -> if (hasSinDot) "s" else "ch"
            0x05EA -> "t"
            else -> ""
        }
        return consonant + vowel(diacritics)
    }

    private fun vowel(diacritics: List<Int>): String {
        for (d in diacritics) {
            when (d) {
                0x05B0 -> return ""           // shva
                0x05B1 -> return "é"          // hataf segol
                0x05B2 -> return "a"          // hataf patah
                0x05B3 -> return "o"          // hataf qamats
                0x05B4 -> return "i"          // hiriq
                0x05B5 -> return "é"          // tsere
                0x05B6 -> return "é"          // segol
                0x05B7 -> return "a"          // patah
                0x05B8 -> return "a"          // qamats sépharade
                0x05B9 -> return "o"          // holam
                0x05BB -> return "ou"         // qubuts
                0x05C7 -> return "o"          // qamats qatan
            }
        }
        return ""
    }

    private fun isVowelMark(cp: Int): Boolean = cp in 0x05B0..0x05BB || cp == 0x05C7

    // MARK: - Divine names pre-pass

    private val tetragrammatonLetters = intArrayOf(0x05D9, 0x05D4, 0x05D5, 0x05D4)

    private fun replaceDivineNames(s: String): String {
        val output = StringBuilder()
        val codePoints = s.codePoints().toArray()
        var i = 0
        while (i < codePoints.size) {
            val matchLen = matchTetragrammaton(i, codePoints)
            if (matchLen != null) {
                output.append("Adonaï")
                i += matchLen
            } else {
                output.appendCodePoint(codePoints[i])
                i++
            }
        }
        return output.toString()
    }

    private fun matchTetragrammaton(start: Int, scalars: IntArray): Int? {
        var i = start
        var p = 0
        while (p < tetragrammatonLetters.size && i < scalars.size) {
            val s = scalars[i]
            when {
                isLetter(s) -> {
                    if (s != tetragrammatonLetters[p]) return null
                    p++; i++
                }
                isDiacritic(s) -> i++
                else -> return null
            }
        }
        return if (p == tetragrammatonLetters.size) i - start else null
    }

    // MARK: - Char classification

    private fun isLetter(cp: Int): Boolean = cp in 0x05D0..0x05EA

    private fun isDiacritic(cp: Int): Boolean =
        cp in 0x0591..0x05BD ||
            cp == 0x05BF ||
            cp in 0x05C1..0x05C2 ||
            cp in 0x05C4..0x05C5 ||
            cp == 0x05C7

    private fun isWhitespace(cp: Int): Boolean =
        cp == 0x20 || cp == 0x09 || cp == 0x0A || cp == 0xA0
}
