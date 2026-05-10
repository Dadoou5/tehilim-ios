import SwiftUI

/// Écran de chargement affiché au démarrage de l'app, environ 1.8 s.
/// Hébreu en grand au-dessus, "Tehilim" en cursive Snell Roundhand en-dessous.
struct SplashView: View {
    @State private var visibleHebrew = false
    @State private var visibleLatin = false
    @State private var glowing = false

    var body: some View {
        ZStack {
            // Dégradé bleu d'eau, raccord avec l'app
            LinearGradient(
                colors: [Color.bgPrimary, Color.bgElevated],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            VStack(spacing: 28) {
                // תהילים — hébreu décoratif, RTL
                Text("תהילים")
                    .font(.custom("ArialHebrew-Bold", size: 88))
                    .foregroundStyle(Color.accentMain)
                    .environment(\.layoutDirection, .rightToLeft)
                    .shadow(color: Color.accentMain.opacity(glowing ? 0.25 : 0.0), radius: 12)
                    .opacity(visibleHebrew ? 1 : 0)
                    .scaleEffect(visibleHebrew ? 1 : 0.88)

                // Tehilim — Latin, calligraphie Snell Roundhand
                Text("Tehilim")
                    .font(.custom("SnellRoundhand-Bold", size: 64))
                    .foregroundStyle(Color.accentMain)
                    .opacity(visibleLatin ? 1 : 0)
                    .offset(y: visibleLatin ? 0 : 16)
            }
            .padding(.horizontal, 24)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Tehilim")
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
    }
}

#Preview {
    SplashView()
}
