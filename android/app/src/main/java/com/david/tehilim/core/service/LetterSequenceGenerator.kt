package com.david.tehilim.core.service

import com.david.tehilim.core.model.LetterSource
import com.david.tehilim.core.model.PrayerType
import com.david.tehilim.core.model.ReadingLetterItem
import com.david.tehilim.core.model.RelationType

/**
 * Mirror exact du LetterSequenceGenerator iOS.
 *
 * **Règle métier non-négociable** : « נשמה » ajouté UNIQUEMENT si Défunt.
 *
 * **Ordre de concaténation** :
 * 1. Lettres du prénom du proche
 * 2. Lettres de בן ou בת
 * 3. Lettres du prénom de la mère
 * 4. Lettres de נשמה (Défunt uniquement)
 */
object LetterSequenceGenerator {

    private val neshamaLetters: List<Char> = listOf('נ', 'ש', 'מ', 'ה')

    fun generate(
        relativeName: String,
        relation: RelationType,
        motherName: String,
        prayerType: PrayerType
    ): List<ReadingLetterItem> {
        val items = mutableListOf<ReadingLetterItem>()
        var index = 0

        // 1. Prénom du proche
        for (letter in HebrewLetterMapper.baseLetters(relativeName)) {
            items += ReadingLetterItem(
                character = letter.toString(),
                source = LetterSource.PROCHE,
                orderIndex = index,
                psalmLetterKey = letter.toString()
            )
            index++
        }

        // 2. Lien (בן / בת)
        for (letter in relation.hebrewCharacters) {
            val base = HebrewLetterMapper.toBase(letter)
            items += ReadingLetterItem(
                character = base.toString(),
                source = LetterSource.LIEN,
                orderIndex = index,
                psalmLetterKey = base.toString()
            )
            index++
        }

        // 3. Prénom de la mère
        for (letter in HebrewLetterMapper.baseLetters(motherName)) {
            items += ReadingLetterItem(
                character = letter.toString(),
                source = LetterSource.MERE,
                orderIndex = index,
                psalmLetterKey = letter.toString()
            )
            index++
        }

        // 4. נשמה (Défunt uniquement)
        if (prayerType == PrayerType.DEFUNT) {
            for (letter in neshamaLetters) {
                items += ReadingLetterItem(
                    character = letter.toString(),
                    source = LetterSource.NESHAMA,
                    orderIndex = index,
                    psalmLetterKey = letter.toString()
                )
                index++
            }
        }

        return items
    }

    fun makeTitle(
        prayerType: PrayerType,
        relativeName: String,
        relation: RelationType,
        motherName: String
    ): String =
        "${prayerType.saveActionTitle} — $relativeName ${relation.hebrew} $motherName"
}
