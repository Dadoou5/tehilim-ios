import SwiftUI

struct Psalm119SectionView: View {
    @EnvironmentObject private var container: AppContainer
    @StateObject private var prefs = Preferences()

    let index: Int
    @State private var presentedPrayer: Prayer.Kind? = nil
    @State private var localShowFR: Bool? = nil

    private var showFR: Bool { localShowFR ?? prefs.translationFR }

    var body: some View {
        Group {
            if let section = container.psalm119Repository.section(at: index),
               let psalm = container.psalmRepository.psalm(id: 119) {
                let verses = psalm.verses.filter { section.versesRange.contains($0.number) }
                ScrollView {
                    LazyVStack(alignment: .leading) {
                        IluyNishmatBanner()
                        Text("\(section.letter) — \(section.name) · v. \(section.verseStart)–\(section.verseEnd)")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                        ForEach(verses) { v in
                            VerseRowView(
                                verse: v,
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
                        navigationFooter()
                            .padding(.vertical, 24)
                    }
                    .readingWidth()
                }
                .background(Color.bgPrimary)
                .navigationTitle(section.letter)
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button {
                            let current = showFR
                            localShowFR = !current
                        } label: {
                            Image(systemName: showFR ? "character.bubble.fill" : "character.bubble")
                        }
                        .accessibilityLabel(showFR ? "Masquer la traduction" : "Afficher la traduction")
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
                .sheet(item: $presentedPrayer) { kind in
                    PrayerView(prayer: Prayer.of(kind))
                }
            } else {
                EmptyStateView(symbol: "exclamationmark.triangle", title: "Section introuvable", message: nil)
            }
        }
    }

    @ViewBuilder
    private func navigationFooter() -> some View {
        HStack {
            if index > 1 {
                NavigationLink(destination: Psalm119SectionView(index: index - 1)) {
                    Label("Précédent", systemImage: "chevron.left")
                }
                .buttonStyle(.bordered)
            }
            Spacer()
            if index < 22 {
                NavigationLink(destination: Psalm119SectionView(index: index + 1)) {
                    Label("Suivant", systemImage: "chevron.right")
                }
                .buttonStyle(.bordered)
            }
        }
        .padding(.horizontal, 16)
    }
}
