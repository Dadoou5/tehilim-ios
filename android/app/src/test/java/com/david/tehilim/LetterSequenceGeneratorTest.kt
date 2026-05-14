package com.david.tehilim

import com.david.tehilim.core.model.LetterSource
import com.david.tehilim.core.model.PrayerType
import com.david.tehilim.core.model.RelationType
import com.david.tehilim.core.service.LetterSequenceGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Mirror exact des tests iOS pour le LetterSequenceGenerator.
 * Garantit la conformité avec la règle métier non-négociable :
 * « נשמה » ajouté UNIQUEMENT pour Défunt.
 */
class LetterSequenceGeneratorTest {

    @Test fun `malade does not append nechama`() {
        val seq = LetterSequenceGenerator.generate(
            relativeName = "יוסף",
            relation = RelationType.BEN,
            motherName = "שרה",
            prayerType = PrayerType.MALADE
        )
        // יוסף (4) + בן (2) + שרה (3) = 9
        assertEquals(9, seq.size)
        assertFalse(seq.any { it.source == LetterSource.NESHAMA })
    }

    @Test fun `defunt appends nechama`() {
        val seq = LetterSequenceGenerator.generate(
            relativeName = "דוד",
            relation = RelationType.BEN,
            motherName = "רחל",
            prayerType = PrayerType.DEFUNT
        )
        // דוד (3) + בן (2) + רחל (3) + נשמה (4) = 12
        assertEquals(12, seq.size)
        val neshama = seq.filter { it.source == LetterSource.NESHAMA }
        assertEquals(4, neshama.size)
        assertEquals(listOf("נ", "ש", "מ", "ה"), neshama.map { it.character })
    }

    @Test fun `ben yossef ben sara expected order`() {
        val seq = LetterSequenceGenerator.generate(
            relativeName = "יוסף",
            relation = RelationType.BEN,
            motherName = "שרה",
            prayerType = PrayerType.MALADE
        )
        // ף final → פ
        val chars = seq.map { it.character }
        assertEquals(listOf("י", "ו", "ס", "פ", "ב", "נ", "ש", "ר", "ה"), chars)
    }

    @Test fun `bat for daughter`() {
        val seq = LetterSequenceGenerator.generate(
            relativeName = "רחל",
            relation = RelationType.BAT,
            motherName = "לאה",
            prayerType = PrayerType.MALADE
        )
        val chars = seq.map { it.character }
        assertEquals(listOf("ר", "ח", "ל", "ב", "ת", "ל", "א", "ה"), chars)
    }

    @Test fun `final letters are mapped`() {
        // אברהם contient ם final → doit être mappé en מ
        val seq = LetterSequenceGenerator.generate(
            relativeName = "אברהם",
            relation = RelationType.BEN,
            motherName = "שרה",
            prayerType = PrayerType.MALADE
        )
        val chars = seq.map { it.character }
        assertEquals(listOf("א", "ב", "ר", "ה", "מ"), chars.take(5))
    }

    @Test fun `sources correctly assigned`() {
        val seq = LetterSequenceGenerator.generate(
            relativeName = "דן",
            relation = RelationType.BEN,
            motherName = "שרה",
            prayerType = PrayerType.DEFUNT
        )
        // דן → ד נ (ן final → נ)
        assertEquals(LetterSource.PROCHE, seq[0].source)
        assertEquals("ד", seq[0].character)
        assertEquals(LetterSource.PROCHE, seq[1].source)
        assertEquals("נ", seq[1].character)
        assertEquals(LetterSource.LIEN, seq[2].source)
        assertEquals(LetterSource.LIEN, seq[3].source)
        assertEquals(LetterSource.MERE, seq[4].source)
        assertEquals(LetterSource.MERE, seq[5].source)
        assertEquals(LetterSource.MERE, seq[6].source)
        assertEquals(LetterSource.NESHAMA, seq[7].source)
        assertEquals(LetterSource.NESHAMA, seq[10].source)
    }

    @Test fun `psalmLetterKey never contains finals`() {
        val seq = LetterSequenceGenerator.generate(
            relativeName = "צחק",
            relation = RelationType.BEN,
            motherName = "פנינה",
            prayerType = PrayerType.MALADE
        )
        val finals = setOf("ך", "ם", "ן", "ף", "ץ")
        for (item in seq) {
            assertEquals(item.psalmLetterKey, item.character)
            assertFalse(finals.contains(item.character))
        }
    }

    @Test fun `orderIndex is sequential`() {
        val seq = LetterSequenceGenerator.generate(
            relativeName = "דוד",
            relation = RelationType.BEN,
            motherName = "רחל",
            prayerType = PrayerType.DEFUNT
        )
        seq.forEachIndexed { i, item -> assertEquals(i, item.orderIndex) }
    }

    @Test fun `makeTitle format malade`() {
        val title = LetterSequenceGenerator.makeTitle(
            prayerType = PrayerType.MALADE,
            relativeName = "יוסף",
            relation = RelationType.BEN,
            motherName = "שרה"
        )
        assertEquals("Refoua Cheléma — יוסף בן שרה", title)
    }

    @Test fun `makeTitle format defunt`() {
        val title = LetterSequenceGenerator.makeTitle(
            prayerType = PrayerType.DEFUNT,
            relativeName = "דוד",
            relation = RelationType.BEN,
            motherName = "רחל"
        )
        assertEquals("Lelouy Nichmat — דוד בן רחל", title)
    }
}
