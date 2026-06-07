import SwiftUI

/// Contrôle de taille du texte en lecture — A− / aperçu / A+. Ajuste la taille
/// du texte principal (hébreu/phonétique) et, si la traduction est affichée,
/// celle de la traduction, du même nombre de pas. Persisté via @AppStorage
/// (les mêmes réglages que l'écran Réglages → reste synchronisé).
struct ReadingTextSizeControl: View {
    @ObservedObject var prefs: Preferences
    /// La traduction est-elle visible ? (pour ajuster aussi sa taille)
    var includeTranslation: Bool

    private func step(_ delta: Int) {
        prefs.textSizeHebrew = prefs.textSizeHebrew.stepped(by: delta)
        if includeTranslation {
            prefs.textSizeFR = prefs.textSizeFR.stepped(by: delta)
        }
    }

    var body: some View {
        VStack(spacing: 12) {
            Text("Taille du texte").font(.subheadline.weight(.semibold))

            HStack(spacing: 0) {
                Button { step(-1) } label: {
                    Image(systemName: "textformat.size.smaller")
                        .font(.title3).frame(maxWidth: .infinity, minHeight: 44)
                }
                .disabled(prefs.textSizeHebrew.isSmallest)
                .accessibilityLabel("Réduire le texte")

                Divider().frame(height: 28)

                Text("Aa")
                    .font(.system(size: 17 * prefs.textSizeHebrew.scale, weight: .semibold))
                    .frame(width: 64)
                    .accessibilityHidden(true)

                Divider().frame(height: 28)

                Button { step(1) } label: {
                    Image(systemName: "textformat.size.larger")
                        .font(.title3).frame(maxWidth: .infinity, minHeight: 44)
                }
                .disabled(prefs.textSizeHebrew.isLargest)
                .accessibilityLabel("Agrandir le texte")
            }
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(Color.dividerToken.opacity(0.4), lineWidth: 0.5)
            )

            Text(prefs.textSizeHebrew.label)
                .font(.caption).foregroundStyle(.secondary)
        }
        .padding(16)
        .frame(width: 260)
    }
}
