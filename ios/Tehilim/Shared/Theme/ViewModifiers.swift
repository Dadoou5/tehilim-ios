import SwiftUI

extension View {
    /// Remplace le fond système des List / Form / ScrollView par `Color.bgPrimary`.
    /// Permet d'avoir le bleu d'eau (mode clair) ou le bleu nuit (mode sombre)
    /// uniformément, au lieu du gris système iOS.
    func appBackground() -> some View {
        self
            .scrollContentBackground(.hidden)
            .background(Color.bgPrimary)
    }

    /// Style "carte" du design system : fond `bgSurface`, coins arrondis 12 pt,
    /// ombre douce pour donner de la profondeur sans charge visuelle.
    func appCard(cornerRadius: CGFloat = 12) -> some View {
        self
            .background(Color.bgSurface)
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
            .shadow(color: Color.black.opacity(0.04), radius: 8, x: 0, y: 2)
    }
}
