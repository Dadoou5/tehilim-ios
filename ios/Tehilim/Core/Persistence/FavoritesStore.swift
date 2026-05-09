import Foundation

final class FavoritesStore: ObservableObject {
    @Published private(set) var ids: Set<Int> = []

    private let url: URL

    init() {
        let dir = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
            ?? FileManager.default.temporaryDirectory
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        self.url = dir.appendingPathComponent("favorites.json")
        load()
    }

    func toggle(_ id: Int) {
        if ids.contains(id) { ids.remove(id) } else { ids.insert(id) }
        save()
    }

    func contains(_ id: Int) -> Bool { ids.contains(id) }

    var sortedIds: [Int] { ids.sorted() }

    // MARK: - Persistence

    private func load() {
        guard let data = try? Data(contentsOf: url),
              let arr = try? JSONDecoder().decode([Int].self, from: data) else { return }
        ids = Set(arr)
    }

    private func save() {
        let arr = Array(ids).sorted()
        if let data = try? JSONEncoder().encode(arr) {
            try? data.write(to: url, options: .atomic)
        }
    }
}
