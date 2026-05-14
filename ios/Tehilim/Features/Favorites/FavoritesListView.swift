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
            } else if let selection {
                // V1.9.4 : iPad SplitView avec List native selection
                listWithSelection(selection: selection)
            } else {
                // iPhone : NavigationLink push standard
                listWithPush
            }
        }
    }

    // MARK: - iPad NavigationSplitView (List(items, selection:))

    @ViewBuilder
    private func listWithSelection(selection: Binding<Int?>) -> some View {
        List(selection: selection) {
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
                        favoriteLabel(p, isSelected: selection.wrappedValue == id)
                            .tag(id)
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

    // MARK: - iPhone (NavigationLink push)

    @ViewBuilder
    private var listWithPush: some View {
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
                            favoriteLabel(p, isSelected: false)
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

    // MARK: - Row

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
        .contentShape(Rectangle())
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Tehilim \(p.id)")
        .accessibilityHint("Ouvre le Tehilim")
        .accessibilityAddTraits(.isButton)
    }
}
