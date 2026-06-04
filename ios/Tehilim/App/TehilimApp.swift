import SwiftUI
import UIKit
import UserNotifications

/// `AppDelegate` minimal : capte les **Universal Links** (`https://…/p/…`) de
/// façon fiable, y compris au cold-start — là où `.onContinueUserActivity`
/// SwiftUI est notoirement défaillant (l'app s'ouvre mais le lien n'est jamais
/// livré). C'est ce qui faisait que les liens `https` n'importaient pas alors
/// que les `tehilim://` (via `.onOpenURL`) fonctionnaient.
final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        // Feature « Chaîne de Tehilim » : init Supabase UNIQUEMENT si la config
        // est présente (Supabase-Info.plist). Sinon l'app reste 100 % locale.
        // Connexion anonyme → uid stable par appareil (identifie créateur &
        // verrous). `SupabaseManager.shared.client` est `nil` sans config.
        if SupabaseManager.shared.client != nil {
            Task { try? await ChainService().ensureSignedIn() }
        }
        return true
    }

    func application(_ application: UIApplication,
                     continue userActivity: NSUserActivity,
                     restorationHandler: @escaping ([UIUserActivityRestoring]?) -> Void) -> Bool {
        guard userActivity.activityType == NSUserActivityTypeBrowsingWeb,
              let url = userActivity.webpageURL else { return false }
        AppContainer.shared.routeIncomingURL(url)
        return true
    }

    func application(_ app: UIApplication, open url: URL,
                     options: [UIApplication.OpenURLOptionsKey: Any] = [:]) -> Bool {
        AppContainer.shared.routeIncomingURL(url)
        return true
    }

    // Notifications push de chaîne : le token APNs arrive ici → envoyé à Supabase.
    func application(_ application: UIApplication,
                     didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        let hex = deviceToken.map { String(format: "%02x", $0) }.joined()
        Task { await ChainService().registerDeviceToken(hex, locale: AppLocale.code) }
    }

    func application(_ application: UIApplication,
                     didFailToRegisterForRemoteNotificationsWithError error: Error) {
        #if DEBUG
        print("Push registration failed: \(error)")
        #endif
    }
}

/// Demande l'autorisation de notifications puis enregistre l'appareil auprès
/// d'APNs (le token est livré à `AppDelegate`, qui l'envoie à Supabase). Appelé
/// à l'ouverture d'une chaîne — seuls les participants sont concernés.
enum PushRegistrar {
    static func request() {
        let center = UNUserNotificationCenter.current()
        center.requestAuthorization(options: [.alert, .sound, .badge]) { granted, _ in
            guard granted else { return }
            DispatchQueue.main.async { UIApplication.shared.registerForRemoteNotifications() }
        }
    }
}

@main
struct TehilimApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @StateObject private var container = AppContainer.shared
    /// Mode Chabbat — pilote l'écran bloquant. Initialisé avec les prefs du
    /// container (singleton) → instance stable pour la session.
    @StateObject private var shabbat = ShabbatManager(preferences: AppContainer.shared.preferences)

    @AppStorage("pref.theme") private var theme: AppTheme = .system
    @AppStorage("pref.onboarding.done") private var onboardingCompleted: Bool = false
    /// V2.1.b — observée pour forcer la recréation du tree au changement
    /// (via `.id(appLanguage)` sur la racine).
    @AppStorage("pref.app.language") private var appLanguage: AppLanguage = .system

    @Environment(\.scenePhase) private var scenePhase
    @State private var showSplash = true

    init() {
        // V2.1.b — swizzle de Bundle.main installé AVANT toute lecture de
        // ressources localisées par SwiftUI. Les `Text("…")` résolvent
        // désormais contre la `.lproj` choisie par l'utilisateur.
        LocalizedBundleInstaller.installOnce()

        // Conservé pour les API iOS qui lisent Locale.current/AppleLanguages
        // (DateFormatter système, voix VoiceOver). Le swizzle prend le relais
        // pour les chaînes UI.
        Self.applyLanguagePreference()
        _ = NotificationManager.shared
        Self.migrateSharedPreferences()
        Self.migrateLanguagePreference()
    }

    var body: some Scene {
        WindowGroup {
            ZStack {
                if showSplash {
                    SplashView()
                        .transition(.opacity)
                } else if shabbat.isBlocking {
                    // Mode Chabbat : l'app est inaccessible, l'écran de
                    // démarrage laisse place à « Chabbat Chalom ».
                    ChabbatChalomView(
                        startsAt: shabbat.state.startedAt,
                        endsAt: shabbat.state.endsAt,
                        onContinue: { shabbat.continueAnyway() }
                    )
                    .transition(.opacity)
                } else {
                    RootTabView()
                        .environmentObject(container)
                        .environmentObject(shabbat)
                        .fullScreenCover(isPresented: Binding(
                            get: { !onboardingCompleted },
                            set: { onboardingCompleted = !$0 }
                        )) {
                            OnboardingView { onboardingCompleted = true }
                        }
                        .transition(.opacity)
                }
            }
            .animation(.easeInOut(duration: 0.45), value: showSplash)
            .animation(.easeInOut(duration: 0.45), value: shabbat.isBlocking)
            .onChange(of: scenePhase) { _, phase in
                if phase == .active { shabbat.refresh() }
            }
            // Liens de prière captés AU NIVEAU DE L'APP (ZStack toujours monté,
            // même pendant le splash) → évite de perdre le lien au cold-start.
            // Le payload est stocké dans le container ; RootTabView présente
            // l'aperçu d'import dès qu'il apparaît.
            .onOpenURL { url in handleIncomingURL(url) }
            .onContinueUserActivity(NSUserActivityTypeBrowsingWeb) { activity in
                if let url = activity.webpageURL { handleIncomingURL(url) }
            }
            // V1.10.7 — `preferredColorScheme` PROMU sur le ZStack pour
            // que la SplashView respecte aussi la préférence de thème de
            // l'app (avant : `preferredColorScheme` était seulement sur
            // RootTabView → splash en mode système, accueil en mode app
            // → flash de couleur disgracieux quand l'utilisateur a forcé
            // un thème opposé à celui du device).
            .preferredColorScheme(theme.colorScheme)
            // V2.1.b — recrée le tree au changement de langue : conjugué au
            // swizzle de Bundle.main, ça force chaque `Text("…")` à relire
            // sa traduction dans la nouvelle locale, sans relancer l'app.
            .id(appLanguage)
            .environment(\.locale, Locale(identifier: appLanguage.activeCode))
            // Le token push est enregistré avec la locale courante. Au changement
            // de langue, on le ré-enregistre pour que les notifications de chaîne
            // arrivent dans la nouvelle langue (sans attendre le prochain lancement).
            .onChange(of: appLanguage) { _, _ in
                if UIApplication.shared.isRegisteredForRemoteNotifications {
                    UIApplication.shared.registerForRemoteNotifications()
                }
            }
            .onAppear {
                DispatchQueue.main.asyncAfter(deadline: .now() + 2.6) {
                    showSplash = false
                }
            }
        }
    }

    /// Relais SwiftUI vers le routage unique du container. L'`AppDelegate`
    /// couvre les Universal Links au cold-start ; ces hooks couvrent le reste
    /// (custom scheme, et UL à chaud) — `routeIncomingURL` est idempotent.
    private func handleIncomingURL(_ url: URL) {
        container.routeIncomingURL(url)
    }

    // MARK: - Language

    /// Lit la préférence `pref.app.language` et configure `AppleLanguages`.
    /// - `.system` → retire l'override (laisse iOS choisir selon la langue système).
    /// - `.fr`/`.en` → écrit le code dans `AppleLanguages`, iOS l'utilise au prochain Bundle access.
    static func applyLanguagePreference() {
        let standard = UserDefaults.standard
        let stored = standard.string(forKey: "pref.app.language") ?? AppLanguage.system.rawValue
        guard let lang = AppLanguage(rawValue: stored) else { return }
        if let code = lang.appleLanguagesCode {
            standard.set([code], forKey: "AppleLanguages")
        } else {
            standard.removeObject(forKey: "AppleLanguages")
        }
        // V2.1.b — partagé avec le widget pour le swizzle LocalizedBundle.
        AppGroup.userDefaults.set(stored, forKey: AppGroup.Keys.appLanguage)
    }

    // MARK: - Migrations

    private static func migrateSharedPreferences() {
        let group = AppGroup.userDefaults
        let standard = UserDefaults.standard
        let key = AppGroup.Keys.dailyMode
        if group.object(forKey: key) == nil,
           let oldValue = standard.string(forKey: key) {
            group.set(oldValue, forKey: key)
        }
    }

    /// Migre l'ancien `pref.translation.lang` (V1.7.0) vers `pref.app.language` (V1.7.2).
    private static func migrateLanguagePreference() {
        let standard = UserDefaults.standard
        if standard.object(forKey: "pref.app.language") == nil,
           let old = standard.string(forKey: "pref.translation.lang") {
            // Si c'était "en" → on respecte ce choix.
            // Si c'était "fr" (défaut historique) → on bascule sur .system, plus naturel.
            let mapped: String = (old == "en") ? "en" : "system"
            standard.set(mapped, forKey: "pref.app.language")
        }
    }
}
