import SwiftUI
import UIKit

struct VerseRowView: View {
    let verse: Verse
    let showTranslation: Bool
    let textMode: TextMode
    let textSizeHebrew: TextSize
    let textSizeFR: TextSize
    let numberStyle: VerseNumberStyle
    /// Référence facultative au psaume parent — nécessaire pour le partage stylisé.
    /// Si nil, le menu de partage propose seulement le texte brut.
    var parentPsalm: Psalm? = nil

    var body: some View {
        VStack(alignment: textMode == .hebrew ? .trailing : .leading, spacing: 8) {
            primaryRow
            if showTranslation { translationRow }
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

    @ViewBuilder
    private var primaryRow: some View {
        if textMode == .hebrew {
            HStack(alignment: .firstTextBaseline, spacing: 8) {
                Text(displayedNumber)
                    .font(.verseNumber(textSizeHebrew))
                    .foregroundStyle(.secondary)
                    .accessibilityHidden(true)
                Text(verse.hebrew)
                    .font(.hebrewBody(textSizeHebrew))
                    .multilineTextAlignment(.trailing)
                    .environment(\.layoutDirection, .rightToLeft)
                    .frame(maxWidth: .infinity, alignment: .trailing)
                    .lineSpacing(8)
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
        if let fr = verse.translationFR, !fr.isEmpty {
            Text(fr)
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
        if let fr = verse.translationFR { parts.append(fr) }
        parts.append("Traduction : Beth Loubavitch — le-tehilim.online")
        return parts.joined(separator: "\n")
    }

    @MainActor
    private func shareImage(psalm: Psalm) -> Image {
        if let ui = VerseShareImageRenderer.render(psalm: psalm, verse: verse) {
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
        if showTranslation, let fr = verse.translationFR, !fr.isEmpty {
            parts.append(fr)
        }
        return parts.joined(separator: " ")
    }
}
