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
                        ForEach(sortedIntents) { intent in
                            NavigationLink(destination: PersonalizedReadingListView(intent: intent, isSaved: true)) {
                                row(intent)
                            }
                        }
                        .onDelete(perform: deleteFromSorted)
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

            // V1.10.7 — Prochaine azcara si la date du décès est renseignée.
            // Calcul fait par MemorialCalculator (règles traditionnelles).
            if let death = intent.civilDateOfDeath,
               let next = MemorialCalculator.nextYahrzeit(deathCivil: death) {
                HStack(spacing: 6) {
                    Image(systemName: "flame.fill")
                        .font(.caption2)
                    Text("Prochaine azcara")
                    Text(":")
                    Text(next.formatted(date: .abbreviated, time: .omitted))
                        .fontWeight(.medium)
                }
                .font(.caption)
                .foregroundStyle(Color.accentMain)
            }
        }
        .padding(.vertical, 4)
        .accessibilityElement(children: .combine)
    }

    private func delete(at offsets: IndexSet) {
        savedPrayers.delete(at: offsets)
    }

    /// V1.10.7 — Mes prières triées par prochaine azcara croissante
    /// (les commémorations à venir en premier), puis par date de création
    /// décroissante pour les prières sans date du décès renseignée.
    private var sortedIntents: [SavedPrayerIntent] {
        let now = Date()
        let withAzcara = savedPrayers.intents.compactMap { intent -> (SavedPrayerIntent, Date)? in
            guard let death = intent.civilDateOfDeath,
                  let next = MemorialCalculator.nextYahrzeit(deathCivil: death, now: now)
            else { return nil }
            return (intent, next)
        }
        .sorted { lhs, rhs in
            if lhs.1 != rhs.1 { return lhs.1 < rhs.1 }
            // Tiebreaker : créé en dernier en premier (= comportement
            // historique pour les prières insérées le même jour avec
            // la même date du décès).
            return lhs.0.createdAt > rhs.0.createdAt
        }
        .map { $0.0 }

        let withoutAzcara = savedPrayers.intents
            .filter { $0.civilDateOfDeath == nil }
            .sorted { $0.createdAt > $1.createdAt }

        return withAzcara + withoutAzcara
    }

    /// Mappe les offsets du `ForEach(sortedIntents)` vers les indices du
    /// store sous-jacent (qui peut être dans un ordre différent).
    private func deleteFromSorted(at offsets: IndexSet) {
        let toDelete = offsets.map { sortedIntents[$0] }
        for intent in toDelete {
            savedPrayers.delete(intent)
        }
    }
}
