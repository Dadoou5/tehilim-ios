import Foundation

/// Contexte de navigation dans une séquence personnalisée du Tehilim 119.
///
/// Passé en paramètre optionnel à `Psalm119SectionView` :
/// - quand `nil`, la vue navigue dans l'alphabet complet (1 → 22)
/// - quand non-nil, prev/next naviguent dans la séquence générée, et
///   l'app affiche « Lettre X sur N » + désactive prev/next aux bornes.
struct PsalmSequenceContext {
    let items: [ReadingLetterItem]
    /// Position courante dans la séquence (0-indexed).
    let currentPosition: Int
    /// Id de la prière sauvegardée (pour mémoriser le `lastReadIndex`).
    /// `nil` si on lit une séquence non-sauvegardée.
    let savedIntentId: UUID?

    var currentItem: ReadingLetterItem? {
        items.indices.contains(currentPosition) ? items[currentPosition] : nil
    }

    var hasPrevious: Bool { currentPosition > 0 }
    var hasNext: Bool { currentPosition < items.count - 1 }

    func previousItem() -> ReadingLetterItem? {
        guard hasPrevious else { return nil }
        return items[currentPosition - 1]
    }

    func nextItem() -> ReadingLetterItem? {
        guard hasNext else { return nil }
        return items[currentPosition + 1]
    }

    /// Retourne un nouveau contexte avec position incrémentée/décrémentée.
    func advance(by delta: Int) -> PsalmSequenceContext {
        let newPos = max(0, min(items.count - 1, currentPosition + delta))
        return PsalmSequenceContext(
            items: items,
            currentPosition: newPos,
            savedIntentId: savedIntentId
        )
    }

    var progressLabel: String {
        "Lettre \(currentPosition + 1) sur \(items.count)"
    }
}
