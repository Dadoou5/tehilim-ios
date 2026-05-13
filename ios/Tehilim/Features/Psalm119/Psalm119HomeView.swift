import SwiftUI

struct Psalm119HomeView: View {
    @EnvironmentObject private var container: AppContainer

    private let columns: [GridItem] = Array(repeating: GridItem(.flexible(), spacing: 12), count: 4)

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
            .padding(16)
        }
        .background(Color.bgPrimary)
        .navigationTitle("119 - AlphaBeta")
    }
}
