package com.david.tehilim

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.david.tehilim.core.model.HebrewYMD
import com.david.tehilim.core.model.PrayerType
import com.david.tehilim.core.model.RelationType
import com.david.tehilim.core.model.SavedPrayerIntent
import com.david.tehilim.core.service.MemorialCalculator
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar
import java.util.Date

/**
 * Tests instrumentation pour MemorialCalculator + rétrocompat SavedPrayerIntent.
 *
 * Doivent être en androidTest/ (et non test/) parce que MemorialCalculator
 * utilise `android.icu.HebrewCalendar` qui n'existe pas en JVM pur.
 *
 * Mirror direct du test iOS MemorialCalculatorTests.swift — vérifie la
 * parité algorithme entre les deux plateformes.
 *
 * Pour lancer : `./gradlew :app:connectedDebugAndroidTest` (émulateur/device).
 */
@RunWith(AndroidJUnit4::class)
class MemorialCalculatorTest {

    private fun civil(y: Int, m: Int, d: Int): Date {
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(y, m - 1, d, 12, 0, 0)
        return cal.time
    }

    /** Cherche dynamiquement une année hébraïque où `month` (ICU) a `expectedDays`. */
    private fun findYear(month: Int, expectedDays: Int): Int? =
        (5780..5810).firstOrNull { MemorialCalculator.daysInMonth(month, it) == expectedDays }

    // Cycle Méton

    @Test
    fun leapYears_metonic() {
        assertTrue("5784 doit être embolismique", MemorialCalculator.isLeap(5784))
        assertFalse("5785 doit être commune", MemorialCalculator.isLeap(5785))
        assertFalse("5786 doit être commune", MemorialCalculator.isLeap(5786))
        assertTrue("5787 doit être embolismique", MemorialCalculator.isLeap(5787))
    }

    // Adaptation du mois

    @Test
    fun adar_nonLeapToLeap_observedInAdarII() {
        // ICU : ADAR (6) = Adar unique en commune OU Adar II en leap. Donc
        // source ADAR (6, non-leap) → target ADAR (6, leap) reste 6.
        val m = MemorialCalculator.adjustedMonth(
            sourceMonth = 6, sourceLeap = false, targetLeap = true
        )
        assertEquals(6, m)
    }

    @Test
    fun adarI_leapToNonLeap_observedInSingleAdar() {
        // Source ADAR_1 (5, leap) → cible commune : Adar unique = ADAR (6).
        val m = MemorialCalculator.adjustedMonth(
            sourceMonth = 5, sourceLeap = true, targetLeap = false
        )
        assertEquals(6, m)
    }

    @Test
    fun adarI_leapToLeap_stillAdarI() {
        val m = MemorialCalculator.adjustedMonth(
            sourceMonth = 5, sourceLeap = true, targetLeap = true
        )
        assertEquals(5, m)
    }

    @Test
    fun otherMonths_unchanged() {
        // Tishri (0) reste Tishri quel que soit le contexte.
        assertEquals(0, MemorialCalculator.adjustedMonth(0, false, false))
        assertEquals(0, MemorialCalculator.adjustedMonth(0, false, true))
        // Nisan (7) reste Nisan (ICU n'a pas de décalage post-Adar).
        assertEquals(7, MemorialCalculator.adjustedMonth(7, false, true))
        assertEquals(7, MemorialCalculator.adjustedMonth(7, true, false))
    }

    // Adaptation du jour (rollover 30 → 1)

    @Test
    fun heshvan30_rolloverToKislev1_inHeshvan29Year() {
        // ICU HESHVAN = 1, KISLEV = 2
        val year = findYear(1, 29) ?: error("No year with Heshvan=29 found")
        val (m, d) = MemorialCalculator.adjustedDay(
            sourceDay = 30, sourceMonth = 1, sourceLeap = false,
            targetMonth = 1, targetYear = year
        )
        assertEquals("Heshvan 30 + Heshvan-29 year → Kislev (2)", 2, m)
        assertEquals(1, d)
    }

    @Test
    fun kislev30_rolloverToTevet1_inKislev29Year() {
        // ICU KISLEV = 2, TEVET = 3
        val year = findYear(2, 29) ?: error("No year with Kislev=29 found")
        val (m, d) = MemorialCalculator.adjustedDay(
            sourceDay = 30, sourceMonth = 2, sourceLeap = false,
            targetMonth = 2, targetYear = year
        )
        assertEquals("Kislev 30 + Kislev-29 year → Tevet (3)", 3, m)
        assertEquals(1, d)
    }

    @Test
    fun adarI30_inNonLeapTarget_observedOn30Shevat() {
        // Source 30 ADAR_1 (5, leap) + cible non-leap → 30 SHEVAT (4).
        val (m, d) = MemorialCalculator.adjustedDay(
            sourceDay = 30, sourceMonth = 5, sourceLeap = true,
            targetMonth = 6, targetYear = 5786 // non-leap
        )
        assertEquals("30 Adar I + non-leap target → Shevat (4)", 4, m)
        assertEquals(30, d)
    }

    @Test
    fun regularDay_noRollover() {
        val (m, d) = MemorialCalculator.adjustedDay(
            sourceDay = 15, sourceMonth = 0, sourceLeap = false,
            targetMonth = 0, targetYear = 5786
        )
        assertEquals(0, m)
        assertEquals(15, d)
    }

    // End-to-end

    @Test
    fun nextYahrzeit_returnsFutureDate() {
        val death = civil(2024, 5, 24)
        val now = civil(2026, 5, 25)
        val next = MemorialCalculator.nextYahrzeit(death, now)
        assertNotNull(next)
        assertTrue("next doit être après now", next!!.after(now))
    }

    @Test
    fun nextYahrzeit_inSameHebrewYearIfStillAhead() {
        val death = civil(2025, 1, 1)
        val now = civil(2025, 7, 1)
        val next = MemorialCalculator.nextYahrzeit(death, now)
        assertNotNull(next)
        val cal = Calendar.getInstance()
        cal.time = next!!
        val nextYear = cal.get(Calendar.YEAR)
        // Doit tomber en 2026 (l'azcara 2025 est déjà passée le 1 juillet).
        assertEquals(2026, nextYear)
    }

    // Conversion civile ↔ hébraïque

    @Test
    fun civilToHebrew_returnsKnownYear() {
        val (y, _, _) = with(MemorialCalculator.hebrewYMD(civil(2026, 5, 24))) {
            Triple(year, month, day)
        }
        assertEquals(5786, y)
    }

    // Rétrocompat sérialisation

    @Test
    fun savedPrayerIntent_decodesLegacyJsonWithoutMemorialFields() {
        val legacyJson = """
            {
              "id": "11111111-2222-3333-4444-555555555555",
              "title": "Lelouy Nichmat — יוסף בן שרה",
              "prayerType": "DEFUNT",
              "relativeFirstName": "יוסף",
              "relationType": "BEN",
              "motherFirstName": "שרה",
              "generatedLetters": [],
              "createdAtEpochMillis": 1735689600000
            }
        """.trimIndent()
        val parser = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val intent = parser.decodeFromString(SavedPrayerIntent.serializer(), legacyJson)

        assertEquals("יוסף", intent.relativeFirstName)
        assertNull(intent.civilDateOfDeathEpochMillis)
        assertNull(intent.hebrewDateOfDeath)
        assertFalse(intent.remindersEnabled)
        assertTrue(intent.notifySevenDaysBefore)   // défaut
        assertTrue(intent.notifySameDay)           // défaut
    }

    @Test
    fun savedPrayerIntent_roundTripWithMemorialFields() {
        val intent = SavedPrayerIntent(
            title = "Lelouy Nichmat — Test",
            prayerType = PrayerType.DEFUNT,
            relativeFirstName = "יוסף",
            relationType = RelationType.BEN,
            motherFirstName = "שרה",
            generatedLetters = emptyList(),
            civilDateOfDeathEpochMillis = civil(2024, 1, 15).time,
            hebrewDateOfDeath = HebrewYMD(year = 5784, month = 4, day = 5),
            remindersEnabled = true,
            notifySevenDaysBefore = true,
            notifySameDay = false
        )
        val parser = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val json = parser.encodeToString(SavedPrayerIntent.serializer(), intent)
        val back = parser.decodeFromString(SavedPrayerIntent.serializer(), json)

        assertEquals(true, back.remindersEnabled)
        assertEquals(false, back.notifySameDay)
        assertEquals(5784, back.hebrewDateOfDeath?.year)
    }
}
