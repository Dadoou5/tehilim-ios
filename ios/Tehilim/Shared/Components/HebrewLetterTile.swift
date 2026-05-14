import SwiftUI

/// Pavé d'une lettre hébraïque pour la grille du Tehilim 119.
///
/// V1.9.2 : design adaptatif iPhone/iPad.
/// - iPhone (compact) : compact, juste la lettre + le numéro
/// - iPad (regular)  : plus aéré, ajoute le nom de la lettre (אלף, בית...) et le range de versets
struct HebrewLetterTile: View {
    @Environment(\.horizontalSizeClass) private var hSize

    let letter: String
    let index: Int
    /// Nom phonétique de la lettre (ex. « אלף », « בית »). Affiché uniquement en regular.
    var name: String? = nil
    /// Numéro du premier verset de la section (ex. 1). Affiché en regular.
    var verseStart: Int? = nil
    /// Numéro du dernier verset de la section (ex. 8). Affiché en regular.
    var verseEnd: Int? = nil

    private var isRegular: Bool { hSize == .regular }

    private var letterFontSize: CGFloat { isRegular ? 64 : 44 }
    private var minHeight: CGFloat { isRegular ? 150 : 88 }

    var body: some View {
        VStack(spacing: isRegular ? 8 : 4) {
            // Lettre principale
            Text(letter)
                .font(.system(size: letterFontSize, weight: .regular, design: .serif))
                .foregroundStyle(.primary)
                .accessibilityHidden(true)

            if isRegular, let name {
                // Nom phonétique de la lettre (visible uniquement iPad)
                Text(name)
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(Color.accentMain)
                    .accessibilityHidden(true)
            }

            // Footer : index + (sur iPad) range de versets
            HStack(spacing: 6) {
                Text("\(index)")
                    .font(.caption2.weight(.medium))
                    .foregroundStyle(.secondary)

                if isRegular, let verseStart, let verseEnd {
                    Text("·")
                        .font(.caption2)
                        .foregroundStyle(.tertiary)
                    Text("v. \(verseStart)–\(verseEnd)")
                        .font(.caption2)
                        .foregroundStyle(.tertiary)
                }
            }
        }
        .frame(maxWidth: .infinity, minHeight: minHeight)
        .padding(.vertical, isRegular ? 16 : 8)
        .background(Color.bgSurface)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .stroke(Color.dividerToken.opacity(0.4), lineWidth: 0.5)
        )
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(accessibilityLabel)
        .accessibilityHint("Affiche la section correspondante du Tehilim 119")
        .accessibilityAddTraits(.isButton)
    }

    private var accessibilityLabel: String {
        if let name, let verseStart, let verseEnd {
            return "Section \(index), lettre \(letter) \(name), versets \(verseStart) à \(verseEnd)"
        }
        return "Section \(index), lettre \(letter)"
    }
}
