import SwiftUI

/// Écran de chargement affiché au démarrage de l'app, environ 1.8 s.
struct SplashView: View {
    @State private var visibleHebrew = false
    @State private var visibleLatin = false
    @State private var visibleDedicationHebrew = false
    @State private var visibleDedicationLatin = false
    @State private var glowing = false

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color.bgPrimary, Color.bgElevated],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            VStack(spacing: 24) {
                Spacer()

                // תהילים — hébreu décoratif, RTL
                Text("תהילים")
                    .font(.custom("FrankRuhlLibre-Regular", size: 96))
                    .foregroundStyle(Color.accentMain)
                    .multilineTextAlignment(.center)
                    .environment(\.layoutDirection, .rightToLeft)
                    .shadow(color: Color.accentMain.opacity(glowing ? 0.25 : 0.0), radius: 12)
                    .opacity(visibleHebrew ? 1 : 0)
                    .scaleEffect(visibleHebrew ? 1 : 0.88)

                // Tehilim — Latin, calligraphie Pinyon Script (Google Fonts OFL).
                // V1.10.7 — remplace SnellRoundhand-Bold (Apple-only) pour parité Android.
                Text("Tehilim")
                    .font(.custom("PinyonScript-Regular", size: 64))
                    .foregroundStyle(Color.accentMain)
                    .opacity(visibleLatin ? 1 : 0)
                    .offset(y: visibleLatin ? 0 : 16)

                Spacer()

                // Dédicace en bas, plus discrète
                VStack(spacing: 6) {
                    Rectangle()
                        .fill(Color.dividerToken)
                        .frame(width: 60, height: 0.5)
                        .opacity(visibleDedicationHebrew ? 1 : 0)

                    Text("לעילוי נשמת ג׳והאן מאיר בן שרה בוגנים")
                        .font(.custom("FrankRuhlLibre-Regular", size: 16))
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                        .environment(\.layoutDirection, .rightToLeft)
                        .opacity(visibleDedicationHebrew ? 1 : 0)
                        .offset(y: visibleDedicationHebrew ? 0 : 8)

                    Text("Pour l'élévation de l'âme de Johann Meïr ben Sarah Bouganim")
                        .font(.system(size: 12, design: .serif).italic())
                        .foregroundStyle(.tertiary)
                        .multilineTextAlignment(.center)
                        .opacity(visibleDedicationLatin ? 1 : 0)
                        .offset(y: visibleDedicationLatin ? 0 : 8)
                }
                .padding(.horizontal, 32)
                .padding(.bottom, 40)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Tehilim. Pour l'élévation de l'âme de Johann Meïr ben Sarah Bouganim")
        .onAppear { animateIn() }
    }

    private func animateIn() {
        withAnimation(.easeOut(duration: 0.8)) {
            visibleHebrew = true
        }
        withAnimation(.easeOut(duration: 0.8).delay(0.35)) {
            visibleLatin = true
        }
        withAnimation(.easeInOut(duration: 1.6).delay(0.6).repeatForever(autoreverses: true)) {
            glowing = true
        }
        withAnimation(.easeOut(duration: 0.7).delay(0.9)) {
            visibleDedicationHebrew = true
        }
        withAnimation(.easeOut(duration: 0.7).delay(1.2)) {
            visibleDedicationLatin = true
        }
    }
}

#Preview {
    SplashView()
}
