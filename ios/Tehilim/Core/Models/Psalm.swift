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

    /// Renvoie la traduction selon la préférence d'app (résout .system).
    func translation(for app: AppLanguage) -> String? {
        translation(for: app.translation)
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

/// Langue effective utilisée pour la traduction des Tehilim (résultat concret : FR ou EN).
enum TranslationLanguage: String, Codable, Hashable {
    case fr = "fr"
    case en = "en"

    var sourceCredit: String {
        switch self {
        case .fr: return "Beth Loubavitch — le-tehilim.online"
        case .en: return "Sefaria — JPS 1917 (domaine public)"
        }
    }
}

/// Choix utilisateur — pilote à la fois l'UI (via AppleLanguages) et la traduction des Tehilim.
enum AppLanguage: String, Codable, CaseIterable, Identifiable {
    case system
    case fr
    case en

    var id: String { rawValue }

    /// Code BCP-47 actif (résout `.system` à la langue iOS courante, fallback "fr").
    var activeCode: String {
        switch self {
        case .system:
            let pref = Locale.preferredLanguages.first ?? "fr"
            return String(pref.split(separator: "-").first ?? "fr")
        case .fr: return "fr"
        case .en: return "en"
        }
    }

    /// Langue effective pour le texte de traduction des Tehilim.
    var translation: TranslationLanguage {
        activeCode == "en" ? .en : .fr
    }

    /// Code à inscrire dans `AppleLanguages`. `nil` pour `.system` (laisse iOS choisir).
    var appleLanguagesCode: String? {
        self == .system ? nil : activeCode
    }
}
