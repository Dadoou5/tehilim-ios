import SwiftUI
import UIKit

struct VerseRowView: View {
    let verse: Verse
    let showTranslation: Bool
    let textMode: TextMode
    let textSizeHebrew: TextSize
    let textSizeFR: TextSize
    let numberStyle: VerseNumberStyle
    /// Langue de la traduction à afficher.
    var translationLang: TranslationLanguage = .fr
    /// Référence facultative au psaume parent — nécessaire pour le partage stylisé.
    /// Si nil, le menu de partage propose seulement le texte brut.
    var parentPsalm: Psalm? = nil
    /// Mode lecture parallèle (V1.9.0) : hébreu et traduction côte-à-côte au lieu d'empilés.
    /// Active sur iPad paysage uniquement, calculé par le parent.
    var sideBySideTranslation: Bool = false

    /// Side-by-side actif seulement si on a vraiment quelque chose à mettre côte-à-côte.
    private var useSideBySide: Bool {
        sideBySideTranslation && showTranslation && textMode == .hebrew
    }

    var body: some View {
        Group {
            if useSideBySide {
                sideBySideLayout
            } else {
                stackedLayout
            }
        }
        .padding(.vertical, 8)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(accessibilityLabel)
        .accessibilityAddTraits(.isStaticText)
        .contextMenu {
            Button {
                UIPasteboard.general.string = textForCopy
            } label: {
                Label("Copier", systemImage: "doc.on.doc")
            }
            if let psalm = parentPsalm {
                ShareLink(
                    item: shareImage(psalm: psalm),
                    preview: SharePreview(
                        "Tehilim \(psalm.id) · verset \(verse.number)",
                        image: shareImage(psalm: psalm)
                    )
                ) {
                    Label("Partager", systemImage: "square.and.arrow.up")
                }
            } else {
                ShareLink(item: textForCopy) {
                    Label("Partager le texte", systemImage: "square.and.arrow.up")
                }
            }
        }
    }

    /// Layout par défaut : empilé (numéro + hébreu en haut, traduction en dessous).
    @ViewBuilder
    private var stackedLayout: some View {
        VStack(alignment: textMode == .hebrew ? .trailing : .leading, spacing: 8) {
            primaryRow
            if showTranslation { translationRow }
        }
    }

    /// Layout parallèle (iPad paysage) : traduction à gauche (LTR),
    /// hébreu+numéro à droite (RTL natif).
    @ViewBuilder
    private var sideBySideLayout: some View {
        HStack(alignment: .top, spacing: 24) {
            translationRow
                .frame(maxWidth: .infinity, alignment: .leading)

            HStack(alignment: .firstTextBaseline, spacing: 8) {
                Text(verse.hebrew)
                    .font(.hebrewBody(textSizeHebrew))
                    .multilineTextAlignment(.trailing)
                    .frame(maxWidth: .infinity, alignment: .trailing)
                    .lineSpacing(8)
                Text(displayedNumber)
                    .font(.verseNumber(textSizeHebrew))
                    .foregroundStyle(.secondary)
                    .accessibilityHidden(true)
            }
            .frame(maxWidth: .infinity)
        }
    }

    @ViewBuilder
    private var primaryRow: some View {
        if textMode == .hebrew {
            HStack(alignment: .firstTextBaseline, spacing: 8) {
                Text(verse.hebrew)
                    .font(.hebrewBody(textSizeHebrew))
                    .multilineTextAlignment(.trailing)
                    .frame(maxWidth: .infinity, alignment: .trailing)
                    .lineSpacing(8)
                Text(displayedNumber)
                    .font(.verseNumber(textSizeHebrew))
                    .foregroundStyle(.secondary)
                    .accessibilityHidden(true)
            }
        } else {
            HStack(alignment: .firstTextBaseline, spacing: 8) {
                Text(displayedNumber)
                    .font(.verseNumber(textSizeHebrew))
                    .foregroundStyle(.secondary)
                    .accessibilityHidden(true)
                Text(phoneticText)
                    .font(.hebrewBody(textSizeHebrew))
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .lineSpacing(4)
                    .italic()
            }
        }
    }

    @ViewBuilder
    private var translationRow: some View {
        if let text = verse.translation(for: translationLang), !text.isEmpty {
            Text(text)
                .font(.frBody(textSizeFR))
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.leading)
                .frame(maxWidth: .infinity, alignment: .leading)
                .lineSpacing(2)
        } else {
            Text("Traduction non disponible.")
                .font(.caption)
                .foregroundStyle(.tertiary)
                .italic()
        }
    }

    private var displayedNumber: String {
        switch numberStyle {
        case .hebrew: return verse.hebrewNumber
        case .arabic: return "\(verse.number)"
        }
    }

    private var phoneticText: String {
        HebrewTransliterator.transliterate(verse.hebrew)
    }

    private var textForCopy: String {
        var parts: [String] = []
        parts.append("Tehilim — verset \(verse.number)")
        parts.append(verse.hebrew)
        if let translation = verse.translation(for: translationLang) {
            parts.append(translation)
        }
        parts.append("Source : \(translationLang.sourceCredit)")
        return parts.joined(separator: "\n")
    }

    @MainActor
    private func shareImage(psalm: Psalm) -> Image {
        if let ui = VerseShareImageRenderer.render(
            psalm: psalm,
            verse: verse,
            translationLang: translationLang
        ) {
            return Image(uiImage: ui)
        }
        return Image(systemName: "square.and.arrow.up")
    }

    private var accessibilityLabel: String {
        var parts = ["Verset \(verse.number)."]
        switch textMode {
        case .hebrew:   parts.append(verse.hebrew)
        case .phonetic: parts.append(phoneticText)
        }
        if showTranslation, let text = verse.translation(for: translationLang), !text.isEmpty {
            parts.append(text)
        }
        return parts.joined(separator: " ")
    }
}
