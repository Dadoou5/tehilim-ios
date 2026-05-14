import SwiftUI

struct Psalm119SectionView: View {
    @EnvironmentObject private var container: AppContainer
    @EnvironmentObject private var savedPrayers: SavedPrayerStore
    @StateObject private var prefs = Preferences()
    @Environment(\.horizontalSizeClass) private var hSize

    let index: Int

    /// Contexte de séquence personnalisée — `nil` = navigation alphabet 1..22.
    /// Non-nil = navigation dans la séquence générée (Précédent/Suivant ciblés).
    var sequenceContext: PsalmSequenceContext? = nil

    @State private var presentedPrayer: Prayer.Kind? = nil
    @State private var localShowFR: Bool? = nil
    @State private var containerWidth: CGFloat = 0

    private var showFR: Bool { localShowFR ?? prefs.translationFR }

    private var sideBySide: Bool {
        AdaptiveLayout.shouldUseSideBySide(containerWidth: containerWidth, sizeClass: hSize)
            && showFR
            && prefs.textMode == .hebrew
    }

    private var maxContentWidth: CGFloat {
        sideBySide ? AdaptiveLayout.sideBySideMaxWidth : AdaptiveLayout.readingMaxWidth
    }

    var body: some View {
        Group {
            if let section = container.psalm119Repository.section(at: index),
               let psalm = container.psalmRepository.psalm(id: 119) {
                let verses = psalm.verses.filter { section.versesRange.contains($0.number) }
                ScrollView {
                    LazyVStack(alignment: .leading) {
                        IluyNishmatBanner()

                        if let ctx = sequenceContext {
                            sequenceProgressHeader(ctx, section: section)
                        }

                        // iPad : barre d'action inline pour toggle traduction
                        if hSize == .regular {
                            inlineTranslationToggle
                        }

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
                                parentPsalm: psalm,
                                sideBySideTranslation: sideBySide
                            )
                            .padding(.horizontal, 16)
                            Divider().padding(.horizontal, 16).opacity(0.3)
                        }
                        navigationFooter()
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
                .navigationTitle(section.letter)
                .toolbar { toolbarItems }
                .sheet(item: $presentedPrayer) { kind in
                    PrayerView(prayer: Prayer.of(kind))
                }
                .onAppear {
                    // Si on est dans une séquence sauvegardée, mémoriser la position lue.
                    if let ctx = sequenceContext, let savedId = ctx.savedIntentId {
                        savedPrayers.updateLastReadIndex(
                            intentId: savedId,
                            lastReadIndex: ctx.currentPosition
                        )
                    }
                }
            } else {
                EmptyStateView(symbol: "exclamationmark.triangle", title: "Section introuvable", message: nil)
            }
        }
    }

    // MARK: - Toolbar

    @ToolbarContentBuilder
    private var toolbarItems: some ToolbarContent {
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

    // MARK: - Sequence progress header

    @ViewBuilder
    private func sequenceProgressHeader(_ ctx: PsalmSequenceContext, section: Psalm119Section) -> some View {
        HStack(spacing: 10) {
            Image(systemName: "list.number")
                .font(.caption.weight(.semibold))
                .foregroundStyle(Color.accentMain)
            Text(ctx.progressLabel)
                .font(.subheadline.weight(.medium))
                .foregroundStyle(.primary)
            if let item = ctx.currentItem {
                Text("· source : \(item.source.labelFR)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .background(Color.accentMain.opacity(0.10))
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
        .padding(.horizontal, 16)
        .padding(.top, 4)
        .accessibilityElement(children: .combine)
    }

    // MARK: - Inline translation toggle (iPad)

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

    // MARK: - Navigation footer (alphabet ou séquence)

    @ViewBuilder
    private func navigationFooter() -> some View {
        if let ctx = sequenceContext {
            sequenceFooter(ctx: ctx)
        } else {
            alphabetFooter
        }
    }

    /// Footer en mode séquence personnalisée : prev/next dans la séquence.
    @ViewBuilder
    private func sequenceFooter(ctx: PsalmSequenceContext) -> some View {
        HStack {
            if let prev = ctx.previousItem() {
                NavigationLink(destination: Psalm119SectionView(
                    index: sectionIndex(for: prev.psalmLetterKey) ?? 1,
                    sequenceContext: ctx.advance(by: -1)
                )) {
                    Label("Précédent", systemImage: "chevron.left")
                }
                .buttonStyle(.bordered)
                .keyboardShortcut("[", modifiers: .command)
            } else {
                Label("Précédent", systemImage: "chevron.left")
                    .labelStyle(.titleAndIcon)
                    .foregroundStyle(.tertiary)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
            }

            Spacer()

            Text(ctx.progressLabel)
                .font(.caption.weight(.medium))
                .foregroundStyle(.secondary)

            Spacer()

            if let next = ctx.nextItem() {
                NavigationLink(destination: Psalm119SectionView(
                    index: sectionIndex(for: next.psalmLetterKey) ?? 1,
                    sequenceContext: ctx.advance(by: 1)
                )) {
                    Label("Suivant", systemImage: "chevron.right")
                }
                .buttonStyle(.bordered)
                .keyboardShortcut("]", modifiers: .command)
            } else {
                Label("Suivant", systemImage: "chevron.right")
                    .labelStyle(.titleAndIcon)
                    .foregroundStyle(.tertiary)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
            }
        }
        .padding(.horizontal, 16)
    }

    /// Footer alphabet classique 1..22 (comportement historique).
    @ViewBuilder
    private var alphabetFooter: some View {
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

    private func sectionIndex(for letter: String) -> Int? {
        container.psalm119Repository.section(forLetter: letter)?.index
    }
}
