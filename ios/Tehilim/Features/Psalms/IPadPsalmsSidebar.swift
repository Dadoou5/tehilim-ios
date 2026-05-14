import SwiftUI

/// Sidebar iPad pour l'onglet Tehilim — V1.9.5.
///
/// **Pourquoi cette vue existe** :
/// Les tentatives précédentes (V1.9.0 → V1.9.4) utilisaient un push depuis
/// `BookListView` vers `PsalmListView` à l'intérieur de la sidebar du
/// `NavigationSplitView`. Le binding de sélection ne propageait pas de
/// façon fiable vers la `detail column` après ce push.
///
/// **Décision UX** : éliminer la double-navigation sur iPad.
/// - 1 seul niveau de liste dans la sidebar
/// - 150 Tehilim affichés directement, groupés en sections selon le mode
/// - `List(selection:)` à la racine → SwiftUI route la sélection correctement
struct IPadPsalmsSidebar: View {
    @EnvironmentObject private var container: AppContainer
    @EnvironmentObject private var favorites: FavoritesStore
    @EnvironmentObject private var router: TabRouter

    /// Binding vers la sélection portée par `PsalmsTabView`.
    @Binding var selectedPsalmId: Int?

    @State private var query: String = ""
    @State private var presentedPrayer: Prayer.Kind? = nil

    var body: some View {
        VStack(spacing: 0) {
            Picker("", selection: $router.psalmsSegment) {
                Text("Livres").tag(0)
                Text("Tous").tag(1)
                Text("Favoris").tag(2)
            }
            .pickerStyle(.segmented)
            .padding(.horizontal, 16)
            .padding(.vertical, 8)

            list
        }
        .navigationTitle("Tehilim")
        .searchable(
            text: $query,
            placement: .navigationBarDrawer(displayMode: .always),
            prompt: "Rechercher un Tehilim"
        )
        .sheet(item: $presentedPrayer) { kind in
            PrayerView(prayer: Prayer.of(kind))
        }
    }

    // MARK: - Liste adaptative

    @ViewBuilder
    private var list: some View {
        switch router.psalmsSegment {
        case 0: byBookList
        case 1: flatList
        default: favoritesList
        }
    }

    // MARK: - Mode Livres (sections)

    @ViewBuilder
    private var byBookList: some View {
        List(selection: $selectedPsalmId) {
            ForEach(1...5, id: \.self) { book in
                let psalmsInBook = filteredPsalms(inBook: book)
                if !psalmsInBook.isEmpty {
                    Section {
                        ForEach(psalmsInBook) { psalm in
                            psalmRow(psalm)
                                .tag(psalm.id)
                        }
                    } header: {
                        bookHeader(book)
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .appBackground()
    }

    // MARK: - Mode Tous (liste plate)

    @ViewBuilder
    private var flatList: some View {
        List(filteredAllPsalms, selection: $selectedPsalmId) { psalm in
            psalmRow(psalm)
                .tag(psalm.id)
        }
        .listStyle(.plain)
        .appBackground()
    }

    // MARK: - Mode Favoris

    @ViewBuilder
    private var favoritesList: some View {
        if favorites.ids.isEmpty {
            EmptyStateView(
                symbol: "heart",
                title: "Aucun favori",
                message: "Tape sur le cœur dans un Tehilim pour l'ajouter ici."
            )
        } else {
            List(selection: $selectedPsalmId) {
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
                            psalmRow(p)
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
        }
    }

    // MARK: - Composants

    @ViewBuilder
    private func bookHeader(_ book: Int) -> some View {
        HStack {
            Text("Livre \(book)")
                .font(.subheadline.weight(.semibold))
            Spacer()
            Text("\(psalmCount(book)) Tehilim · \(rangeLabel(book))")
                .font(.caption2)
                .foregroundStyle(.tertiary)
                .textCase(.none)
        }
    }

    @ViewBuilder
    private func psalmRow(_ psalm: Psalm) -> some View {
        HStack(spacing: 10) {
            Text("\(psalm.id)")
                .font(.callout.weight(.semibold))
                .frame(width: 32, alignment: .leading)
            Text(psalm.hebrewNumber)
                .font(.callout)
                .foregroundStyle(.secondary)
                .frame(width: 32, alignment: .leading)
            VStack(alignment: .leading, spacing: 1) {
                Text("Tehilim \(psalm.id)").font(.subheadline.weight(.medium))
                Text(psalm.verses.count == 1 ? "1 verset" : "\(psalm.verses.count) versets")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            if favorites.contains(psalm.id) {
                Image(systemName: "heart.fill")
                    .font(.caption)
                    .foregroundStyle(Color.accentMain)
                    .accessibilityLabel("Favori")
            }
        }
        .padding(.vertical, 2)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(
            "Tehilim \(psalm.id), \(psalm.verses.count) versets" +
            (favorites.contains(psalm.id) ? ", favori" : "")
        )
        .accessibilityHint("Ouvre le Tehilim")
    }

    // MARK: - Helpers

    private func filteredPsalms(inBook book: Int) -> [Psalm] {
        let all = container.psalmRepository.psalms(inBook: book)
        return applyFilter(to: all)
    }

    private var filteredAllPsalms: [Psalm] {
        applyFilter(to: container.psalmRepository.allPsalms)
    }

    private func applyFilter(to psalms: [Psalm]) -> [Psalm] {
        guard !query.isEmpty else { return psalms }
        let q = query.lowercased()
        return psalms.filter {
            "\($0.id)".contains(q) ||
            $0.hebrewNumber.contains(query) ||
            ($0.hebrewTitle ?? "").contains(query)
        }
    }

    private func rangeLabel(_ book: Int) -> String {
        guard let r = Psalm.bookRanges[book] else { return "" }
        return "\(r.lowerBound)–\(r.upperBound)"
    }

    private func psalmCount(_ book: Int) -> Int {
        guard let r = Psalm.bookRanges[book] else { return 0 }
        return r.upperBound - r.lowerBound + 1
    }
}
