import Foundation
import Combine
import SwiftUI

/// Persistance des prières sauvegardées (Lelouy Nichmat) — V1.10.5 sync iCloud.
///
/// **Migration auto** : tout ancien fichier `saved_prayers.json` dans Documents
/// est migré vers iCloud KVS au premier lancement.
///
/// **Sync entre devices** : un Lelouy Nichmat créé sur iPhone apparaît sur iPad
/// (et inversement) via NSUbiquitousKeyValueStore.
///
/// **Limite Apple** : 1 MB total partagé pour toute l'app, large pour des dizaines
/// de prières (chaque SavedPrayerIntent fait ~300-500 octets en JSON).
final class SavedPrayerStore: ObservableObject {

    @Published private(set) var intents: [SavedPrayerIntent] = []

    private let kvsKey = "saved_prayers"
    private let legacyURL: URL
    private var cancellable: AnyCancellable?

    init(filename: String = "saved_prayers.json") {
        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first
            ?? URL(fileURLWithPath: NSTemporaryDirectory())
        self.legacyURL = docs.appendingPathComponent(filename)

        migrateLegacyIfNeeded()
        loadFromCloud()
        observeCloudChanges()
    }

    // MARK: - CRUD

    func add(_ intent: SavedPrayerIntent) {
        intents.insert(intent, at: 0)
        persist()
    }

    /// V1.10.5 : retourne une prière existante avec mêmes paramètres
    /// (prénom + lien + mère), ou nil si nouvelle. Évite les doublons en
    /// sauvegarde automatique.
    func findExisting(
        relativeFirstName: String,
        relationType: RelationType,
        motherFirstName: String
    ) -> SavedPrayerIntent? {
        intents.first {
            $0.relativeFirstName == relativeFirstName &&
            $0.relationType == relationType &&
            $0.motherFirstName == motherFirstName
        }
    }

    /// V1.10.5 : ajoute ou retourne l'existant. Garantit pas de doublon
    /// quand l'auto-save tape « Générer » plusieurs fois.
    func addOrFindExisting(_ intent: SavedPrayerIntent) -> SavedPrayerIntent {
        if let existing = findExisting(
            relativeFirstName: intent.relativeFirstName,
            relationType: intent.relationType,
            motherFirstName: intent.motherFirstName
        ) {
            return existing
        }
        add(intent)
        return intent
    }

    func delete(_ intent: SavedPrayerIntent) {
        intents.removeAll { $0.id == intent.id }
        persist()
    }

    func delete(at offsets: IndexSet) {
        intents.remove(atOffsets: offsets)
        persist()
    }

    func updateLastReadIndex(intentId: UUID, lastReadIndex: Int) {
        guard let i = intents.firstIndex(where: { $0.id == intentId }) else { return }
        intents[i].lastReadIndex = lastReadIndex
        persist()
    }

    func mostRecent(of type: PrayerType) -> SavedPrayerIntent? {
        intents.first { $0.prayerType == type }
    }

    func filtered(by type: PrayerType) -> [SavedPrayerIntent] {
        intents.filter { $0.prayerType == type }
    }

    // MARK: - Migration

    private func migrateLegacyIfNeeded() {
        guard FileManager.default.fileExists(atPath: legacyURL.path),
              let data = try? Data(contentsOf: legacyURL) else { return }
        iCloudKVS.shared.migrateIfNeeded(key: kvsKey, legacyData: data)
    }

    // MARK: - iCloud read/write

    private func loadFromCloud() {
        if let arr = iCloudKVS.shared.load([SavedPrayerIntent].self, forKey: kvsKey) {
            self.intents = arr
        }
    }

    private func persist() {
        iCloudKVS.shared.save(intents, forKey: kvsKey)
        // Cache local en miroir (rétrocompat + offline).
        if let data = try? JSONEncoder().encode(intents) {
            try? data.write(to: legacyURL, options: .atomic)
        }
    }

    private func observeCloudChanges() {
        cancellable = iCloudKVS.shared.externalChange.sink { [weak self] keys in
            guard let self, keys.contains(self.kvsKey) else { return }
            self.loadFromCloud()
        }
    }
}
