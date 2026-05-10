import Foundation

struct Psalm: Codable, Identifiable, Hashable {
    let id: Int                  // 1...150
    let book: Int                // 1...5
    let hebrewNumber: String
    let hebrewTitle: String?
    let verses: [Verse]
    let tags: [String]
}

struct Verse: Codable, Identifiable, Hashable {
    let id: String               // "23:1"
    let number: Int
    let hebrewNumber: String
    let hebrew: String
    let translationFR: String?
    let translationEN: String?

    /// Renvoie la traduction selon la langue choisie.
    func translation(for lang: TranslationLanguage) -> String? {
        switch lang {
        case .fr: return translationFR
        case .en: return translationEN
        }
    }
}

extension Psalm {
    static let bookRanges: [Int: ClosedRange<Int>] = [
        1: 1...41,
        2: 42...72,
        3: 73...89,
        4: 90...106,
        5: 107...150
    ]

    static func book(forPsalmId id: Int) -> Int {
        for (book, range) in bookRanges where range.contains(id) { return book }
        return 1
    }
}

/// Langue de la traduction du texte des Tehilim (séparée de la langue UI).
enum TranslationLanguage: String, Codable, CaseIterable, Identifiable {
    case fr = "fr"
    case en = "en"

    var id: String { rawValue }

    var label: String {
        switch self {
        case .fr: return "Français"
        case .en: return "English"
        }
    }

    var nativeLabel: String {
        switch self {
        case .fr: return "Français"
        case .en: return "English"
        }
    }

    var sourceCredit: String {
        switch self {
        case .fr: return "Beth Loubavitch — le-tehilim.online"
        case .en: return "Sefaria — JPS 1917 (domaine public)"
        }
    }
}
