import SwiftUI

struct OnboardingView: View {
    let onFinish: () -> Void

    @State private var page: Int = 0

    private let pages: [OnboardingPage] = [
        .init(
            symbol: "book.closed.fill",
            title: "Bienvenue",
            body: "L'intégralité des 150 Tehilim, en hébreu vocalisé et en français.\n\nTraduction : Beth Loubavitch.\nTexte hébreu : Sefaria — Miqra according to the Masorah."
        ),
        .init(
            symbol: "calendar",
            title: "Au quotidien",
            body: "Retrouve les Tehilim du jour, ceux associés à un moment de vie (santé, mariage, deuil, voyage…), et 17 cas de la vie organisés en 4 sections."
        ),
        .init(
            symbol: "slider.horizontal.3",
            title: "À ton rythme",
            body: "Choisis la taille du texte, le mode hébreu ou phonétique, l'affichage de la traduction, l'heure du rappel quotidien — tout est configurable dans Réglages."
        ),
    ]

    var body: some View {
        VStack(spacing: 0) {
            TabView(selection: $page) {
                ForEach(Array(pages.enumerated()), id: \.offset) { idx, p in
                    OnboardingPageView(page: p)
                        .tag(idx)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            .frame(maxHeight: .infinity)

            // Indicateur de page
            HStack(spacing: 8) {
                ForEach(0..<pages.count, id: \.self) { i in
                    Circle()
                        .fill(i == page ? Color.accentMain : Color.dividerToken)
                        .frame(width: 8, height: 8)
                }
            }
            .padding(.bottom, 24)
            .accessibilityHidden(true)

            // Boutons
            HStack {
                Button("Passer") { onFinish() }
                    .opacity(page == pages.count - 1 ? 0 : 1)
                    .disabled(page == pages.count - 1)

                Spacer()

                Button {
                    if page < pages.count - 1 {
                        withAnimation { page += 1 }
                    } else {
                        onFinish()
                    }
                } label: {
                    Text(page == pages.count - 1 ? "Commencer" : "Suivant")
                        .fontWeight(.semibold)
                        .frame(minWidth: 120)
                }
                .buttonStyle(.borderedProminent)
                .tint(.accentMain)
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 32)
        }
        .background(Color.bgPrimary)
    }
}

private struct OnboardingPage {
    let symbol: String
    let title: LocalizedStringKey
    let body: LocalizedStringKey
}

private struct OnboardingPageView: View {
    let page: OnboardingPage

    var body: some View {
        VStack(spacing: 32) {
            Spacer()
            Image(systemName: page.symbol)
                .font(.system(size: 88, weight: .light))
                .foregroundStyle(Color.accentMain)
                .accessibilityHidden(true)
            VStack(spacing: 16) {
                Text(page.title)
                    .font(.largeTitle.weight(.bold))
                    .multilineTextAlignment(.center)
                Text(page.body)
                    .font(.body)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
                    .lineSpacing(4)
            }
            Spacer()
            Spacer()
        }
        .padding()
    }
}
