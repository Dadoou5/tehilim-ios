import SwiftUI

struct BookListView: View {
    /// Forwardé à PsalmListView pour activer la sélection en mode NavigationSplitView.
    var selection: Binding<Int?>? = nil

    var body: some View {
        List {
            ForEach(1...5, id: \.self) { book in
                NavigationLink(destination: PsalmListView(book: book, selection: selection)) {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Livre \(book)").font(.headline)
                            Text(rangeLabel(book))
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                        Text("\(psalmCount(book)) Tehilim")
                            .font(.caption.weight(.medium))
                            .foregroundStyle(Color.accentMain)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(
                                RoundedRectangle(cornerRadius: 6, style: .continuous)
                                    .fill(Color.accentMuted.opacity(0.35))
                            )
                    }
                    .padding(.vertical, 4)
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

    private func psalmCount(_ book: Int) -> Int {
        guard let r = Psalm.bookRanges[book] else { return 0 }
        return r.upperBound - r.lowerBound + 1
    }
}
