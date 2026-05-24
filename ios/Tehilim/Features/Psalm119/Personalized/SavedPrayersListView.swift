import SwiftUI

/// Liste des Lelouy Nichmat sauvegardés — V1.10.2 (section unique).
///
/// Note compatibilité : les anciennes entrées de type `.malade` (V1.10.0/1) sont
/// affichées si elles existent, sans rupture. Le store n'efface jamais les données.
struct SavedPrayersListView: View {
    @EnvironmentObject private var savedPrayers: SavedPrayerStore

    var body: some View {
        Group {
            if savedPrayers.intents.isEmpty {
                EmptyStateView(
                    symbol: "tray",
                    title: "Aucun Lelouy Nichmat sauvegardé",
                    message: "Génère une lecture personnalisée et sauvegarde-la pour la retrouver ici."
                )
            } else {
                List {
                    Section {
                        ForEach(savedPrayers.intents) { intent in
                            NavigationLink(destination: PersonalizedReadingListView(intent: intent, isSaved: true)) {
                                row(intent)
                            }
                        }
                        .onDelete(perform: delete)
                    } header: {
                        Text("Lelouy Nichmat")
                    } footer: {
                        // V1.10.7 — pas de conditionnel `s` dans
                        // l'interpolation (casserait la clé de lookup
                        // LocalizedStringKey). On choisit la clé selon
                        // le count, chaque clé existe en EN.
                        let count = savedPrayers.intents.count
                        Group {
                            if count <= 1 {
                                Text("\(count) sauvegardé")
                            } else {
                                Text("\(count) sauvegardés")
                            }
                        }
                        .font(.caption)
                        .foregroundStyle(.tertiary)
                    }
                }
                .listStyle(.insetGrouped)
                .appBackground()
            }
        }
        .navigationTitle("Mes prières")
        .navigationBarTitleDisplayMode(.large)
    }

    @ViewBuilder
    private func row(_ intent: SavedPrayerIntent) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(intent.hebrewSubject)
                .font(.headline)
                .environment(\.layoutDirection, .rightToLeft)
                .frame(maxWidth: .infinity, alignment: .trailing)
            HStack(spacing: 8) {
                Text("\(intent.generatedLetters.count) lettres")
                Text("·")
                Text(intent.createdAt.formatted(date: .abbreviated, time: .omitted))
                if let last = intent.lastReadIndex {
                    Text("·")
                    Text("lue jusqu'à \(last + 1)")
                        .foregroundStyle(Color.accentMain)
                }
            }
            .font(.caption)
            .foregroundStyle(.secondary)
        }
        .padding(.vertical, 4)
        .accessibilityElement(children: .combine)
    }

    private func delete(at offsets: IndexSet) {
        savedPrayers.delete(at: offsets)
    }
}
