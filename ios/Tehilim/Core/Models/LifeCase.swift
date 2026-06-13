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
    /// Traductions hébraïques (optionnelles, fallback EN puis FR).
    let titleHE: String?
    let noteHE: String?
    let sectionHE: String?
}

extension LifeCase {
    /// Renvoie le titre dans la langue active de l'app.
    var localizedTitle: String {
        LifeCase.pick(fr: title, en: titleEN, he: titleHE)
    }

    /// Renvoie la note dans la langue active de l'app.
    var localizedNote: String {
        LifeCase.pick(fr: note, en: noteEN, he: noteHE)
    }

    /// Section dans la langue active de l'app.
    var localizedSection: String? {
        switch LifeCase.contentCode {
        case "he": return sectionHE ?? sectionEN ?? section
        case "en": return sectionEN ?? section
        default:   return section
        }
    }

    /// Code de langue du contenu éditorial : "fr" | "en" | "he".
    ///
    /// V1.12 — délègue à `AppLocale` (source unique). Avant, le cas `.system`
    /// lisait `Locale.current` (instantané figé) → le contenu restait en
    /// anglais après une bascule à chaud vers Système.
    /// V2.2.b — l'hébreu a désormais son propre contenu (titleHE/noteHE…),
    /// avec repli sur EN puis FR si une clé manque.
    static var contentCode: String { AppLocale.code }

    /// Choisit la chaîne selon la langue active, avec repli HE → EN → FR.
    static func pick(fr: String, en: String?, he: String?) -> String {
        switch contentCode {
        case "he": return he ?? en ?? fr
        case "en": return en ?? fr
        default:   return fr
        }
    }
}
