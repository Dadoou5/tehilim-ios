import SwiftUI

/// Carte visuelle d'un verset, prête à être rendue en image (1080×1080) pour le partage.
/// V1.9.0 : la langue de la traduction et son attribution suivent désormais la
/// préférence active de l'utilisateur (FR/EN), au lieu de toujours afficher le FR.
struct VerseShareCard: View {
    let psalm: Psalm
    let verse: Verse
    let translationLang: TranslationLanguage

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

                if let translation = verse.translation(for: translationLang), !translation.isEmpty {
                    Text(translation)
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
                    Text(translationLang.sourceCredit)
                        .font(.system(size: 18))
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
    static func render(
        psalm: Psalm,
        verse: Verse,
        translationLang: TranslationLanguage,
        colorScheme: ColorScheme = .light
    ) -> UIImage? {
        let card = VerseShareCard(
            psalm: psalm,
            verse: verse,
            translationLang: translationLang
        )
        .environment(\.colorScheme, colorScheme)
        let renderer = ImageRenderer(content: card)
        renderer.scale = 2 // retina
        return renderer.uiImage
    }
}
