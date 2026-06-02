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
    /// Temps de lecture **approximatif** en minutes, estimé à partir du nombre
    /// de mots hébreux (~110 mots/min, lecture dévotionnelle vocalisée).
    /// Minimum 1 min. Indication, pas une mesure exacte.
    var estimatedReadingMinutes: Int {
        let words = verses.reduce(0) { acc, v in
            acc + v.hebrew.split(whereSeparator: { $0.isWhitespace }).count
        }
        return max(1, Int((Double(words) / 110.0).rounded()))
    }

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

    /// Code BCP-47 actif. Pour `.system`, suit la langue de l'appareil si
    /// c'est le français ou l'anglais ; pour toute autre langue système
    /// (l'app n'est traduite qu'en fr/en), l'**anglais** est la valeur par
    /// défaut (et non le français).
    var activeCode: String {
        switch self {
        case .system:
            let pref = Locale.preferredLanguages.first ?? "en"
            let code = String(pref.split(separator: "-").first ?? "en")
            return (code == "fr" || code == "en") ? code : "en"
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
