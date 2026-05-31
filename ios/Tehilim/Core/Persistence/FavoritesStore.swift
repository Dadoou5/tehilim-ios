import Foundation
import Combine

/// Stocke les Tehilim mis en favori — V1.10.5 synchronise via iCloud KVS.
///
/// **Migration auto** : si l'utilisateur avait déjà des favoris dans le fichier
/// `favorites.json` local d'une version antérieure, ils sont rapatriés une fois
/// vers iCloud KVS au premier lancement V1.10.5+.
///
/// **Sync entre devices** : changement sur device A → notification iCloud →
/// les autres devices reçoivent l'update via `NSUbiquitousKeyValueStore.didChangeExternallyNotification`.
final class FavoritesStore: ObservableObject {
    @Published private(set) var ids: Set<Int> = []

    private let kvsKey = "favorites.ids"
    private let legacyURL: URL
    private var cancellable: AnyCancellable?

    init() {
        // Ancien emplacement (V1.0–V1.10.4) — utilisé pour la migration.
        let dir = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
            ?? FileManager.default.temporaryDirectory
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        self.legacyURL = dir.appendingPathComponent("favorites.json")

        migrateLegacyIfNeeded()
        loadFromCloud()
        observeCloudChanges()
    }

    func toggle(_ id: Int) {
        if ids.contains(id) { ids.remove(id) } else { ids.insert(id) }
        save()
    }

    /// Retire un favori (ex. swipe-to-delete depuis la liste). No-op si absent.
    func remove(_ id: Int) {
        guard ids.contains(id) else { return }
        ids.remove(id)
        save()
    }

    func contains(_ id: Int) -> Bool { ids.contains(id) }

    var sortedIds: [Int] { ids.sorted() }

    // MARK: - Migration

    private func migrateLegacyIfNeeded() {
        guard let data = try? Data(contentsOf: legacyURL),
              let legacyArray = try? JSONDecoder().decode([Int].self, from: data) else { return }
        iCloudKVS.shared.migrateIntArrayIfNeeded(key: kvsKey, legacyArray: legacyArray)
        // On garde le fichier legacy en place — il sert de cache offline
        // en cas de non-disponibilité d'iCloud + UserDefaults.
    }

    // MARK: - iCloud read/write

    private func loadFromCloud() {
        if let arr = iCloudKVS.shared.loadIntArray(forKey: kvsKey) {
            self.ids = Set(arr)
        }
    }

    private func save() {
        let arr = Array(ids).sorted()
        iCloudKVS.shared.saveIntArray(arr, forKey: kvsKey)
        // On maintient le fichier legacy à jour pour le widget et la rétrocompat.
        if let data = try? JSONEncoder().encode(arr) {
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
