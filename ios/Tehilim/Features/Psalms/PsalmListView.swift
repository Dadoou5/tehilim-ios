import SwiftUI

struct PsalmListView: View {
    @EnvironmentObject private var container: AppContainer
    @EnvironmentObject private var favorites: FavoritesStore

    /// Si nil → liste complète.
    let book: Int?

    /// Optionnel : binding de sélection pour le mode NavigationSplitView (iPad).
    /// - nil : comportement standard (NavigationLink push, iPhone et iPad portrait)
    /// - non-nil : tap met à jour la sélection → la detail column affiche le Tehilim
    var selection: Binding<Int?>? = nil

    @State private var query: String = ""

    var body: some View {
        List(filteredPsalms) { psalm in
            if let selection {
                // iPad NavigationSplitView : tap → met à jour la detail column
                // V1.9.3 : utilise onTapGesture + contentShape pour assurer la
                // détection du tap dans toute la zone de la ligne (Button dans
                // List a une zone de tap parfois étroite).
                psalmRow(psalm, isSelected: selection.wrappedValue == psalm.id, showChevron: true)
                    .contentShape(Rectangle())
                    .onTapGesture {
                        selection.wrappedValue = psalm.id
                    }
                    .listRowBackground(
                        selection.wrappedValue == psalm.id
                        ? Color.accentMain.opacity(0.12)
                        : Color.clear
                    )
            } else {
                NavigationLink(destination: PsalmDetailView(psalmId: psalm.id)) {
                    psalmRow(psalm, isSelected: false, showChevron: false)
                }
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
    private func psalmRow(_ psalm: Psalm, isSelected: Bool, showChevron: Bool) -> some View {
        HStack(spacing: 12) {
            Text("\(psalm.id)")
                .font(.callout.weight(.semibold))
                .foregroundStyle(isSelected ? Color.accentMain : .primary)
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
            if showChevron {
                Image(systemName: "chevron.right")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.tertiary)
                    .accessibilityHidden(true)
            }
        }
        .padding(.vertical, 4)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Tehilim \(psalm.id), \(psalm.verses.count) versets\(favorites.contains(psalm.id) ? ", favori" : "")")
        .accessibilityHint("Ouvre le Tehilim")
        .accessibilityAddTraits(.isButton)
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
