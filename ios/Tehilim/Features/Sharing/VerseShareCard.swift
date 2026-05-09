import SwiftUI

/// Carte visuelle d'un verset, prête à être rendue en image (1080×1080) pour le partage.
struct VerseShareCard: View {
    let psalm: Psalm
    let verse: Verse

    var body: some View {
        ZStack {
            Color.bgPrimary
            VStack(spacing: 24) {
                Text("Tehilim \(psalm.id) · \(psalm.hebrewNumber)")
                    .font(.system(size: 32, weight: .semibold, design: .serif))
                    .foregroundStyle(Color.accentMain)

                Text(verse.hebrew)
                    .font(.system(size: 48, weight: .regular, design: .default))
                    .multilineTextAlignment(.trailing)
                    .environment(\.layoutDirection, .rightToLeft)
                    .lineSpacing(12)
                    .foregroundStyle(.primary)

                if let fr = verse.translationFR, !fr.isEmpty {
                    Text(fr)
                        .font(.system(size: 32, weight: .regular, design: .serif))
                        .italic()
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                }

                Spacer(minLength: 0)

                VStack(spacing: 6) {
                    Text("Verset \(verse.number)")
                        .font(.system(size: 24, weight: .medium))
                        .foregroundStyle(.secondary)
                    Text("Traduction : Beth Loubavitch")
                        .font(.system(size: 18))
                        .foregroundStyle(.tertiary)
                    Text("le-tehilim.online")
                        .font(.system(size: 16))
                        .foregroundStyle(.tertiary)
                }
            }
            .padding(64)
        }
        .frame(width: 1080, height: 1080)
    }
}

/// Génère une UIImage rendable de la carte au moment du partage.
@MainActor
enum VerseShareImageRenderer {
    static func render(psalm: Psalm, verse: Verse, colorScheme: ColorScheme = .light) -> UIImage? {
        let card = VerseShareCard(psalm: psalm, verse: verse)
            .environment(\.colorScheme, colorScheme)
        let renderer = ImageRenderer(content: card)
        renderer.scale = 2 // retina
        return renderer.uiImage
    }
}
