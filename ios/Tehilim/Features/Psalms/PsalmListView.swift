import SwiftUI

/// Liste plate des Tehilim — V1.9.5 : utilisée uniquement sur iPhone.
/// (Sur iPad, `IPadPsalmsSidebar` affiche directement la liste avec sélection native.)
struct PsalmListView: View {
    @EnvironmentObject private var container: AppContainer
    @EnvironmentObject private var favorites: FavoritesStore

    /// Si nil → liste complète.
    let book: Int?

    @State private var query: String = ""

    var body: some View {
        List(filteredPsalms) { psalm in
            NavigationLink(destination: PsalmDetailView(psalmId: psalm.id)) {
                psalmRow(psalm)
            }
        }
        .listStyle(.plain)
        .appBackground()
        .navigationTitle(book.map { "Livre \($0)" } ?? "Tous les Tehilim")
        .searchable(text: $query, placement: .navigationBarDrawer(displayMode: .always),
                    prompt: "Rechercher un Tehilim")
    }

    private var psalms: [Psalm] {
        if let book {
            return container.psalmRepository.psalms(inBook: book)
        }
        return container.psalmRepository.allPsalms
    }

    private var filteredPsalms: [Psalm] {
        guard !query.isEmpty else { return psalms }
        let q = query.lowercased()
        return psalms.filter {
            "\($0.id)".contains(q) ||
            $0.hebrewNumber.contains(query) ||
            ($0.hebrewTitle ?? "").contains(query)
        }
    }

    @ViewBuilder
    private func psalmRow(_ psalm: Psalm) -> some View {
        HStack(spacing: 12) {
            Text("\(psalm.id)")
                .font(.callout.weight(.semibold))
                .frame(width: 36, alignment: .leading)
            Text(psalm.hebrewNumber)
                .font(.callout)
                .foregroundStyle(.secondary)
                .frame(width: 36, alignment: .leading)
            VStack(alignment: .leading, spacing: 2) {
                Text("Tehilim \(psalm.id)").font(.headline)
                versesCountText(psalm)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            if favorites.contains(psalm.id) {
                Image(systemName: "heart.fill")
                    .foregroundStyle(Color.accentMain)
                    .accessibilityLabel("Favori")
            }
        }
    }

    @ViewBuilder
    private func versesCountText(_ psalm: Psalm) -> some View {
        if psalm.verses.count == 1 {
            Text("1 verset")
        } else {
            Text("\(psalm.verses.count) versets")
        }
    }
}
