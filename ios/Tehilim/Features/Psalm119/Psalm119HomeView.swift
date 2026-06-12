import SwiftUI

struct Psalm119HomeView: View {
    @EnvironmentObject private var container: AppContainer
    @EnvironmentObject private var savedPrayers: SavedPrayerStore
    @Environment(\.horizontalSizeClass) private var hSize

    @State private var showingForm: Bool = false

    private var isRegular: Bool { hSize == .regular }

    private var columnsCount: Int {
        AdaptiveLayout.psalm119ColumnCount(for: hSize)
    }

    private var columns: [GridItem] {
        Array(
            repeating: GridItem(.flexible(), spacing: isRegular ? 16 : 12),
            count: columnsCount
        )
    }

    /// V1.4 — sections aplaties pour rendu RTL : chaque ligne est inversée
    /// (Aleph finit en haut à droite), et la dernière ligne incomplète est
    /// préfixée de placeholders `nil` côté gauche pour coller ses lettres
    /// à droite. LazyVGrid remplit ensuite ce flat list cellule par cellule
    /// dans l'ordre LTR natif → visuellement on lit RTL.
    private var rtlGridCells: [Psalm119Section?] {
        let raw = container.psalm119Repository.sections
        let cols = columnsCount
        guard cols > 0, !raw.isEmpty else { return [] }
        var result: [Psalm119Section?] = []
        var index = 0
        while index < raw.count {
            let end = min(index + cols, raw.count)
            let chunk = Array(raw[index..<end])
            let padding = cols - chunk.count
            // Placeholders à gauche pour la ligne incomplète
            if padding > 0 {
                result.append(contentsOf: Array(repeating: nil, count: padding))
            }
            // Sections inversées (la plus petite index = la plus à droite)
            result.append(contentsOf: chunk.reversed().map { Optional($0) })
            index = end
        }
        return result
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: isRegular ? 20 : 16) {
                if isRegular {
                    headerCard
                }

                // V1.10.0 — Entry points lecture personnalisée
                personalizedSection

                // Grille alphabet — sens de lecture hébreu (Aleph haut-droite).
                // Voir `rtlGridCells` pour la construction du flat list avec
                // placeholders. La grille est verrouillée en LTR : l'ordre RTL
                // est déjà construit manuellement — sous UI hébreu (RTL
                // global), laisser la grille hériter doublerait l'inversion
                // et Aleph partirait à gauche.
                LazyVGrid(columns: columns, spacing: isRegular ? 16 : 12) {
                    ForEach(Array(rtlGridCells.enumerated()), id: \.offset) { _, maybeSection in
                        if let section = maybeSection {
                            NavigationLink(destination: Psalm119SectionView(index: section.index)) {
                                HebrewLetterTile(
                                    letter: section.letter,
                                    index: section.index,
                                    name: section.name,
                                    verseStart: section.verseStart,
                                    verseEnd: section.verseEnd
                                )
                            }
                            .buttonStyle(.plain)
                        } else {
                            // Cellule vide pour aligner la dernière ligne à droite
                            Color.clear
                                .frame(maxWidth: .infinity, minHeight: isRegular ? 150 : 88)
                                .accessibilityHidden(true)
                        }
                    }
                }
                .environment(\.layoutDirection, .leftToRight)
            }
            .padding(.horizontal, AdaptiveLayout.horizontalPadding(for: hSize))
            .padding(.vertical, isRegular ? 24 : 16)
        }
        .background(Color.bgPrimary)
        .navigationTitle("119 - AlphaBeta")
        .sheet(isPresented: $showingForm) {
            PersonalizedReadingFormView()
        }
    }

    // MARK: - Header iPad

    @ViewBuilder
    private var headerCard: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Tehilim 119")
                .font(.title2.weight(.semibold))
                .foregroundStyle(.primary)
            Text("Les 22 lettres de l'alphabet hébreu, huit versets chacune.")
                .font(.callout)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
        .background(Color.bgSurface)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(Color.dividerToken.opacity(0.4), lineWidth: 0.5)
        )
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isHeader)
    }

    // MARK: - Section lecture personnalisée

    @ViewBuilder
    private var personalizedSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Lelouy Nichmat")
                .font(.headline)
                .accessibilityAddTraits(.isHeader)

            LazyVGrid(
                columns: Array(
                    repeating: GridItem(.flexible(), spacing: 12),
                    count: isRegular ? 2 : 1
                ),
                spacing: 12
            ) {
                // Carte : nouvelle lecture
                // V1.10.7 — subtitle via L() pour passer par le swizzle
                // Bundle (sans ça, `String` passé à `Text()` ne se localise
                // pas — seul `LocalizedStringKey` le fait automatiquement).
                Button { showingForm = true } label: {
                    actionCard(
                        symbol: "flame.fill",
                        title: "Nouvelle lecture",
                        subtitle: L("Élévation de l'âme — prénom + mère"),
                        accent: true
                    )
                }
                .buttonStyle(.plain)

                // Carte : prières sauvegardées
                NavigationLink(destination: SavedPrayersListView()) {
                    actionCard(
                        symbol: "tray.full",
                        title: "Mes prières",
                        subtitle: savedCountLabel,
                        accent: false
                    )
                }
                .buttonStyle(.plain)
            }
        }
    }

    // V1.10.7 — localisé via L() (passe par le swizzle Bundle pour
    // suivre la bascule de langue à chaud V2.1.b).
    private var savedCountLabel: String {
        let count = savedPrayers.intents.count
        switch count {
        case 0: return L("Aucun sauvegardé")
        case 1: return L("1 sauvegardé")
        default: return String(format: L("%lld sauvegardés"), count)
        }
    }

    @ViewBuilder
    private func actionCard(symbol: String, title: LocalizedStringKey, subtitle: String, accent: Bool) -> some View {
        HStack(spacing: 14) {
            ZStack {
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(Color.accentMain.opacity(accent ? 0.18 : 0.10))
                    .frame(width: 44, height: 44)
                Image(systemName: symbol)
                    .font(.title3.weight(.medium))
                    .foregroundStyle(Color.accentMain)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.headline)
                    .foregroundStyle(.primary)
                Text(subtitle)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            Image(systemName: "chevron.right")
                .font(.caption.weight(.semibold))
                .foregroundStyle(.tertiary)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.bgSurface)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(Color.dividerToken.opacity(0.4), lineWidth: 0.5)
        )
    }
}
