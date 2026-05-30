import Foundation

struct LifeCase: Codable, Identifiable, Hashable {
    let id: String
    let title: String
    let symbol: String
    let note: String
    let psalms: [Int]
    /// Section logique pour l'affichage (optionnelle, fallback "Autres").
    let section: String?
    /// Traductions anglaises (optionnelles, fallback sur la version française).
    let titleEN: String?
    let noteEN: String?
    let sectionEN: String?
}

extension LifeCase {
    /// Renvoie le titre dans la langue active de l'OS.
    var localizedTitle: String {
        LifeCase.preferEnglish ? (titleEN ?? title) : title
    }

    /// Renvoie la note dans la langue active de l'OS.
    var localizedNote: String {
        LifeCase.preferEnglish ? (noteEN ?? note) : note
    }

    /// Section dans la langue active de l'OS.
    var localizedSection: String? {
        LifeCase.preferEnglish ? (sectionEN ?? section) : section
    }

    /// `true` si la langue active de l'app est l'anglais.
    ///
    /// V1.12 — délègue à `AppLocale` (source unique). Avant, le cas `.system`
    /// lisait `Locale.current` (instantané figé) → le contenu restait en
    /// anglais après une bascule à chaud vers Système, alors que l'UI était
    /// déjà repassée en français.
    static var preferEnglish: Bool {
        AppLocale.code == "en"
    }
}
