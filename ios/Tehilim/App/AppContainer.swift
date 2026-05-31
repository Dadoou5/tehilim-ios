import Foundation

/// Conteneur d'injection de dépendances simple.
/// Centralise les services pour éviter la duplication et faciliter les tests.
final class AppContainer: ObservableObject {
    static let shared = AppContainer()

    let contentLoader: ContentLoading
    let psalmRepository: PsalmRepository
    let lifeCaseRepository: LifeCaseRepository
    let psalm119Repository: Psalm119Repository
    let dailyEngine: DailyEngine
    let searchInterpreter: SearchInterpreter
    let favorites: FavoritesStore
    let savedPrayers: SavedPrayerStore
    let preferences: Preferences

    /// Prière reçue via lien partagé (`tehilim://prayer` ou Universal Link),
    /// en attente d'aperçu d'import. Capté au niveau de l'App (toujours monté,
    /// même pendant le splash) pour ne pas perdre le lien au cold-start, puis
    /// présenté par `RootTabView` dès qu'il apparaît.
    @Published var pendingPrayerImport: PrayerShareLink.Payload?

    init(
        contentLoader: ContentLoading = BundledContentLoader()
    ) {
        self.contentLoader = contentLoader
        self.preferences = Preferences()
        self.favorites = FavoritesStore()
        self.savedPrayers = SavedPrayerStore()

        do {
            let psalms = try contentLoader.loadPsalms()
            let cases = try contentLoader.loadLifeCases()
            let sections = try contentLoader.loadPsalm119Sections()
            let rules = try contentLoader.loadDailyRules()

            self.psalmRepository = PsalmRepository(psalms: psalms)
            self.lifeCaseRepository = LifeCaseRepository(cases: cases)
            self.psalm119Repository = Psalm119Repository(sections: sections)
            self.dailyEngine = DailyEngine(rules: rules)
        } catch {
            // Fallback minimal pour ne JAMAIS planter au lancement.
            assertionFailure("Failed to load bundled content: \(error)")
            self.psalmRepository = PsalmRepository(psalms: [])
            self.lifeCaseRepository = LifeCaseRepository(cases: [])
            self.psalm119Repository = Psalm119Repository(sections: [])
            self.dailyEngine = DailyEngine(rules: DailyRules.empty)
        }

        self.searchInterpreter = SearchInterpreter(repository: self.psalmRepository)
    }
}
