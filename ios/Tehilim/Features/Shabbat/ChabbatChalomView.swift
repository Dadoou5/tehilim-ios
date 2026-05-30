import SwiftUI

/// Écran affiché pendant Chabbat à la place du contenu de l'app (et repris par
/// le widget). « שבת שלום » en hébreu + « Chabbat Chalom » en français, avec
/// l'heure de fin de Chabbat calculée selon la position, une bougie animée, et
/// un bouton discret « Continuer quand même » (échappatoire de session).
struct ChabbatChalomView: View {
    /// Début de Chabbat (allumage des bougies). Optionnel.
    let startsAt: Date?
    /// Fin de Chabbat (Havdala). Optionnel : si nil, on n'affiche pas l'heure.
    let endsAt: Date?
    /// Action déclenchée par « Continuer quand même ».
    var onContinue: () -> Void

    @State private var flameFlicker = false
    @State private var visibleFlame = false
    @State private var visibleHebrew = false
    @State private var visibleLatin = false
    @State private var visibleInfo = false
    @State private var glowing = false

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color.bgPrimary, Color.bgElevated],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            VStack(spacing: 22) {
                Spacer()

                // Bougie animée (flamme qui vacille)
                Image(systemName: "flame.fill")
                    .font(.system(size: 72))
                    .foregroundStyle(Color.accentMain)
                    .shadow(color: Color.accentMain.opacity(glowing ? 0.55 : 0.15),
                            radius: glowing ? 26 : 10)
                    .scaleEffect(x: flameFlicker ? 0.94 : 1.04,
                                 y: flameFlicker ? 1.06 : 0.96,
                                 anchor: .bottom)
                    .rotationEffect(.degrees(flameFlicker ? -2.5 : 2.5), anchor: .bottom)
                    .opacity(visibleFlame ? 1 : 0)
                    .scaleEffect(visibleFlame ? 1 : 0.6)
                    .accessibilityHidden(true)

                // שבת שלום — hébreu
                Text("שבת שלום")
                    .font(.custom("FrankRuhlLibre-Regular", size: 64))
                    .foregroundStyle(Color.accentMain)
                    .environment(\.layoutDirection, .rightToLeft)
                    .shadow(color: Color.accentMain.opacity(glowing ? 0.25 : 0), radius: 12)
                    .opacity(visibleHebrew ? 1 : 0)
                    .scaleEffect(visibleHebrew ? 1 : 0.85)

                // Chabbat Chalom — français
                Text("Chabbat Chalom")
                    .font(.custom("PinyonScript-Regular", size: 46))
                    .foregroundStyle(.primary)
                    .opacity(visibleLatin ? 1 : 0)
                    .offset(y: visibleLatin ? 0 : 14)

                // Horaires de début et de fin de Chabbat
                if startsAt != nil || endsAt != nil {
                    VStack(spacing: 10) {
                        if let startsAt {
                            timeRow(title: "Début du Chabbat", date: startsAt)
                        }
                        if let endsAt {
                            timeRow(title: "Fin du Chabbat", date: endsAt)
                        }
                        Text("Horaires calculés selon ta position (bougies : coucher − 18 min · fin : sortie des étoiles).")
                            .font(.caption2)
                            .foregroundStyle(.tertiary)
                            .multilineTextAlignment(.center)
                            .padding(.top, 2)
                    }
                    .padding(.top, 8)
                    .opacity(visibleInfo ? 1 : 0)
                    .offset(y: visibleInfo ? 0 : 10)
                    .padding(.horizontal, 32)
                }

                Spacer()

                // Échappatoire discrète
                Button(action: onContinue) {
                    Text("Continuer quand même")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .underline()
                }
                .opacity(visibleInfo ? 1 : 0)
                .padding(.bottom, 32)
                .accessibilityHint("Ouvre l'application malgré le mode Chabbat")
            }
            .padding(.horizontal, 24)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel(
            "Chabbat Chalom"
            + (startsAt != nil ? ". Début du Chabbat \(endLabel(startsAt!))" : "")
            + (endsAt != nil ? ". Fin du Chabbat \(endLabel(endsAt!))" : "")
        )
        .onAppear { animateIn() }
    }

    /// Ligne « LIBELLÉ : date · heure ».
    @ViewBuilder
    private func timeRow(title: LocalizedStringKey, date: Date) -> some View {
        VStack(spacing: 2) {
            Text(title)
                .font(.caption.weight(.semibold))
                .foregroundStyle(.secondary)
                .textCase(.uppercase)
            Text(endLabel(date))
                .font(.headline)
                .foregroundStyle(.primary)
                .multilineTextAlignment(.center)
        }
    }

    /// Ex. « samedi 31 mai · 22:14 ».
    private func endLabel(_ date: Date) -> String {
        date.formatted(.dateTime.weekday(.wide).day().month(.wide).locale(AppLocale.locale)) +
        " · " +
        date.formatted(.dateTime.hour().minute().locale(AppLocale.locale))
    }

    private func animateIn() {
        withAnimation(.easeOut(duration: 0.6)) { visibleFlame = true }
        withAnimation(.easeInOut(duration: 0.45).repeatForever(autoreverses: true)) { flameFlicker = true }
        withAnimation(.easeInOut(duration: 1.8).delay(0.3).repeatForever(autoreverses: true)) { glowing = true }
        withAnimation(.easeOut(duration: 0.8).delay(0.3)) { visibleHebrew = true }
        withAnimation(.easeOut(duration: 0.8).delay(0.6)) { visibleLatin = true }
        withAnimation(.easeOut(duration: 0.7).delay(1.0)) { visibleInfo = true }
    }
}

#Preview {
    ChabbatChalomView(
        startsAt: Date().addingTimeInterval(-3600 * 19),
        endsAt: Date().addingTimeInterval(3600 * 5),
        onContinue: {}
    )
}
