import SwiftUI

struct Psalm119HomeView: View {
    @EnvironmentObject private var container: AppContainer
    @Environment(\.horizontalSizeClass) private var hSize

    private var isRegular: Bool { hSize == .regular }

    private var columns: [GridItem] {
        Array(
            repeating: GridItem(.flexible(), spacing: isRegular ? 16 : 12),
            count: AdaptiveLayout.psalm119ColumnCount(for: hSize)
        )
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: isRegular ? 20 : 12) {
                if isRegular {
                    // Header iPad : contexte de la page
                    headerCard
                }

                LazyVGrid(columns: columns, spacing: isRegular ? 16 : 12) {
                    ForEach(container.psalm119Repository.sections) { section in
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
                    }
                }
            }
            .padding(.horizontal, AdaptiveLayout.horizontalPadding(for: hSize))
            .padding(.vertical, isRegular ? 24 : 16)
            // Pas de readingWidth() ici : on veut utiliser toute la largeur iPad.
        }
        .background(Color.bgPrimary)
        .navigationTitle("119 - AlphaBeta")
    }

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
}
