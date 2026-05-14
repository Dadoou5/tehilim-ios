import SwiftUI

struct FavoritesListView: View {
    @EnvironmentObject private var container: AppContainer
    @EnvironmentObject private var favorites: FavoritesStore

    /// Optionnel : binding de sélection pour le mode NavigationSplitView (iPad).
    var selection: Binding<Int?>? = nil

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
                            favoriteRow(id: id)
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

    @ViewBuilder
    private func favoriteRow(id: Int) -> some View {
        if let p = container.psalmRepository.psalm(id: id) {
            if let selection {
                Button {
                    selection.wrappedValue = id
                } label: {
                    favoriteLabel(p, isSelected: selection.wrappedValue == id)
                }
                .buttonStyle(.plain)
                .listRowBackground(
                    selection.wrappedValue == id
                    ? Color.accentMain.opacity(0.12)
                    : nil
                )
            } else {
                NavigationLink(destination: PsalmDetailView(psalmId: id, siblings: favorites.sortedIds)) {
                    favoriteLabel(p, isSelected: false)
                }
            }
        }
    }

    @ViewBuilder
    private func favoriteLabel(_ p: Psalm, isSelected: Bool) -> some View {
        HStack {
            Text("Tehilim \(p.id) · \(p.hebrewNumber)")
                .foregroundStyle(isSelected ? Color.accentMain : .primary)
            Spacer()
            if isSelected {
                Image(systemName: "checkmark")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Color.accentMain)
            }
        }
    }
}
