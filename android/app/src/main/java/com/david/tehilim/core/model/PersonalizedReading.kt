package com.david.tehilim.core.model

import android.content.Context
import androidx.annotation.StringRes
import com.david.tehilim.R
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Mirror direct du modèle iOS PersonalizedReading.swift.
 *
 * **Règle métier non-négociable** : « נשמה » est ajouté en fin de séquence
 * UNIQUEMENT pour Défunt (Lelouy Nichmat). Jamais l'utilisateur ne tape
 * « נשמה » lui-même.
 */

@Serializable
enum class PrayerType {
    MALADE,
    DEFUNT;

    val labelFR: String get() = when (this) {
        MALADE -> "Malade"
        DEFUNT -> "Défunt"
    }

    val saveActionTitle: String get() = when (this) {
        MALADE -> "Refoua Cheléma"
        DEFUNT -> "Lelouy Nichmat"
    }
}

@Serializable
enum class RelationType {
    BEN, BAT;

    val hebrew: String get() = when (this) {
        BEN -> "בן"
        BAT -> "בת"
    }

    val hebrewCharacters: List<Char> get() = hebrew.toList()
}

@Serializable
enum class LetterSource {
    PROCHE, LIEN, MERE, NESHAMA;

    /** Label localisé (FR/EN selon locale active). NESHAMA reste en hébreu. */
    fun localizedLabel(context: Context): String = when (this) {
        PROCHE  -> context.getString(R.string.letter_source_proche)
        LIEN    -> context.getString(R.string.letter_source_lien)
        MERE    -> context.getString(R.string.letter_source_mere)
        NESHAMA -> "נשמה"
    }

    /** Resource string id (utilisable depuis Composable via stringResource). */
    @get:StringRes
    val labelRes: Int? get() = when (this) {
        PROCHE  -> R.string.letter_source_proche
        LIEN    -> R.string.letter_source_lien
        MERE    -> R.string.letter_source_mere
        NESHAMA -> null // hébreu pur, pas de ressource
    }
}

@Serializable
data class ReadingLetterItem(
    val id: String = UUID.randomUUID().toString(),
    val character: String,             // une seule lettre, stockée en String pour Codable
    val source: LetterSource,
    val orderIndex: Int,
    val psalmLetterKey: String         // = character (forme base) pour mapper à la section 119
) {
    val letter: Char get() = character.firstOrNull() ?: ' '
}

/**
 * Date hébraïque mémorisée — cache des composantes calculées par ICU
 * `HebrewCalendar` à partir d'une date civile. Le `month` suit l'indexation
 * ICU (0=TISHRI, 1=HESHVAN, ..., 5=ADAR_1 leap-only, 6=ADAR, ..., 12=ELUL).
 *
 * V1.4 — feature Commémoration.
 */
@Serializable
data class HebrewYMD(
    val year: Int,
    val month: Int,
    val day: Int
)

/**
 * V1.4 — ajout des 5 champs Commémoration. Les valeurs par défaut + le
 * `Json { ignoreUnknownKeys = true }` du store garantissent la rétrocompat
 * avec les payloads existants (V1.3.x) qui n'ont pas ces champs.
 */
@Serializable
data class SavedPrayerIntent(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val prayerType: PrayerType,
    val relativeFirstName: String,
    val relationType: RelationType,
    val motherFirstName: String,
    val generatedLetters: List<ReadingLetterItem>,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val lastReadIndex: Int? = null,
    // V1.4 — Commémoration. Optionnels / défauts garantissent la rétrocompat.
    val civilDateOfDeathEpochMillis: Long? = null,
    val hebrewDateOfDeath: HebrewYMD? = null,
    val remindersEnabled: Boolean = false,
    val notifySevenDaysBefore: Boolean = true,
    val notifySameDay: Boolean = true
) {
    val hebrewSubject: String
        get() = "$relativeFirstName ${relationType.hebrew} $motherFirstName"
}
