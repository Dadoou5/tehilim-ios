package com.david.tehilim

import com.david.tehilim.core.service.HebrewTransliterator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests basiques du HebrewTransliterator — mirror partiel des tests iOS.
 * Garantit que les ports Swift et Kotlin produisent les mêmes résultats.
 */
class HebrewTransliteratorTest {

    @Test fun `simple mizmor`() {
        // מִזְמוֹר — mizmor (hiriq + zayin + shva + mem + vav-holam + resh)
        // attendu approximatif : "mizmor"
        val result = HebrewTransliterator.transliterate("מִזְמוֹר")
        assertTrue("expected 'm' and 'r' in result : $result", result.contains("m") && result.endsWith("r"))
    }

    @Test fun `tetragrammaton replaced by Adonai`() {
        val result = HebrewTransliterator.transliterate("יְהוָה")
        assertTrue("expected Adonaï substitution : $result", result.contains("Adona"))
    }

    @Test fun `shin vs sin distinction`() {
        // שָׁ avec point sur droite = sh → "cha"
        // שָׂ avec point sur gauche = s → "sa"
        val shin = HebrewTransliterator.transliterate("שָׁ")
        val sin = HebrewTransliterator.transliterate("שָׂ")
        assertTrue("Shin should start with 'ch' : $shin", shin.startsWith("ch"))
        assertTrue("Sin should start with 's' : $sin", sin.startsWith("s"))
    }

    @Test fun `tsadi gives ts`() {
        // צָ → "tsa"
        val result = HebrewTransliterator.transliterate("צָ")
        assertTrue("expected 'ts' : $result", result.contains("ts"))
    }

    @Test fun `vav as shuruq is ou`() {
        // וּ (vav + dagesh seul) → "ou"
        val result = HebrewTransliterator.transliterate("וּ")
        assertEquals("ou", result)
    }

    @Test fun `vav as holam is o`() {
        // וֹ → "o"
        val result = HebrewTransliterator.transliterate("וֹ")
        assertEquals("o", result)
    }
}
