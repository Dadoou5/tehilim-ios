package com.david.tehilim.core.model

/**
 * Enums utilisés par les Préférences. Mirror du modèle iOS Preferences.swift.
 */

enum class AppLanguage(val storageValue: String) {
    SYSTEM("system"),
    FR("fr"),
    EN("en");

    /**
     * Langue de traduction effective utilisée pour afficher les versets.
     * SYSTEM = on regarde la locale du device.
     */
    val translation: TranslationLanguage get() = when (this) {
        EN -> TranslationLanguage.EN
        FR -> TranslationLanguage.FR
        SYSTEM -> {
            val locale = java.util.Locale.getDefault().language
            if (locale.startsWith("en")) TranslationLanguage.EN else TranslationLanguage.FR
        }
    }
}

enum class AppTheme(val label: String) {
    SYSTEM("Système"),
    LIGHT("Clair"),
    DARK("Sombre");
}

enum class TextMode(val label: String) {
    HEBREW("Hébreu"),
    PHONETIC("Phonétique");
}

/** Échelle d'affichage du texte (mêmes ratios que iOS). */
enum class TextSize(val scale: Float, val label: String) {
    XSMALL(0.85f, "Très petit"),
    SMALL(0.95f, "Petit"),
    MEDIUM(1.0f, "Moyen"),
    LARGE(1.15f, "Grand"),
    XLARGE(1.30f, "Très grand");
}

enum class VerseNumberStyle(val label: String) {
    HEBREW("Hébreu"),
    ARABIC("Numérique");
}
