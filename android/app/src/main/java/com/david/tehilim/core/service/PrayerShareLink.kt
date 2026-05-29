package com.david.tehilim.core.service

import android.content.Context
import android.net.Uri
import com.david.tehilim.R
import com.david.tehilim.core.model.PrayerType
import com.david.tehilim.core.model.RelationType
import com.david.tehilim.core.model.SavedPrayerIntent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Mirror Android du PrayerShareLink.swift iOS.
 *
 * Encode / décode une prière partageable via lien `tehilim://prayer`.
 * Le lien est **minimal** : type, prénom du proche, lien de parenté, prénom
 * de la mère, date du décès optionnelle. La séquence de lettres et la date
 * hébraïque sont **recalculées à l'import** (elles dérivent de ces champs).
 *
 * Format : `tehilim://prayer?v=1&type=<malade|defunt>&name=<enc>&rel=<ben|bat>&mother=<enc>&death=<yyyy-MM-dd>`
 *
 * Le schéma `tehilim://` est déjà déclaré dans l'AndroidManifest (intent-filter
 * `<data android:scheme="tehilim" />`, sans host → capture tous les hosts).
 */
object PrayerShareLink {

    const val SCHEME = "tehilim"
    const val HOST = "prayer"
    const val VERSION = "1"

    /** `yyyy-MM-dd` / locale US : format stable, identique à iOS. */
    private fun dateFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // MARK: - Encodage

    fun uri(intent: SavedPrayerIntent): Uri {
        val builder = Uri.Builder()
            .scheme(SCHEME)
            .authority(HOST)
            .appendQueryParameter("v", VERSION)
            .appendQueryParameter("type", intent.prayerType.wire)
            .appendQueryParameter("name", intent.relativeFirstName)
            .appendQueryParameter("rel", intent.relationType.wire)
            .appendQueryParameter("mother", intent.motherFirstName)
        intent.civilDateOfDeathEpochMillis?.let {
            builder.appendQueryParameter("death", dateFormat().format(Date(it)))
        }
        return builder.build()
    }

    /** Message texte prêt à partager (SMS / WhatsApp) : description + lien. */
    fun shareMessage(context: Context, intent: SavedPrayerIntent): String {
        val link = uri(intent).toString()
        val intro = context.getString(R.string.share_prayer_intro)
        return "${intent.prayerType.saveActionTitle} — ${intent.hebrewSubject}\n\n$intro\n$link"
    }

    // MARK: - Décodage

    data class Payload(
        val prayerType: PrayerType,
        val relativeFirstName: String,
        val relationType: RelationType,
        val motherFirstName: String,
        val civilDateOfDeathEpochMillis: Long?
    ) {
        val hebrewSubject: String
            get() = "$relativeFirstName ${relationType.hebrew} $motherFirstName"
    }

    /** Parse `tehilim://prayer?...` ; null si le lien n'est pas valide. */
    fun payload(uri: Uri): Payload? {
        if (uri.scheme != SCHEME || uri.host != HOST) return null
        val type = uri.getQueryParameter("type")?.let { prayerTypeFromWire(it) } ?: return null
        val rel = uri.getQueryParameter("rel")?.let { relationFromWire(it) } ?: return null
        val name = uri.getQueryParameter("name")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val mother = uri.getQueryParameter("mother")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val death = uri.getQueryParameter("death")?.let {
            runCatching { dateFormat().parse(it)?.time }.getOrNull()
        }
        return Payload(type, name, rel, mother, death)
    }

    /**
     * Construit un [SavedPrayerIntent] complet : régénère la séquence et la
     * date hébraïque. Rappels désactivés par défaut (le destinataire décide).
     */
    fun makeIntent(p: Payload): SavedPrayerIntent {
        val sequence = LetterSequenceGenerator.generate(
            relativeName = p.relativeFirstName,
            relation = p.relationType,
            motherName = p.motherFirstName,
            prayerType = p.prayerType
        )
        return SavedPrayerIntent(
            title = LetterSequenceGenerator.makeTitle(
                prayerType = p.prayerType,
                relativeName = p.relativeFirstName,
                relation = p.relationType,
                motherName = p.motherFirstName
            ),
            prayerType = p.prayerType,
            relativeFirstName = p.relativeFirstName,
            relationType = p.relationType,
            motherFirstName = p.motherFirstName,
            generatedLetters = sequence,
            civilDateOfDeathEpochMillis = p.civilDateOfDeathEpochMillis,
            hebrewDateOfDeath = p.civilDateOfDeathEpochMillis?.let {
                MemorialCalculator.hebrewYMD(Date(it))
            },
            remindersEnabled = false
        )
    }

    // MARK: - Valeurs « wire » (identiques à iOS rawValue)

    private val PrayerType.wire: String
        get() = when (this) {
            PrayerType.MALADE -> "malade"
            PrayerType.DEFUNT -> "defunt"
        }

    private val RelationType.wire: String
        get() = when (this) {
            RelationType.BEN -> "ben"
            RelationType.BAT -> "bat"
        }

    private fun prayerTypeFromWire(s: String): PrayerType? = when (s.lowercase()) {
        "malade" -> PrayerType.MALADE
        "defunt" -> PrayerType.DEFUNT
        else -> null
    }

    private fun relationFromWire(s: String): RelationType? = when (s.lowercase()) {
        "ben" -> RelationType.BEN
        "bat" -> RelationType.BAT
        else -> null
    }
}
