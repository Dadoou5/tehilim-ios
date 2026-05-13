import SwiftUI

struct PsalmDetailView: View {
    @EnvironmentObject private var container: AppContainer
    @EnvironmentObject private var favorites: FavoritesStore
    @StateObject private var prefs = Preferences()

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    let psalmId: Int
    /// Liste de psaumes dans laquelle naviguer (prev/next).
    /// - nil : navigation naturelle 1→150 dans tout le corpus.
    /// - non nil : navigation restreinte à la liste (favoris, du jour, cas de la vie…).
    var siblings: [Int]? = nil

    @State private var localShowFR: Bool? = nil
    @State private var presentedPrayer: Prayer.Kind? = nil

    var body: some View {
        Group {
            if let psalm = container.psalmRepository.psalm(id: psalmId) {
                content(psalm: psalm)
            } else {
                EmptyStateView(
                    symbol: "exclamationmark.triangle",
                    title: "Tehilim introuvable",
                    message: "Ce psaume n'existe pas dans le corpus chargé."
                )
            }
        }
        .navigationBarTitleDisplayMode(.inline)
    }

    @ViewBuilder
    private func content(psalm: Psalm) -> some View {
        let n = computeNeighbors(for: psalm.id)

        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                IluyNishmatBanner()
                if let title = psalm.hebrewTitle {
                    Text(title)
                        .font(.hebrewTitle())
                        .multilineTextAlignment(.center)
                        .environment(\.layoutDirection, .rightToLeft)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .accessibilityAddTraits(.isHeader)
                }
                Divider().padding(.horizontal, 16)
                ForEach(psalm.verses) { verse in
                    VerseRowView(
                        verse: verse,
                        showTranslation: showFR,
                        textMode: prefs.textMode,
                        textSizeHebrew: prefs.textSizeHebrew,
                        textSizeFR: prefs.textSizeFR,
                        numberStyle: prefs.verseNumberStyle,
                        translationLang: prefs.appLanguage.translation,
                        parentPsalm: psalm
                    )
                    .padding(.horizontal, 16)
                    Divider().padding(.horizontal, 16).opacity(0.3)
                }
                navigation(prev: n.prev, next: n.next)
                    .padding(.vertical, 24)
            }
            .readingWidth()
        }
        .background(Color.bgPrimary)
        .navigationTitle("Tehilim \(psalm.id) · \(psalm.hebrewNumber)")
        .toolbar { toolbarContent(psalm: psalm) }
        .sheet(item: $presentedPrayer) { kind in
            PrayerView(prayer: Prayer.of(kind))
        }
        .transaction { tx in
            if reduceMotion { tx.animation = nil }
        }
        .onAppear {
            prefs.lastReadPsalmId = psalm.id
            if let firstVerse = psalm.verses.first { prefs.lastReadVerseId = firstVerse.id }
        }
    }

    @ToolbarContentBuilder
    private func toolbarContent(psalm: Psalm) -> some ToolbarContent {
        ToolbarItem(placement: .topBarTrailing) {
            Button {
                favorites.toggle(psalm.id)
            } label: {
                Image(systemName: favorites.contains(psalm.id) ? "heart.fill" : "heart")
            }
            .accessibilityLabel(favorites.contains(psalm.id) ? "Retirer des favoris" : "Ajouter aux favoris")
        }
        ToolbarItem(placement: .topBarTrailing) {
            Button {
                let current = showFR
                localShowFR = !current
            } label: {
                Image(systemName: showFR ? "character.bubble.fill" : "character.bubble")
            }
            .accessibilityLabel(showFR ? "Masquer la traduction française" : "Afficher la traduction française")
        }
        ToolbarItem(placement: .topBarTrailing) {
            Menu {
                Button {
                    presentedPrayer = .before
                } label: {
                    Label(Prayer.Kind.before.titleFR, systemImage: Prayer.Kind.before.symbol)
                }
                Button {
                    presentedPrayer = .after
                } label: {
                    Label(Prayer.Kind.after.titleFR, systemImage: Prayer.Kind.after.symbol)
                }
            } label: {
                Image(systemName: "ellipsis.circle")
            }
            .accessibilityLabel("Plus d'actions")
        }
    }

    @ViewBuilder
    private func navigation(prev: Int?, next: Int?) -> some View {
        HStack {
            if let prev {
                NavigationLink(destination: PsalmDetailView(psalmId: prev, siblings: siblings)) {
                    Label("Tehilim \(prev)", systemImage: "chevron.left")
                }
                .buttonStyle(.bordered)
                .accessibilityLabel("Tehilim précédent, numéro \(prev)")
            } else {
                Spacer()
            }
            Spacer()
            if let next {
                NavigationLink(destination: PsalmDetailView(psalmId: next, siblings: siblings)) {
                    Label("Tehilim \(next)", systemImage: "chevron.right")
                        .labelStyle(.titleAndIcon)
                        .environment(\.layoutDirection, .leftToRight)
                }
                .buttonStyle(.bordered)
                .accessibilityLabel("Tehilim suivant, numéro \(next)")
            } else {
                Spacer()
            }
        }
        .padding(.horizontal, 16)
    }

    /// Calcule prev/next dans la liste fournie, ou dans le corpus complet si aucune.
    private func computeNeighbors(for id: Int) -> (prev: Int?, next: Int?) {
        if let siblings, let idx = siblings.firstIndex(of: id) {
            let prev = idx > 0 ? siblings[idx - 1] : nil
            let next = idx < siblings.count - 1 ? siblings[idx + 1] : nil
            return (prev, next)
        }
        return container.psalmRepository.neighbors(of: id)
    }

    private var showFR: Bool { localShowFR ?? prefs.translationFR }
}
