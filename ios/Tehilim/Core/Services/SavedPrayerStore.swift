import Foundation
import SwiftUI

/// Persistance des prières sauvegardées (Refoua Cheléma / Lelouy Nichmat).
///
/// Stockage simple en JSON dans le dossier `Documents` de l'app, comme `FavoritesStore`.
/// Le format est versionné par l'enum `SavedPrayerIntent` (Codable) ; toute évolution
/// future devra prévoir une migration.
final class SavedPrayerStore: ObservableObject {

    @Published private(set) var intents: [SavedPrayerIntent] = []

    private let fileURL: URL

    init(filename: String = "saved_prayers.json") {
        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first
            ?? URL(fileURLWithPath: NSTemporaryDirectory())
        self.fileURL = docs.appendingPathComponent(filename)
        load()
    }

    // MARK: - CRUD

    /// Ajoute une nouvelle prière en haut de la liste.
    func add(_ intent: SavedPrayerIntent) {
        intents.insert(intent, at: 0)
        persist()
    }

    /// Supprime une prière.
    func delete(_ intent: SavedPrayerIntent) {
        intents.removeAll { $0.id == intent.id }
        persist()
    }

    /// Supprime à l'index donné (utile pour `.onDelete` dans SwiftUI List).
    func delete(at offsets: IndexSet) {
        intents.remove(atOffsets: offsets)
        persist()
    }

    /// Met à jour la position de lecture pour une prière donnée.
    func updateLastReadIndex(intentId: UUID, lastReadIndex: Int) {
        guard let i = intents.firstIndex(where: { $0.id == intentId }) else { return }
        intents[i].lastReadIndex = lastReadIndex
        persist()
    }

    /// Retourne la prière la plus récente du type donné, ou nil.
    func mostRecent(of type: PrayerType) -> SavedPrayerIntent? {
        intents.first { $0.prayerType == type }
    }

    /// Filtré par type (utile pour la vue Saved séparée Refoua / Lelouy).
    func filtered(by type: PrayerType) -> [SavedPrayerIntent] {
        intents.filter { $0.prayerType == type }
    }

    // MARK: - I/O

    private func persist() {
        do {
            let data = try JSONEncoder().encode(intents)
            try data.write(to: fileURL, options: .atomic)
        } catch {
            // Log seulement — pas critique de bloquer l'UI.
            print("SavedPrayerStore: persist failed: \(error)")
        }
    }

    private func load() {
        guard FileManager.default.fileExists(atPath: fileURL.path) else { return }
        do {
            let data = try Data(contentsOf: fileURL)
            self.intents = try JSONDecoder().decode([SavedPrayerIntent].self, from: data)
        } catch {
            print("SavedPrayerStore: load failed: \(error)")
        }
    }
}
