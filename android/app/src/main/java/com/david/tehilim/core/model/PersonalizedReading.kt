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
    val lastReadIndex: Int? = null
) {
    val hebrewSubject: String
        get() = "$relativeFirstName ${relationType.hebrew} $motherFirstName"
}
