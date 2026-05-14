package com.david.tehilim.features.psalm119

import com.david.tehilim.core.model.ReadingLetterItem

/**
 * Contexte de navigation dans une séquence personnalisée du Tehilim 119.
 * Mirror du PsalmSequenceContext SwiftUI.
 */
data class PsalmSequenceContext(
    val items: List<ReadingLetterItem>,
    val currentPosition: Int,
    val savedIntentId: String? = null
) {
    val currentItem: ReadingLetterItem? = items.getOrNull(currentPosition)
    val hasPrevious: Boolean = currentPosition > 0
    val hasNext: Boolean = currentPosition < items.size - 1
    val progressLabel: String = "Lettre ${currentPosition + 1} sur ${items.size}"

    fun previousItem(): ReadingLetterItem? = items.getOrNull(currentPosition - 1)
    fun nextItem(): ReadingLetterItem? = items.getOrNull(currentPosition + 1)
    fun advanceBy(delta: Int): PsalmSequenceContext =
        copy(currentPosition = (currentPosition + delta).coerceIn(0, items.size - 1))
}
