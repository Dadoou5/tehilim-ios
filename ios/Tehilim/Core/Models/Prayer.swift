import Foundation

/// Référence à un verset existant du corpus.
struct PrayerVerseRef: Hashable {
    let psalmId: Int
    let verseNumber: Int
}

/// Prière encadrant la lecture des Tehilim.
/// Composée de versets référencés dans le corpus principal — les sources
/// (texte hébreu Sefaria, traduction Beth Loubavitch) sont héritées automatiquement.
struct Prayer: Identifiable, Hashable {
    enum Kind: String, Identifiable, Hashable, CaseIterable {
        case before, after
        var id: String { rawValue }

        var titleFR: String {
            switch self {
            case .before: return "Prière avant la lecture"
            case .after:  return "Prière après la lecture"
            }
        }

        var subtitleFR: String {
            switch self {
            case .before: return "Tehilim 95, versets 1 à 3"
            case .after:  return "Tehilim 14:7 et 37, versets 39 à 40"
            }
        }

        var symbol: String {
            switch self {
            case .before: return "play.circle"
            case .after:  return "checkmark.circle"
            }
        }
    }

    let kind: Kind
    let verseRefs: [PrayerVerseRef]
    var id: String { kind.id }

    /// Prière d'introduction — Tehilim 95, versets 1 à 3 (selon Beth Loubavitch / le-tehilim.online).
    static let before = Prayer(
        kind: .before,
        verseRefs: [
            .init(psalmId: 95, verseNumber: 1),
            .init(psalmId: 95, verseNumber: 2),
            .init(psalmId: 95, verseNumber: 3),
        ]
    )

    /// Prière de clôture — Tehilim 14:7 et 37:39-40 (selon Beth Loubavitch / le-tehilim.online).
    static let after = Prayer(
        kind: .after,
        verseRefs: [
            .init(psalmId: 14, verseNumber: 7),
            .init(psalmId: 37, verseNumber: 39),
            .init(psalmId: 37, verseNumber: 40),
        ]
    )

    static func of(_ kind: Kind) -> Prayer {
        kind == .before ? .before : .after
    }
}
