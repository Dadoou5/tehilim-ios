import SwiftUI

/// Écran de chargement affiché au démarrage de l'app (~2.6 s).
///
/// V2.2.b — animation « écriture » : les deux titres s'écrivent lettre à
/// lettre, simultanément — l'hébreu de droite à gauche (sens d'écriture),
/// le latin de gauche à droite. Chaque glyphe arrive comme un trait de
/// plume : flou d'encre, léger excès d'échelle, inclinaison calligraphique
/// (latin), puis se pose sur la ligne avec un ressort. Une fois les mots
/// écrits, le halo pulse, le filet se trace et les dédicaces montent.
///
/// Chronologie (budget 2.6 s avant le fondu, cf. TehilimApp) :
///   0.00 → 0.95 s  תהילים s'écrit (6 lettres, cadence 120 ms)
///   0.25 → 1.25 s  Tehilim s'écrit (révélation au masque, plume invisible)
///   1.20 s         halo doré (pulse infini) + le filet se trace
///   1.30 s         dédicace hébraïque
///   1.55 s         dédicace latine
struct SplashView: View {
    private static let hebrewLetters = Array("תהילים")

    @State private var written = false
    @State private var latinProgress: CGFloat = 0
    @State private var visibleDivider = false
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

                hebrewTitle
                latinTitle

                Spacer()

                dedication
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Tehilim. Pour l'élévation de l'âme de Johann Meïr ben Sarah Bouganim")
        .onAppear { animateIn() }
    }

    // MARK: - Titres « écrits » lettre à lettre

    /// תהילים — l'HStack est verrouillé en RTL : l'index 0 (ת) se place à
    /// droite et le délai croissant par index écrit donc de droite à gauche.
    private var hebrewTitle: some View {
        HStack(spacing: 0) {
            ForEach(Self.hebrewLetters.indices, id: \.self) { i in
                Text(verbatim: String(Self.hebrewLetters[i]))
                    .font(.custom("FrankRuhlLibre-Regular", size: 96))
                    .foregroundStyle(Color.accentMain)
                    .shadow(color: Color.accentMain.opacity(glowing ? 0.25 : 0.0), radius: 12)
                    .opacity(written ? 1 : 0)
                    .blur(radius: written ? 0 : 8)
                    .scaleEffect(written ? 1 : 1.3, anchor: .bottom)
                    .offset(y: written ? 0 : 14)
                    .animation(
                        .spring(response: 0.55, dampingFraction: 0.78)
                            .delay(Double(i) * 0.12),
                        value: written
                    )
            }
        }
        .environment(\.layoutDirection, .rightToLeft)
    }

    /// Tehilim — calligraphie Pinyon Script écrite de gauche à droite.
    /// Pinyon est une cursive contextuelle : découpée lettre à lettre, le
    /// shaping CoreText se dégrade (liaisons perdues). Le mot est donc rendu
    /// ENTIER (ligatures parfaites) et révélé par un masque dégradé qui
    /// balaie de gauche à droite — la « plume invisible » qui écrit. Le bord
    /// du masque est doux (36 pt) : l'encre semble apparaître, pas surgir.
    private var latinTitle: some View {
        Text(verbatim: "Tehilim")
            .font(.custom("PinyonScript-Regular", size: 64))
            .foregroundStyle(Color.accentMain)
            .mask(alignment: .leading) {
                GeometryReader { geo in
                    let pen: CGFloat = 36
                    let reveal = latinProgress * (geo.size.width + pen)
                    HStack(spacing: 0) {
                        Rectangle()
                            .frame(width: max(0, reveal - pen))
                        LinearGradient(
                            colors: [.black, .clear],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                        .frame(width: min(pen, max(0, reveal)))
                        Spacer(minLength: 0)
                    }
                }
            }
            // La calligraphie reste LTR même sous UI hébreu (RTL global).
            .environment(\.layoutDirection, .leftToRight)
    }

    // MARK: - Dédicace

    private var dedication: some View {
        VStack(spacing: 6) {
            // Le filet se « trace » du centre vers les bords (0 → 60 pt).
            Rectangle()
                .fill(Color.dividerToken)
                .frame(width: visibleDivider ? 60 : 0, height: 0.5)
                .opacity(visibleDivider ? 1 : 0)

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

    // MARK: - Chorégraphie

    private func animateIn() {
        // Les délais par lettre (hébreu) sont portés par les `.animation(value:)`.
        written = true

        // La plume écrit « Tehilim » en ~1 s, départ décalé de 0.25 s pour
        // que les deux mots finissent de s'écrire presque ensemble.
        withAnimation(.easeInOut(duration: 1.0).delay(0.25)) {
            latinProgress = 1
        }

        withAnimation(.easeInOut(duration: 1.6).delay(1.2).repeatForever(autoreverses: true)) {
            glowing = true
        }
        withAnimation(.easeOut(duration: 0.5).delay(1.2)) {
            visibleDivider = true
        }
        withAnimation(.easeOut(duration: 0.7).delay(1.3)) {
            visibleDedicationHebrew = true
        }
        withAnimation(.easeOut(duration: 0.7).delay(1.55)) {
            visibleDedicationLatin = true
        }
    }
}

#Preview {
    SplashView()
}
