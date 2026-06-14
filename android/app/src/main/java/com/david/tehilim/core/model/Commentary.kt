package com.david.tehilim.core.model

import kotlinx.serialization.Serializable

/**
 * Commentaire classique sur un verset (V2.4 — mode étude).
 * Source : Sefaria (Rashi CC-BY ; Metzudat David, domaine public).
 */
@Serializable
data class CommentaryItem(
    val lemma: String? = null,
    val he: String,
    val en: String? = null,
    val fr: String? = null
)

enum class CommentaryKind(val key: String) {
    RASHI("rashi"),
    METZUDAT("metzudat");

    val hebrewName: String
        get() = when (this) {
            RASHI -> "רש״י"
            METZUDAT -> "מצודת דוד"
        }
}

/** Un commentaire prêt à afficher (commentateur + texte). */
data class VerseCommentary(
    val kind: CommentaryKind,
    val lemma: String?,
    val he: String,
    val en: String?,
    val fr: String?
) {
    /** Texte selon le code langue (« fr »/« en »/« he »), repli FR→EN→HE / EN→HE. */
    fun text(code: String): String = when (code) {
        "fr" -> fr ?: en ?: he
        "en" -> en ?: he
        else -> he
    }
}

/** Index « psaume:verset » → commentaires ordonnés (Rashi puis Metzudat David). */
class CommentaryRepository(private val byKey: Map<String, List<VerseCommentary>>) {
    fun comments(psalmId: Int, verse: Int): List<VerseCommentary> =
        byKey["$psalmId:$verse"].orEmpty()

    val isAvailable: Boolean get() = byKey.isNotEmpty()
}
