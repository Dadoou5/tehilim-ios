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

                VStack(spacing: 14) {
                    Text("Verset \(verse.number)")
                        .font(.system(size: 24, weight: .medium))
                        .foregroundStyle(.secondary)

                    // Icône de lancement de l'app.
                    Image("ChainBrandLogo")
                        .resizable()
                        .interpolation(.high)
                        .scaledToFit()
                        .frame(width: 88, height: 88)
                        .clipShape(RoundedRectangle(cornerRadius: 19, style: .continuous))

                    // Dédicace לעילוי נשמת (identique à l'encart de chaque Tehilim).
                    VStack(spacing: 4) {
                        Text("לעילוי נשמת ג׳והאן מאיר בן שרה בוגנים")
                            .font(.system(size: 22))
                            .environment(\.layoutDirection, .rightToLeft)
                            .multilineTextAlignment(.center)
                        Text("Pour l'élévation de l'âme de Johann Meïr ben Sarah Bouganim")
                            .font(.system(size: 20, design: .serif))
                            .italic()
                            .multilineTextAlignment(.center)
                    }
                    .foregroundStyle(.secondary)
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
