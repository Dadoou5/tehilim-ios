import Foundation

/// Snapshot local d'une chaîne (compte rendu final), conservé par le **créateur**
/// même après la suppression cloud (TTL). Permet de garder l'historique.
struct ChainArchiveSnapshot: Codable, Identifiable, Equatable {
    let id: String
    let name: String
    let intentionRaw: String
    let detail: String
    let creatorName: String
    let readingDeadline: Date
    let archivedAt: Date
    /// psalmId ("1".."150") → nom du lecteur.
    let assignments: [String: String]

    var intention: ChainIntention { ChainIntention(rawValue: intentionRaw) ?? .reussite }
    var subjectLine: String {
        let d = detail.trimmingCharacters(in: .whitespacesAndNewlines)
        return d.isEmpty ? name : "\(name) — \(d)"
    }
}

/// Persiste localement : (1) les ids de chaînes connues (créées/rejointes) pour
/// alimenter « Mes chaînes » sans requête globale ; (2) les archives créateur.
final class ChainArchiveStore: ObservableObject {
    @Published private(set) var knownChainIds: [String] = []
    @Published private(set) var archives: [ChainArchiveSnapshot] = []

    private let defaults: UserDefaults
    private let knownKey = "chains.known.ids"
    private let archivesKey = "chains.archives"

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        if let ids = defaults.array(forKey: knownKey) as? [String] {
            knownChainIds = ids
        }
        if let data = defaults.data(forKey: archivesKey),
           let decoded = try? JSONDecoder().decode([ChainArchiveSnapshot].self, from: data) {
            archives = decoded
        }
    }

    /// Mémorise une chaîne (créée ou rejointe) — en tête, sans doublon.
    func remember(_ chainId: String) {
        guard !chainId.isEmpty else { return }
        knownChainIds.removeAll { $0 == chainId }
        knownChainIds.insert(chainId, at: 0)
        defaults.set(knownChainIds, forKey: knownKey)
    }

    func forget(_ chainId: String) {
        knownChainIds.removeAll { $0 == chainId }
        defaults.set(knownChainIds, forKey: knownKey)
    }

    /// Ajoute (ou remplace) une archive de compte rendu.
    func saveArchive(_ snapshot: ChainArchiveSnapshot) {
        archives.removeAll { $0.id == snapshot.id }
        archives.insert(snapshot, at: 0)
        persistArchives()
    }

    func deleteArchive(_ id: String) {
        archives.removeAll { $0.id == id }
        persistArchives()
    }

    private func persistArchives() {
        if let data = try? JSONEncoder().encode(archives) {
            defaults.set(data, forKey: archivesKey)
        }
    }
}
