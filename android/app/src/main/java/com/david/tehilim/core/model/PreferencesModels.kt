package com.david.tehilim.core.model

import androidx.annotation.StringRes
import com.david.tehilim.R

/**
 * Enums utilisés par les Préférences. Mirror du modèle iOS Preferences.swift.
 *
 * V1.3.0 — chaque enum expose un `labelRes` (R.string.*) au lieu d'un label
 * français en dur. Le label final est résolu côté UI via stringResource() ou
 * context.getString() pour respecter la locale (FR/EN) choisie par l'utilisateur.
 */

enum class AppLanguage(val storageValue: String, @StringRes val labelRes: Int) {
    SYSTEM("system", R.string.enum_applang_system),
    FR("fr", R.string.enum_applang_fr),
    EN("en", R.string.enum_applang_en),
    HE("he", R.string.enum_applang_he);

    /**
     * Langue de traduction effective utilisée pour afficher les versets.
     * SYSTEM = on regarde la locale du device. En hébreu, le texte source EST
     * l'hébreu — la traduction affichable retombe sur l'anglais (corpus FR/EN).
     */
    val translation: TranslationLanguage get() = when (this) {
        EN, HE -> TranslationLanguage.EN
        FR -> TranslationLanguage.FR
        SYSTEM -> {
            val locale = java.util.Locale.getDefault().language
            // `Locale.getLanguage()` rapporte l'hébreu sous l'ancien code « iw ».
            if (locale.startsWith("en") || locale.startsWith("iw") || locale.startsWith("he"))
                TranslationLanguage.EN
            else TranslationLanguage.FR
        }
    }

    /**
     * Langue du contenu éditorial (cas de la vie : titres, conseils, sections).
     * Contrairement à [translation] (corpus des versets, FR/EN), ce contenu
     * existe désormais en hébreu — d'où une troisième valeur HE.
     */
    val content: ContentLanguage get() = when (this) {
        FR -> ContentLanguage.FR
        EN -> ContentLanguage.EN
        HE -> ContentLanguage.HE
        SYSTEM -> {
            val locale = java.util.Locale.getDefault().language
            when {
                locale.startsWith("iw") || locale.startsWith("he") -> ContentLanguage.HE
                locale.startsWith("en") -> ContentLanguage.EN
                else -> ContentLanguage.FR
            }
        }
    }
}

/** Langue du contenu éditorial localisé en FR / EN / HE (repli HE → EN → FR). */
enum class ContentLanguage { FR, EN, HE }

enum class AppTheme(@StringRes val labelRes: Int) {
    SYSTEM(R.string.enum_theme_system),
    LIGHT(R.string.enum_theme_light),
    DARK(R.string.enum_theme_dark);
}

enum class TextMode(@StringRes val labelRes: Int) {
    HEBREW(R.string.enum_textmode_hebrew),
    PHONETIC(R.string.enum_textmode_phonetic);
}

/** Échelle d'affichage du texte (mêmes ratios que iOS). */
enum class TextSize(val scale: Float, @StringRes val labelRes: Int) {
    XSMALL(0.85f, R.string.enum_textsize_xsmall),
    SMALL(0.95f, R.string.enum_textsize_small),
    MEDIUM(1.0f, R.string.enum_textsize_medium),
    LARGE(1.15f, R.string.enum_textsize_large),
    XLARGE(1.30f, R.string.enum_textsize_xlarge),
    XXLARGE(1.50f, R.string.enum_textsize_xxlarge),
    XXXLARGE(1.75f, R.string.enum_textsize_xxxlarge),
    XXXXLARGE(2.00f, R.string.enum_textsize_xxxxlarge);

    /** Pas suivant / précédent borné (pour les contrôles A− / A+ en lecture). */
    fun stepped(delta: Int): TextSize =
        entries[(ordinal + delta).coerceIn(0, entries.size - 1)]
    val isSmallest get() = ordinal == 0
    val isLargest get() = ordinal == entries.size - 1
}

enum class VerseNumberStyle(@StringRes val labelRes: Int) {
    HEBREW(R.string.enum_versestyle_hebrew),
    ARABIC(R.string.enum_versestyle_arabic);
}
