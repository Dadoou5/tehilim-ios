import SwiftUI

/// Liste des favoris — V1.9.5 : utilisée uniquement sur iPhone.
/// (Sur iPad, `IPadPsalmsSidebar` gère le mode favoris natif avec selection.)
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
                                // Swipe → « Retirer » : supprime le favori sans
                                // ouvrir le Tehilim. Le swipe-to-delete natif
                                // (onDelete) marche aussi via le bouton Modifier.
                                .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                                    Button(role: .destructive) {
                                        favorites.remove(id)
                                    } label: {
                                        Label("Retirer", systemImage: "heart.slash")
                                    }
                                }
                            }
                        }
                        .onDelete(perform: deleteFavorites)
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
                // Bouton « Modifier » : rend la suppression découvrable (les
                // nouveaux users ne pensent pas spontanément au swipe). Pattern
                // natif iOS, aligné sur l'écran « Mes prières ».
                .toolbar {
                    ToolbarItem(placement: .navigationBarTrailing) {
                        EditButton()
                    }
                }
                .sheet(item: $presentedPrayer) { kind in
                    PrayerView(prayer: Prayer.of(kind))
                }
            }
        }
    }

    /// Mappe les offsets de `ForEach(favorites.sortedIds)` vers les ids à
    /// retirer (le store n'est pas indexé par position).
    private func deleteFavorites(at offsets: IndexSet) {
        let ids = favorites.sortedIds
        for index in offsets where ids.indices.contains(index) {
            favorites.remove(ids[index])
        }
    }
}
