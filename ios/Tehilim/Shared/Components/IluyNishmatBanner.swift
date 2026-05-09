import SwiftUI

/// Encart discret en tête de chaque psaume — לעילוי נשמת.
/// Hébreu (RTL) au-dessus du français, séparé par des filets fins.
struct IluyNishmatBanner: View {
    var body: some View {
        VStack(spacing: 4) {
            Text("לעילוי נשמת יוחנן מאיר בן שרה בוגנים")
                .font(.caption)
                .multilineTextAlignment(.center)
                .environment(\.layoutDirection, .rightToLeft)
            Text("Pour l'élévation de l'âme de Johann Meïr ben Sarah Bouganim")
                .font(.caption2)
                .italic()
                .multilineTextAlignment(.center)
        }
        .foregroundStyle(.secondary)
        .frame(maxWidth: .infinity)
        .padding(.vertical, 10)
        .padding(.horizontal, 16)
        .overlay(
            Rectangle().fill(Color.dividerToken).frame(height: 0.5),
            alignment: .top
        )
        .overlay(
            Rectangle().fill(Color.dividerToken).frame(height: 0.5),
            alignment: .bottom
        )
        .padding(.horizontal, 16)
        .padding(.top, 4)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Pour l'élévation de l'âme de Johann Meïr ben Sarah Bouganim")
    }
}
