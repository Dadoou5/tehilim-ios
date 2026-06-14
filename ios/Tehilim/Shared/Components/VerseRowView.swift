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
    /// V2.4 — mode étude : commentaires du verset (Rashi, Metzudat David).
    var commentaries: [VerseCommentary] = []
    /// V2.4 — affiche l'affordance des commentaires (option globale).
    var showCommentaries: Bool = false

    @State private var commentariesExpanded = false

    /// Side-by-side actif seulement si on a vraiment quelque chose à mettre côte-à-côte.
    private var useSideBySide: Bool {
        sideBySideTranslation && showTranslation && textMode == .hebrew
    }

    private var hasCommentaries: Bool { showCommentaries && !commentaries.isEmpty }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            verseContent
            if hasCommentaries { commentarySection }
        }
        .padding(.vertical, 8)
    }

    /// Le verset (numéro + hébreu/phonétique + traduction) avec son
    /// accessibilité et son menu contextuel — scope hors commentaires.
    @ViewBuilder
    private var verseContent: some View {
        Group {
            if useSideBySide {
                sideBySideLayout
            } else {
                stackedLayout
            }
        }
        // V2.2.c — ligne verrouillée en LTR : l'alignement à droite du texte
        // hébreu (.trailing) et à gauche de la traduction (.leading) doit
        // rester identique quelle que soit la langue d'interface. Sous UI
        // hébraïque (RTL global), sans ce verrou, .trailing s'inverserait en
        // gauche et le numéro passerait du mauvais côté. Le moteur de texte
        // gère le RTL interne de l'hébreu (bidi) indépendamment.
        .environment(\.layoutDirection, .leftToRight)
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
                // V1.10.4 : Transferable lazy — l'image 1080×1080 n'est rendue
                // qu'au moment où l'utilisateur tape effectivement sur Partager.
                // Avant : 2 × 18 MB matérialisés par verset visible → OOM.
                ShareLink(
                    item: VerseShareTransferable(
                        psalm: psalm,
                        verse: verse,
                        translationLang: translationLang
                    ),
                    preview: SharePreview(
                        "Tehilim \(psalm.id) · verset \(verse.number)",
                        image: Image(systemName: "doc.richtext")
                    )
                ) {
                    Label("Partager", systemImage: "square.and.arrow.up")
                }
                ShareLink(item: textForCopy) {
                    Label("Partager le texte", systemImage: "doc.plaintext")
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

    // MARK: - Commentaires (mode étude, V2.4)

    @ViewBuilder
    private var commentarySection: some View {
        VStack(alignment: .leading, spacing: 0) {
            Button {
                withAnimation(.easeInOut(duration: 0.2)) { commentariesExpanded.toggle() }
            } label: {
                HStack(spacing: 6) {
                    Image(systemName: "text.book.closed")
                    Text("Commentaires")
                    Text("· \(commentaries.count)")
                    Spacer()
                    Image(systemName: commentariesExpanded ? "chevron.up" : "chevron.down")
                }
                .font(.caption.weight(.semibold))
                .foregroundStyle(Color.accentMain)
                .padding(.top, 6)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .environment(\.layoutDirection, .leftToRight)

            if commentariesExpanded {
                VStack(alignment: .leading, spacing: 8) {
                    ForEach(commentaries) { commentaryCard($0) }
                }
                .padding(.top, 8)
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
    }

    /// Une carte de commentaire. Le texte suit la langue de l'app : hébreu (RTL,
    /// avec dibour hamatchil), ou traduction FR/EN (LTR) quand elle existe.
    @ViewBuilder
    private func commentaryCard(_ c: VerseCommentary) -> some View {
        let code = AppLocale.code
        let body = c.text(for: code)
        let isHebrew = body == c.he
        VStack(alignment: isHebrew ? .trailing : .leading, spacing: 4) {
            Text(c.kind.hebrewName)
                .font(.caption2.weight(.bold))
                .foregroundStyle(Color.accentMain)
            if isHebrew {
                hebrewCommentaryText(c)
                    .font(.hebrewBody(.s))
                    .multilineTextAlignment(.trailing)
                    .frame(maxWidth: .infinity, alignment: .trailing)
                    .lineSpacing(4)
                    .environment(\.layoutDirection, .rightToLeft)
            } else {
                latinCommentaryText(c, body)
                    .font(.frBody(.s))
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .lineSpacing(3)
                    .environment(\.layoutDirection, .leftToRight)
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: isHebrew ? .trailing : .leading)
        .background(Color.accentMain.opacity(0.06))
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
    }

    /// Hébreu : dibour hamatchil en gras (primaire) + corps (secondaire).
    private func hebrewCommentaryText(_ c: VerseCommentary) -> Text {
        if let lemma = c.lemma, !lemma.isEmpty {
            return Text(lemma).bold().foregroundColor(.primary)
                + Text(" ") + Text(c.he).foregroundColor(.secondary)
        }
        return Text(c.he).foregroundColor(.secondary)
    }

    /// FR/EN : dibour hamatchil **hébreu** en gras (non traduit) + traduction.
    /// Le mot hébreu s'aligne naturellement (bidi) au début du texte latin.
    private func latinCommentaryText(_ c: VerseCommentary, _ body: String) -> Text {
        if let lemma = c.lemma, !lemma.isEmpty {
            return Text(lemma).bold().foregroundColor(.primary)
                + Text(" ") + Text(body).foregroundColor(.secondary)
        }
        return Text(body).foregroundColor(.secondary)
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
        parts.append("לעילוי נשמת ג׳והאן מאיר בן שרה בוגנים")
        parts.append(NSLocalizedString("Pour l'élévation de l'âme de Johann Meïr ben Sarah Bouganim", comment: ""))
        return parts.joined(separator: "\n")
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
