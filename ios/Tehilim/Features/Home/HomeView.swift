import SwiftUI

struct HomeView: View {
    @Binding var path: NavigationPath
    @EnvironmentObject private var container: AppContainer
    @EnvironmentObject private var router: TabRouter
    @EnvironmentObject private var favorites: FavoritesStore
    @EnvironmentObject private var savedPrayers: SavedPrayerStore
    @Environment(\.horizontalSizeClass) private var hSize

    /// V2.1.b — observation directe via @AppStorage pour que la carte
    /// « Reprendre la lecture » et le bloc « Tehilim du jour » se mettent à
    /// jour instantanément quand l'utilisateur ouvre un Tehilim ou change le
    /// mode dans Réglages (avant, le HomeViewModel n'était rafraîchi que sur
    /// `onAppear` qui ne firait pas si Home restait monté en arrière-plan).
    @AppStorage("pref.lastReadPsalmId") private var lastReadIdStored: Int = 0
    @AppStorage("pref.dailyMode", store: AppGroup.userDefaults) private var dailyModeRaw: String = DailyMode.monthly.rawValue

    private var lastReadId: Int? { lastReadIdStored == 0 ? nil : lastReadIdStored }
    private var dailyMode: DailyMode { DailyMode(rawValue: dailyModeRaw) ?? .monthly }
    private var todayPsalms: [Int] {
        container.dailyEngine.psalmsForToday(mode: dailyMode)
    }

    @State private var searchPresented = false
    @State private var presentedPrayer: Prayer.Kind? = nil

    /// Grille « Explorer » adaptative : 2 colonnes sur iPhone, et autant que la
    /// largeur le permet sur iPad (3 en portrait, 5–6 en paysage).
    private var exploreColumns: [GridItem] {
        AdaptiveLayout.adaptiveColumns(for: hSize, compactMin: 150, regularMin: 190)
    }

    /// Raccourcis Favoris / Tehilim du jour : empilés sur iPhone (compactMin
    /// > toute largeur iPhone → 1 colonne), côte-à-côte sur iPad. regularMin à
    /// 380 → exactement 2 colonnes en portrait ET paysage iPad (on n'a que 2
    /// items : un minimum plus bas créerait une 3ᵉ colonne vide en paysage).
    private var shortcutColumns: [GridItem] {
        AdaptiveLayout.adaptiveColumns(for: hSize, compactMin: 2000, regularMin: 380, spacing: 16)
    }

    // MARK: - Azcara à venir (V2.3)

    /// Une azcara dont la date tombe dans les 7 prochains jours (fenêtre inclusive
    /// J−7 → J). Plusieurs prières peuvent matcher sur la période.
    private struct UpcomingAzcara: Identifiable {
        let intent: SavedPrayerIntent
        let date: Date
        let days: Int
        var id: UUID { intent.id }
    }

    private var upcomingAzcarot: [UpcomingAzcara] {
        let cal = Calendar.current
        let today = cal.startOfDay(for: Date())
        return savedPrayers.intents.compactMap { intent -> UpcomingAzcara? in
            guard let death = intent.civilDateOfDeath,
                  let next = MemorialCalculator.nextYahrzeit(deathCivil: death) else { return nil }
            let azDay = cal.startOfDay(for: next)
            let days = cal.dateComponents([.day], from: today, to: azDay).day ?? -1
            guard (0...7).contains(days) else { return nil }
            return UpcomingAzcara(intent: intent, date: next, days: days)
        }
        .sorted { $0.date < $1.date }
    }

    @ViewBuilder
    private var azcaraSection: some View {
        let list = upcomingAzcarot
        if !list.isEmpty {
            VStack(alignment: .leading, spacing: 8) {
                SectionHeader(title: "Azcara à venir")
                ForEach(list) { item in
                    NavigationLink(destination: PersonalizedReadingListView(intent: item.intent, isSaved: true)) {
                        azcaraRow(item)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private func azcaraCountdown(_ days: Int) -> String {
        switch days {
        case 0:  return L("Aujourd'hui")
        case 1:  return L("Demain")
        default: return String(format: L("Dans %lld jours"), days)
        }
    }

    @ViewBuilder
    private func azcaraRow(_ item: UpcomingAzcara) -> some View {
        HStack(spacing: 14) {
            ZStack {
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(Color.accentMain.opacity(0.14))
                    .frame(width: 44, height: 44)
                Image(systemName: "flame.fill")
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(Color.accentMain)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(item.intent.hebrewSubject)
                    .font(.headline).foregroundStyle(.primary)
                    .lineLimit(1)
                Text("\(azcaraCountdown(item.days)) · \(item.date.formatted(.dateTime.day().month().locale(AppLocale.locale)))")
                    .font(.caption).foregroundStyle(.secondary)
            }
            Spacer()
            Image(systemName: "chevron.right")
                .font(.caption.weight(.semibold))
                .foregroundStyle(.tertiary)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.bgSurface)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(Color.dividerToken.opacity(0.4), lineWidth: 0.5)
        )
        .accessibilityElement(children: .combine)
    }

    var body: some View {
        NavigationStack(path: $path) {
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {

                    HebrewDateBanner()

                    azcaraSection

                    if let lastId = lastReadId, let psalm = container.psalmRepository.psalm(id: lastId) {
                        SectionHeader(title: "Reprendre la lecture")
                        NavigationLink(destination: PsalmDetailView(psalmId: psalm.id)) {
                            ResumeCard(psalm: psalm)
                        }
                        .buttonStyle(.plain)
                    }

                    // Favoris + Tehilim du jour : empilés sur iPhone, côte-à-côte
                    // sur iPad (paysage surtout) pour exploiter la largeur.
                    LazyVGrid(columns: shortcutColumns, alignment: .leading, spacing: 16) {
                        VStack(alignment: .leading, spacing: 8) {
                            SectionHeader(title: "Mes favoris")
                            Button {
                                router.go(.psalms, psalmsSegment: 2, resetPath: true)
                            } label: {
                                FavoritesShortcutCard(count: favorites.ids.count)
                            }
                            .buttonStyle(.plain)
                        }

                        VStack(alignment: .leading, spacing: 8) {
                            SectionHeader(title: "Tehilim du jour", subtitle: dailyMode.label)
                            Button {
                                router.go(.daily, resetPath: true)
                            } label: {
                                DailySummaryCard(psalmIds: todayPsalms)
                            }
                            .buttonStyle(.plain)
                        }
                    }

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
                        NavigationLink(destination: MyChainsView()) {
                            ExploreCard(symbol: "link", title: "Chaîne de Tehilim")
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
                .readingWidth(maxWidth: AdaptiveLayout.dashboardMaxWidth)
            }
            .background(Color.bgPrimary)
            .navigationTitle("Tehilim")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { searchPresented = true } label: {
                        Image(systemName: "magnifyingglass")
                    }
                    .keyboardShortcut("f", modifiers: .command)
                    .accessibilityLabel("Rechercher")
                }
            }
            .sheet(isPresented: $searchPresented) {
                SearchView()
            }
            .sheet(item: $presentedPrayer) { kind in
                PrayerView(prayer: Prayer.of(kind))
            }
        }
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
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
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
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
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

