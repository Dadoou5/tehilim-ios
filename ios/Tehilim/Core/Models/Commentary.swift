import Foundation

/// Un commentaire classique sur un verset (V2.4 — mode étude).
/// Source : Sefaria (Rashi CC-BY ; Metzudat David, domaine public).
struct VerseCommentary: Identifiable, Hashable {
    enum Kind: String, Codable, CaseIterable {
        case rashi
        case metzudat

        /// Nom affiché en hébreu (toujours, c'est le nom canonique du commentateur).
        var hebrewName: String {
            switch self {
            case .rashi:    return "רש״י"
            case .metzudat: return "מצודת דוד"
            }
        }
        /// Nom translittéré (libellé secondaire pour les locales latines).
        var latinName: String {
            switch self {
            case .rashi:    return "Rachi"
            case .metzudat: return "Metsoudat David"
            }
        }
    }

    let kind: Kind
    /// Dibour hamatchil (mots du verset commentés), en gras. Optionnel.
    let lemma: String?
    /// Texte du commentaire en hébreu.
    let he: String
    /// Traduction anglaise (Rashi/Rosenberg uniquement, sinon nil).
    let en: String?
    /// Traduction française (Rashi, générée depuis l'anglais ; sinon nil).
    let fr: String?

    var id: String { "\(kind.rawValue)|\(lemma ?? "")|\(he.prefix(16))" }

    /// Texte à afficher selon le code langue de l'app, avec repli FR→EN→HE / EN→HE.
    func text(for code: String) -> String {
        switch code {
        case "fr": return fr ?? en ?? he
        case "en": return en ?? he
        default:   return he
        }
    }
}

/// Index rapide des commentaires : « psaume:verset » → commentaires ordonnés
/// (Rashi puis Metzudat David).
final class CommentaryRepository {
    private let byKey: [String: [VerseCommentary]]

    init(byKey: [String: [VerseCommentary]] = [:]) { self.byKey = byKey }

    func comments(psalmId: Int, verse: Int) -> [VerseCommentary] {
        byKey["\(psalmId):\(verse)"] ?? []
    }

    var isAvailable: Bool { !byKey.isEmpty }
}
