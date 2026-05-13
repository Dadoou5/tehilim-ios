import SwiftUI

struct Psalm119HomeView: View {
    @EnvironmentObject private var container: AppContainer
    @Environment(\.horizontalSizeClass) private var hSize

    private var columns: [GridItem] {
        Array(
            repeating: GridItem(.flexible(), spacing: 12),
            count: AdaptiveLayout.psalm119ColumnCount(for: hSize)
        )
    }

    var body: some View {
        ScrollView {
            LazyVGrid(columns: columns, spacing: 12) {
                ForEach(container.psalm119Repository.sections) { section in
                    NavigationLink(destination: Psalm119SectionView(index: section.index)) {
                        HebrewLetterTile(letter: section.letter, index: section.index)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, AdaptiveLayout.horizontalPadding(for: hSize))
            .padding(.vertical, 16)
            .readingWidth()
        }
        .background(Color.bgPrimary)
        .navigationTitle("119 - AlphaBeta")
    }
}
