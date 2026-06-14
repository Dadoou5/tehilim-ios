import Foundation

/// Conteneur d'injection de dépendances simple.
/// Centralise les services pour éviter la duplication et faciliter les tests.
final class AppContainer: ObservableObject {
    static let shared = AppContainer()

    let contentLoader: ContentLoading
    let psalmRepository: PsalmRepository
    let lifeCaseRepository: LifeCaseRepository
    let psalm119Repository: Psalm119Repository
    let commentaryRepository: CommentaryRepository
    let dailyEngine: DailyEngine
    let searchInterpreter: SearchInterpreter
    let favorites: FavoritesStore
    let savedPrayers: SavedPrayerStore
    let preferences: Preferences

    /// Feature « Chaîne de Tehilim » : accès Supabase + archive locale.
    let chains = ChainService()
    let chainArchive = ChainArchiveStore()

    /// Prière reçue via lien partagé (`tehilim://prayer` ou Universal Link),
    /// en attente d'aperçu d'import. Capté au niveau de l'App (toujours monté,
    /// même pendant le splash) pour ne pas perdre le lien au cold-start, puis
    /// présenté par `RootTabView` dès qu'il apparaît.
    @Published var pendingPrayerImport: PrayerShareLink.Payload?

    /// Chaîne reçue via lien partagé (`tehilim://chain?id=…` ou Universal Link
    /// `/c/?id=…`), en attente d'ouverture. Présentée par `RootTabView`.
    @Published var pendingChainOpen: String?

    /// **Point d'entrée unique** de tous les liens entrants (custom scheme,
    /// Universal Link), appelé par `.onOpenURL`, `.onContinueUserActivity` ET
    /// l'`AppDelegate` (qui capte les Universal Links de façon fiable, y
    /// compris au cold-start). Toujours exécuté sur le main thread.
    func routeIncomingURL(_ url: URL) {
        Task { @MainActor in
            if PrayerShareLink.isPrayerLink(url) {
                self.pendingPrayerImport = PrayerShareLink.payload(from: url)
                return
            }
            if ChainShareLink.isChainLink(url) {
                self.pendingChainOpen = ChainShareLink.chainId(from: url)
                return
            }
            guard url.scheme == "tehilim" else { return }
            switch url.host {
            case "daily":     NotificationManager.shared.pendingRoute = .daily
            case "lifecases": NotificationManager.shared.pendingRoute = .lifeCases
            case "psalms":    NotificationManager.shared.pendingRoute = .psalms
            case "settings":  NotificationManager.shared.pendingRoute = .settings
            case "home":      NotificationManager.shared.pendingRoute = .home
            default: break
            }
        }
    }

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

        // Commentaires (mode étude) — optionnel : repo vide si le fichier manque.
        self.commentaryRepository = (try? contentLoader.loadCommentaries()) ?? CommentaryRepository()

        self.searchInterpreter = SearchInterpreter(repository: self.psalmRepository)
    }
}
