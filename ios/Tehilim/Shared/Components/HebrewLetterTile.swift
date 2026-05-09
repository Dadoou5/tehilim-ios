import SwiftUI

struct HebrewLetterTile: View {
    let letter: String
    let index: Int

    var body: some View {
        VStack(spacing: 4) {
            Text(letter)
                .font(.system(size: 44, weight: .regular, design: .serif))
            Text("\(index)")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, minHeight: 88) // > 44 pt cible tactile
        .background(.thinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Section \(index), lettre \(letter)")
        .accessibilityHint("Affiche la section correspondante du Tehilim 119")
    }
}
