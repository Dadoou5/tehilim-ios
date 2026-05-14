import SwiftUI

/// Liste des prières sauvegardées — séparées en 2 sections : Refoua Cheléma / Lelouy Nichmat.
struct SavedPrayersListView: View {
    @EnvironmentObject private var savedPrayers: SavedPrayerStore

    var body: some View {
        Group {
            if savedPrayers.intents.isEmpty {
                EmptyStateView(
                    symbol: "tray",
                    title: "Aucune prière sauvegardée",
                    message: "Génère une lecture personnalisée et sauvegarde-la pour la retrouver ici."
                )
            } else {
                List {
                    section(title: "Refoua Cheléma", type: .malade)
                    section(title: "Lelouy Nichmat", type: .defunt)
                }
                .listStyle(.insetGrouped)
                .appBackground()
            }
        }
        .navigationTitle("Mes prières")
        .navigationBarTitleDisplayMode(.large)
    }

    @ViewBuilder
    private func section(title: String, type: PrayerType) -> some View {
        let filtered = savedPrayers.filtered(by: type)
        if !filtered.isEmpty {
            Section(title) {
                ForEach(filtered) { intent in
                    NavigationLink(destination: PersonalizedReadingListView(intent: intent, isSaved: true)) {
                        row(intent)
                    }
                }
                .onDelete { offsets in
                    delete(filtered: filtered, at: offsets)
                }
            }
        }
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

    private func delete(filtered: [SavedPrayerIntent], at offsets: IndexSet) {
        for index in offsets {
            let intent = filtered[index]
            savedPrayers.delete(intent)
        }
    }
}
