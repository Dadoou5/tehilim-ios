import SwiftUI

struct FavoritesListView: View {
    @EnvironmentObject private var container: AppContainer
    @EnvironmentObject private var favorites: FavoritesStore

    @State private var presentedPrayer: Prayer.Kind? = nil

    var body: some View {
        Group {
            if favorites.ids.isEmpty {
                EmptyStateView(
                    symbol: "heart",
                    title: "Aucun favori",
                    message: "Tape sur le cœur dans un Tehilim pour l'ajouter ici."
                )
            } else {
                List {
                    Section {
                        Button {
                            presentedPrayer = .before
                        } label: {
                            Label(Prayer.Kind.before.titleFR, systemImage: Prayer.Kind.before.symbol)
                        }
                    }

                    Section("Tehilim favoris") {
                        ForEach(favorites.sortedIds, id: \.self) { id in
                            if let p = container.psalmRepository.psalm(id: id) {
                                NavigationLink(destination: PsalmDetailView(psalmId: id, siblings: favorites.sortedIds)) {
                                    Text("Tehilim \(p.id) · \(p.hebrewNumber)")
                                }
                            }
                        }
                    }

                    Section {
                        Button {
                            presentedPrayer = .after
                        } label: {
                            Label(Prayer.Kind.after.titleFR, systemImage: Prayer.Kind.after.symbol)
                        }
                    }
                }
                .listStyle(.insetGrouped)
                .appBackground()
                .sheet(item: $presentedPrayer) { kind in
                    PrayerView(prayer: Prayer.of(kind))
                }
            }
        }
    }
}
