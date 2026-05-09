import SwiftUI

struct SearchView: View {
    @EnvironmentObject private var container: AppContainer
    @Environment(\.dismiss) private var dismiss

    @State private var query: String = ""
    @State private var result: SearchQueryResult?
    @State private var recents: [Int] = []

    var body: some View {
        NavigationStack {
            List {
                if let result {
                    if let exact = result.exactMatch {
                        Section("Résultat") {
                            NavigationLink(destination: PsalmDetailView(psalmId: exact.id).onAppear { remember(exact.id) }) {
                                resultRow(exact, primary: true)
                            }
                        }
                    } else if !query.isEmpty {
                        Section {
                            EmptyStateView(
                                symbol: "magnifyingglass",
                                title: "Aucun Tehilim trouvé",
                                message: "Essaie un numéro entre 1 et 150, ou les lettres hébraïques (ex. כג)."
                            )
                            .listRowBackground(Color.clear)
                        }
                    }

                    if !result.suggestions.isEmpty {
                        Section("Suggestions") {
                            ForEach(result.suggestions) { p in
                                NavigationLink(destination: PsalmDetailView(psalmId: p.id).onAppear { remember(p.id) }) {
                                    resultRow(p, primary: false)
                                }
                            }
                        }
                    }
                }

                if !recents.isEmpty {
                    Section("Récents") {
                        ForEach(recents, id: \.self) { id in
                            if let p = container.psalmRepository.psalm(id: id) {
                                NavigationLink(destination: PsalmDetailView(psalmId: p.id).onAppear { remember(p.id) }) {
                                    resultRow(p, primary: false)
                                }
                            }
                        }
                    }
                }
            }
            .listStyle(.insetGrouped)
            .appBackground()
            .navigationTitle("Rechercher")
            .navigationBarTitleDisplayMode(.inline)
            .searchable(text: $query, placement: .navigationBarDrawer(displayMode: .always),
                        prompt: "23, כג, tehilim 23")
            .onChange(of: query) { _, _ in compute() }
            .onAppear { compute(); recents = loadRecents() }
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Annuler") { dismiss() }
                }
            }
        }
    }

    private func compute() {
        result = container.searchInterpreter.interpret(query)
    }

    private func resultRow(_ psalm: Psalm, primary: Bool) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text("Tehilim \(psalm.id) · \(psalm.hebrewNumber)")
                .font(primary ? .headline : .subheadline)
            if let title = psalm.hebrewTitle {
                Text(title).font(.caption).foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 4)
    }

    // MARK: - Recents

    private func loadRecents() -> [Int] {
        UserDefaults.standard.array(forKey: "search.recents") as? [Int] ?? []
    }
    private func remember(_ id: Int) {
        var arr = loadRecents()
        arr.removeAll { $0 == id }
        arr.insert(id, at: 0)
        if arr.count > 10 { arr = Array(arr.prefix(10)) }
        UserDefaults.standard.set(arr, forKey: "search.recents")
        recents = arr
    }
}
