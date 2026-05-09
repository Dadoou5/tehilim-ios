import SwiftUI

struct BookListView: View {
    var body: some View {
        List {
            ForEach(1...5, id: \.self) { book in
                NavigationLink(destination: PsalmListView(book: book)) {
                    HStack {
                        VStack(alignment: .leading) {
                            Text("Livre \(book)").font(.headline)
                            Text(rangeLabel(book)).font(.caption).foregroundStyle(.secondary)
                        }
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .appBackground()
    }

    private func rangeLabel(_ book: Int) -> String {
        guard let r = Psalm.bookRanges[book] else { return "" }
        return "\(r.lowerBound)–\(r.upperBound)"
    }
}
