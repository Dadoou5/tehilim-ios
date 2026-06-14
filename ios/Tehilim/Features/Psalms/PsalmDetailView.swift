import SwiftUI

struct PsalmDetailView: View {
    @EnvironmentObject private var container: AppContainer
    @EnvironmentObject private var favorites: FavoritesStore
    @StateObject private var prefs = Preferences()

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.horizontalSizeClass) private var hSize

    let psalmId: Int
    /// Liste de psaumes dans laquelle naviguer (prev/next).
    /// - nil : navigation naturelle 1→150 dans tout le corpus.
    /// - non nil : navigation restreinte à la liste (favoris, du jour, cas de la vie…).
    var siblings: [Int]? = nil

    @State private var localShowFR: Bool? = nil
    @State private var showTextSize = false
    @State private var presentedPrayer: Prayer.Kind? = nil
    @State private var containerWidth: CGFloat = 0

    private var sideBySide: Bool {
        AdaptiveLayout.shouldUseSideBySide(containerWidth: containerWidth, sizeClass: hSize)
            && showFR
            && prefs.textMode == .hebrew
    }

    /// Lecture en **deux colonnes** (façon Tehilim imprimé) : activée sur iPad
    /// suffisamment large quand on lit un **texte seul** (hébreu seul ou
    /// phonétique seul, traduction masquée) — là où une colonne unique laissait
    /// d'immenses marges vides en paysage. Exclut le mode parallèle (hébreu +
    /// traduction) qui occupe déjà la largeur. Seuil sur le nombre de versets
    /// pour éviter de scinder un psaume très court.
    private var twoColumnText: Bool {
        hSize == .regular
            && containerWidth >= AdaptiveLayout.sideBySideMinWidth
            && !sideBySide
            && !showFR
    }

    private var maxContentWidth: CGFloat {
        (sideBySide || twoColumnText)
            ? AdaptiveLayout.sideBySideMaxWidth
            : AdaptiveLayout.readingMaxWidth
    }

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

                // iPad : barre d'action inline pour rendre le toggle traduction
                // immédiatement visible (la toolbar SwiftUI peut le masquer en NavigationSplitView).
                if hSize == .regular {
                    inlineTranslationToggle
                }

                // Numéro du Tehilim toujours visible dans le contenu (le titre de
                // la barre peut être tronqué par les boutons sur petit écran).
                VStack(spacing: 1) {
                    Text("Tehilim \(psalm.id)")
                        .font(.headline)
                    Text(psalm.hebrewNumber)
                        .font(.subheadline).foregroundStyle(.secondary)
                        .environment(\.layoutDirection, .rightToLeft)
                }
                .frame(maxWidth: .infinity)
                .padding(.top, 4)

                if let title = psalm.hebrewTitle {
                    Text(title)
                        .font(.hebrewTitle())
                        .multilineTextAlignment(.center)
                        .environment(\.layoutDirection, .rightToLeft)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .accessibilityAddTraits(.isHeader)
                }
                // Temps de lecture approximatif.
                HStack(spacing: 5) {
                    Image(systemName: "clock")
                    Text("~\(psalm.estimatedReadingMinutes) min")
                }
                .font(.caption)
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity)
                .padding(.bottom, 6)
                .accessibilityElement(children: .combine)
                .accessibilityLabel("Temps de lecture environ \(psalm.estimatedReadingMinutes) minutes")
                Divider().padding(.horizontal, 16)
                if twoColumnText && psalm.verses.count >= 6 {
                    twoColumnVerses(psalm: psalm)
                } else {
                    ForEach(psalm.verses) { verse in
                        verseRow(verse, psalm: psalm)
                            .padding(.horizontal, 16)
                        Divider().padding(.horizontal, 16).opacity(0.3)
                    }
                }
                navigation(prev: n.prev, next: n.next)
                    .padding(.vertical, 24)
            }
            .readingWidth(maxWidth: maxContentWidth)
        }
        .background(Color.bgPrimary)
        .background(
            GeometryReader { proxy in
                Color.clear
                    .onAppear { containerWidth = proxy.size.width }
                    .onChange(of: proxy.size.width) { _, new in containerWidth = new }
            }
        )
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

    /// Une ligne de verset (facteur commun mono-colonne / deux-colonnes).
    @ViewBuilder
    private func verseRow(_ verse: Verse, psalm: Psalm) -> some View {
        VerseRowView(
            verse: verse,
            showTranslation: showFR,
            textMode: prefs.textMode,
            textSizeHebrew: prefs.textSizeHebrew,
            textSizeFR: prefs.textSizeFR,
            numberStyle: prefs.verseNumberStyle,
            translationLang: prefs.appLanguage.translation,
            parentPsalm: psalm,
            sideBySideTranslation: sideBySide
        )
    }

    /// Lecture en deux colonnes (texte seul, iPad large). Les versets sont
    /// répartis en deux moitiés de comptage égal qui se lisent **de haut en
    /// bas** : colonne A puis colonne B. L'ordre visuel des colonnes respecte
    /// le sens de lecture — pour l'hébreu (RTL) la 1ʳᵉ moitié est à **droite**,
    /// pour la phonétique (LTR) à **gauche**.
    @ViewBuilder
    private func twoColumnVerses(psalm: Psalm) -> some View {
        let verses = psalm.verses
        let mid = (verses.count + 1) / 2
        let firstHalf = Array(verses[..<mid])
        let secondHalf = Array(verses[mid...])
        let isHebrew = prefs.textMode == .hebrew

        HStack(alignment: .top, spacing: 28) {
            // Colonne de gauche : 2ᵉ moitié en hébreu, 1ʳᵉ moitié en phonétique.
            verseColumn(isHebrew ? secondHalf : firstHalf, psalm: psalm)
            Divider()
            // Colonne de droite : 1ʳᵉ moitié en hébreu (lue en premier), 2ᵉ en phonétique.
            verseColumn(isHebrew ? firstHalf : secondHalf, psalm: psalm)
        }
        .padding(.horizontal, 16)
    }

    @ViewBuilder
    private func verseColumn(_ verses: [Verse], psalm: Psalm) -> some View {
        LazyVStack(alignment: prefs.textMode == .hebrew ? .trailing : .leading, spacing: 0) {
            ForEach(verses) { verse in
                verseRow(verse, psalm: psalm)
                Divider().opacity(0.3)
            }
        }
        .frame(maxWidth: .infinity, alignment: .top)
    }

    /// Barre d'action inline visible sur iPad — duplique le toggle du toolbar
    /// pour garantir sa découvrabilité, surtout dans le contexte NavigationSplitView
    /// où SwiftUI peut masquer ou tronquer les items du toolbar.
    @ViewBuilder
    private var inlineTranslationToggle: some View {
        HStack(spacing: 12) {
            Spacer()
            Button {
                let current = showFR
                localShowFR = !current
            } label: {
                Label(
                    showFR ? "Masquer la traduction" : "Afficher la traduction",
                    systemImage: showFR ? "character.bubble.fill" : "character.bubble"
                )
                .font(.subheadline.weight(.medium))
            }
            .buttonStyle(.bordered)
            .tint(Color.accentMain)
            .accessibilityLabel(showFR ? "Masquer la traduction" : "Afficher la traduction")
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
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
                Label(
                    showFR ? "Masquer la traduction" : "Afficher la traduction",
                    systemImage: showFR ? "character.bubble.fill" : "character.bubble"
                )
            }
            .help(showFR ? "Masquer la traduction" : "Afficher la traduction")
            .accessibilityLabel(showFR ? "Masquer la traduction" : "Afficher la traduction")
        }
        ToolbarItem(placement: .topBarTrailing) {
            Button {
                showTextSize = true
            } label: {
                Image(systemName: "textformat.size")
            }
            .help("Taille du texte")
            .accessibilityLabel("Taille du texte")
            .popover(isPresented: $showTextSize) {
                ReadingTextSizeControl(prefs: prefs, includeTranslation: showFR)
                    .presentationCompactAdaptation(.popover)
            }
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
                .keyboardShortcut("[", modifiers: .command)
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
                .keyboardShortcut("]", modifiers: .command)
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
