import SwiftUI

struct HomeView: View {
    @Binding var path: NavigationPath
    @EnvironmentObject private var container: AppContainer
    @EnvironmentObject private var router: TabRouter
    @EnvironmentObject private var favorites: FavoritesStore
    @StateObject private var viewModel = HomeViewModel()
    @Environment(\.horizontalSizeClass) private var hSize

    @State private var searchPresented = false
    @State private var presentedPrayer: Prayer.Kind? = nil

    private var exploreColumns: [GridItem] {
        Array(
            repeating: GridItem(.flexible(), spacing: 12),
            count: AdaptiveLayout.exploreColumnCount(for: hSize)
        )
    }

    var body: some View {
        NavigationStack(path: $path) {
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {

                    HebrewDateBanner()

                    if let lastId = viewModel.lastReadId, let psalm = container.psalmRepository.psalm(id: lastId) {
                        SectionHeader(title: "Reprendre la lecture")
                        NavigationLink(destination: PsalmDetailView(psalmId: psalm.id)) {
                            ResumeCard(psalm: psalm)
                        }
                        .buttonStyle(.plain)
                    }

                    SectionHeader(title: "Mes favoris")
                    Button {
                        router.go(.psalms, psalmsSegment: 2, resetPath: true)
                    } label: {
                        FavoritesShortcutCard(count: favorites.ids.count)
                    }
                    .buttonStyle(.plain)

                    SectionHeader(title: "Tehilim du jour", subtitle: viewModel.dailyMode.label)
                    Button {
                        router.go(.daily, resetPath: true)
                    } label: {
                        DailySummaryCard(psalmIds: viewModel.todayPsalms)
                    }
                    .buttonStyle(.plain)

                    SectionHeader(title: "Explorer")
                    LazyVGrid(columns: exploreColumns, spacing: 12) {
                        Button { router.go(.psalms, psalmsSegment: 0, resetPath: true) } label: {
                            ExploreCard(symbol: "books.vertical", title: "5 livres")
                        }
                        Button { router.go(.lifeCases, resetPath: true) } label: {
                            ExploreCard(symbol: "heart.text.square", title: "Cas de la vie")
                        }
                        NavigationLink(destination: Psalm119HomeView()) {
                            ExploreCard(symbol: "textformat", title: "119 - AlphaBeta")
                        }
                        Button { router.go(.psalms, psalmsSegment: 1, resetPath: true) } label: {
                            ExploreCard(symbol: "list.bullet", title: "Tous (1–150)")
                        }
                        Button { presentedPrayer = .before } label: {
                            ExploreCard(symbol: Prayer.Kind.before.symbol, title: "Prière avant")
                        }
                        Button { presentedPrayer = .after } label: {
                            ExploreCard(symbol: Prayer.Kind.after.symbol, title: "Prière après")
                        }
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, AdaptiveLayout.horizontalPadding(for: hSize))
                .padding(.vertical, 16)
                .readingWidth()
            }
            .background(Color.bgPrimary)
            .navigationTitle("Tehilim")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { searchPresented = true } label: {
                        Image(systemName: "magnifyingglass")
                    }
                    .accessibilityLabel("Rechercher")
                }
            }
            .sheet(isPresented: $searchPresented) {
                SearchView()
            }
            .sheet(item: $presentedPrayer) { kind in
                PrayerView(prayer: Prayer.of(kind))
            }
            .onAppear { viewModel.refresh(container: container) }
        }
    }
}

@MainActor
final class HomeViewModel: ObservableObject {
    @Published var lastReadId: Int? = nil
    @Published var todayPsalms: [Int] = []
    @Published var dailyMode: DailyMode = .monthly

    func refresh(container: AppContainer) {
        let prefs = container.preferences
        lastReadId = prefs.lastReadPsalmId == 0 ? nil : prefs.lastReadPsalmId
        dailyMode = prefs.dailyMode
        todayPsalms = container.dailyEngine.psalmsForToday(mode: prefs.dailyMode)
    }
}

private struct HebrewDateBanner: View {
    let date = HebrewDateFormatter.formatted()
    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text("\(date.dayOfWeek) · \(date.transliterated)")
                .font(.subheadline.weight(.medium))
                .foregroundStyle(.secondary)
            Text(date.hebrew)
                .font(.subheadline)
                .foregroundStyle(.tertiary)
                .environment(\.layoutDirection, .rightToLeft)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(date.dayOfWeek) \(date.transliterated). Calendrier hébraïque : \(date.hebrew).")
    }
}

private struct SectionHeader: View {
    let title: LocalizedStringKey
    /// Sous-titre déjà localisé (souvent une valeur dynamique comme "Cycle mensuel").
    var subtitle: String? = nil
    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title)
                .font(.headline)
                .accessibilityAddTraits(.isHeader)
            if let subtitle {
                Text(subtitle).font(.caption).foregroundStyle(.secondary)
            }
        }
    }
}

private struct ResumeCard: View {
    let psalm: Psalm
    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text("Tehilim \(psalm.id) · \(psalm.hebrewNumber)")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                if let hebrewTitle = psalm.hebrewTitle {
                    Text(hebrewTitle)
                        .font(.title3.weight(.semibold))
                } else {
                    Text("Reprendre")
                        .font(.title3.weight(.semibold))
                }
            }
            Spacer()
            Image(systemName: "arrow.right").foregroundStyle(.secondary)
        }
        .padding(16)
        .appCard()
    }
}

private struct FavoritesShortcutCard: View {
    let count: Int
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: count > 0 ? "heart.fill" : "heart")
                .font(.title2)
                .foregroundStyle(Color.accentMain)
                .frame(width: 32)
            VStack(alignment: .leading, spacing: 2) {
                primary.font(.headline)
                secondary.font(.caption).foregroundStyle(.secondary)
            }
            Spacer()
            Image(systemName: "chevron.right").foregroundStyle(.tertiary)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.bgSurface)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(count > 0 ? "Voir mes \(count) favoris" : "Aucun favori")
        .accessibilityHint("Ouvre la liste des Tehilim sauvegardés")
        .accessibilityAddTraits(.isButton)
    }

    @ViewBuilder
    private var primary: some View {
        switch count {
        case 0:  Text("Aucun favori")
        case 1:  Text("1 Tehilim sauvegardé")
        default: Text("\(count) Tehilim sauvegardés")
        }
    }

    @ViewBuilder
    private var secondary: some View {
        if count == 0 {
            Text("Tape ♡ sur un Tehilim pour l'ajouter ici")
        } else {
            Text("Voir la liste")
        }
    }
}

private struct DailySummaryCard: View {
    let psalmIds: [Int]
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if psalmIds.isEmpty {
                Text("Aucun Tehilim défini pour ce mode.").foregroundStyle(.secondary)
            } else {
                Text(psalmIds.prefix(8).map(String.init).joined(separator: " · "))
                    .font(.callout)
                if psalmIds.count > 8 { Text("+\(psalmIds.count - 8)").font(.caption).foregroundStyle(.secondary) }
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .appCard()
    }
}

private struct ExploreCard: View {
    let symbol: String
    let title: LocalizedStringKey
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Image(systemName: symbol)
                .font(.title2)
                .foregroundStyle(Color.accentMain)
            Text(title).font(.headline)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .appCard()
    }
}

