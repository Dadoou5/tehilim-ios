package com.david.tehilim

import com.david.tehilim.core.service.HebrewLetterMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Mirror exact des tests iOS HebrewLetterMapperTests.
 * Garantit que les 2 ports (Swift + Kotlin) produisent les mêmes résultats.
 */
class HebrewLetterMapperTest {

    @Test fun `finals map to base`() {
        assertEquals('כ', HebrewLetterMapper.toBase('ך'))
        assertEquals('מ', HebrewLetterMapper.toBase('ם'))
        assertEquals('נ', HebrewLetterMapper.toBase('ן'))
        assertEquals('פ', HebrewLetterMapper.toBase('ף'))
        assertEquals('צ', HebrewLetterMapper.toBase('ץ'))
    }

    @Test fun `base letters pass through`() {
        assertEquals('א', HebrewLetterMapper.toBase('א'))
        assertEquals('ת', HebrewLetterMapper.toBase('ת'))
    }

    @Test fun `non-hebrew passes through unchanged`() {
        assertEquals('a', HebrewLetterMapper.toBase('a'))
        assertEquals('5', HebrewLetterMapper.toBase('5'))
    }

    @Test fun `isHebrewLetter detects range correctly`() {
        assertTrue(HebrewLetterMapper.isHebrewLetter('א'))
        assertTrue(HebrewLetterMapper.isHebrewLetter('ך'))
        assertTrue(HebrewLetterMapper.isHebrewLetter('ת'))
        assertFalse(HebrewLetterMapper.isHebrewLetter('a'))
        assertFalse(HebrewLetterMapper.isHebrewLetter(' '))
        assertFalse(HebrewLetterMapper.isHebrewLetter('1'))
    }

    @Test fun `filterHebrew keeps only hebrew chars`() {
        assertEquals("יוסף", HebrewLetterMapper.filterHebrew("יוסף"))
        assertEquals("יוסף", HebrewLetterMapper.filterHebrew("Yossef יוסף"))
        assertEquals("יוסף", HebrewLetterMapper.filterHebrew("יוסף 123"))
        assertEquals("", HebrewLetterMapper.filterHebrew("abc"))
    }

    @Test fun `baseLetters returns final-mapped chars`() {
        // שלום : ם final → מ
        assertEquals(listOf('ש', 'ל', 'ו', 'מ'), HebrewLetterMapper.baseLetters("שלום"))
    }

    @Test fun `baseLetters with ירושלים`() {
        // ירושלים : ם final → מ
        assertEquals(
            listOf('י', 'ר', 'ו', 'ש', 'ל', 'י', 'מ'),
            HebrewLetterMapper.baseLetters("ירושלים")
        )
    }

    @Test fun `isValidHebrewName accepts and rejects correctly`() {
        assertTrue(HebrewLetterMapper.isValidHebrewName("יוסף"))
        assertTrue(HebrewLetterMapper.isValidHebrewName("שרה"))
        assertFalse(HebrewLetterMapper.isValidHebrewName(""))
        assertFalse(HebrewLetterMapper.isValidHebrewName("   "))
        assertFalse(HebrewLetterMapper.isValidHebrewName("Yossef"))
        assertFalse(HebrewLetterMapper.isValidHebrewName("יוסף 123"))
    }
}
