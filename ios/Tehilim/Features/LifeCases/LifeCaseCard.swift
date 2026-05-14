import SwiftUI

/// Carte d'un cas de la vie — utilisée dans la grille adaptive de l'onglet « Cas de la vie ».
///
/// Pensée mobile-first : sur iPhone, deux cartes côte-à-côte (~180pt de large).
/// Sur iPad, trois cartes côte-à-côte avec plus d'air autour.
struct LifeCaseCard: View {
    let lifeCase: LifeCase

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Icône proéminente
            ZStack {
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(Color.accentMain.opacity(0.12))
                    .frame(width: 48, height: 48)
                Image(systemName: lifeCase.symbol)
                    .font(.title2.weight(.medium))
                    .foregroundStyle(Color.accentMain)
            }
            .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: 2) {
                Text(lifeCase.localizedTitle)
                    .font(.headline)
                    .foregroundStyle(.primary)
                    .multilineTextAlignment(.leading)
                    .fixedSize(horizontal: false, vertical: true)
                    .lineLimit(2)

                Text(lifeCase.localizedNote)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.leading)
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.top, 2)
            }

            Spacer(minLength: 0)

            // Footer compteur
            HStack(spacing: 4) {
                Image(systemName: "text.book.closed")
                    .font(.caption2)
                    .foregroundStyle(.tertiary)
                Text(psalmCountLabel)
                    .font(.caption2.weight(.medium))
                    .foregroundStyle(.tertiary)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, minHeight: 160, alignment: .topLeading)
        .background(Color.bgSurface)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(Color.dividerToken.opacity(0.4), lineWidth: 0.5)
        )
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(lifeCase.localizedTitle). \(psalmCountLabel)")
        .accessibilityHint("Ouvre les Tehilim associés à \(lifeCase.localizedTitle)")
        .accessibilityAddTraits(.isButton)
    }

    private var psalmCountLabel: String {
        let n = lifeCase.psalms.count
        return n == 1 ? "1 Tehilim" : "\(n) Tehilim"
    }
}
