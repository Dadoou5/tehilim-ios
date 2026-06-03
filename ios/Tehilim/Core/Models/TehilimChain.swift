import Foundation

/// Modèles de la feature « Chaîne de Tehilim » (lecture collective temps réel).
///
/// Volontairement **sans dépendance backend** : ce sont de simples valeurs
/// Codable. Le mapping vers Supabase (Postgres timestamptz ↔ Date, colonnes
/// snake_case) est fait dans `ChainService`. Les modèles compilent sans le SDK.

/// Type d'intention d'une chaîne.
enum ChainIntention: String, Codable, CaseIterable, Identifiable {
    case lelouy     // Lelouy Nichmat (à la mémoire d'un défunt)
    case refoua     // Refoua Chelema (guérison)
    case reussite   // Réussite / protection (un proche, Am Israël…)

    var id: String { rawValue }

    /// Libellé court (titre) — localisé via la clé éponyme.
    var titleKey: String {
        switch self {
        case .lelouy:   return "Lelouy Nichmat"
        case .refoua:   return "Refoua Chelema"
        case .reussite: return "Pour la réussite de"
        }
    }

    var symbol: String {
        switch self {
        case .lelouy:   return "flame.fill"
        case .refoua:   return "heart.fill"
        case .reussite: return "star.fill"
        }
    }
}

/// Les 5 livres des Tehilim — repères de section dans la grille.
enum TehilimBook: Int, CaseIterable, Identifiable {
    case one = 1, two, three, four, five
    var id: Int { rawValue }
    var range: ClosedRange<Int> {
        switch self {
        case .one:   return 1...41
        case .two:   return 42...72
        case .three: return 73...89
        case .four:  return 90...106
        case .five:  return 107...150
        }
    }
    /// Libellé (clé = texte FR, traduite via .lproj).
    var titleKey: String {
        switch self {
        case .one:   return "Livre I"
        case .two:   return "Livre II"
        case .three: return "Livre III"
        case .four:  return "Livre IV"
        case .five:  return "Livre V"
        }
    }
    static func book(for psalm: Int) -> TehilimBook {
        allCases.first { $0.range.contains(psalm) } ?? .one
    }
}

/// Phase courante d'une chaîne, dérivée du temps + du flag `distributed`.
enum ChainPhase {
    case selecting   // sélection ouverte
    case locked      // lecture seule (sélection close ou distribuée)
}

/// Une chaîne de Tehilim (document `chains/{id}`).
struct TehilimChain: Identifiable, Codable, Equatable {
    /// Nombre total de Tehilim d'une chaîne — le livre entier.
    static let totalPsalms = 150

    /// Compresse une liste de numéros en **plages** lisibles pour le compte
    /// rendu : `[1,2,3,5,8,9]` → « 1 à 3, 5, 8 à 9 ». `separator` = « à »/« to ».
    static func compressRanges(_ ids: [Int], separator: String) -> String {
        let s = ids.sorted()
        guard let first = s.first else { return "" }
        var parts: [String] = []
        var start = first, prev = first
        func flush() { parts.append(start == prev ? "\(start)" : "\(start) \(separator) \(prev)") }
        for n in s.dropFirst() {
            if n == prev + 1 { prev = n } else { flush(); start = n; prev = n }
        }
        flush()
        return parts.joined(separator: ", ")
    }

    let id: String
    var name: String
    var intentionType: ChainIntention
    var intentionDetail: String
    let creatorUid: String
    var creatorName: String
    let createdAt: Date
    var selectionDeadline: Date
    var readingDeadline: Date
    var distributed: Bool
    /// Fin de lecture + marge → champ `expires_at` (purge cloud par le cron).
    var expiresAt: Date

    /// Sujet lisible : « Lelouy Nichmat — David » / « Refoua Chelema — … ».
    var subjectLine: String {
        let detail = intentionDetail.trimmingCharacters(in: .whitespacesAndNewlines)
        return detail.isEmpty ? name : "\(name) — \(detail)"
    }

    func phase(now: Date = Date()) -> ChainPhase {
        if distributed { return .locked }
        return now < selectionDeadline ? .selecting : .locked
    }

    /// Sélection encore modifiable (fenêtre ouverte ET non distribuée).
    func isSelectionOpen(now: Date = Date()) -> Bool {
        phase(now: now) == .selecting
    }
}

/// Un participant (document `chains/{id}/participants/{uid}`).
struct ChainParticipant: Identifiable, Codable, Equatable {
    /// `id` = uid anonyme Supabase (auth).
    let id: String
    var name: String
    var isCreator: Bool
    let joinedAt: Date
}

/// L'attribution d'un Tehilim (document `chains/{id}/assignments/{psalmId}`).
/// `id` = numéro du Tehilim sous forme de chaîne ("1".."150").
struct ChainAssignment: Identifiable, Codable, Equatable {
    let id: String           // psalmId "1".."150"
    let uid: String          // qui s'est engagé
    var name: String         // dénormalisé pour affichage du nom à côté
    var byCreator: Bool      // attribué d'office par le créateur
    let assignedAt: Date

    var psalmId: Int { Int(id) ?? 0 }
}
