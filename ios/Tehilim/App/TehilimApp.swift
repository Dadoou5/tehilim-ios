import SwiftUI

@main
struct TehilimApp: App {
    @StateObject private var container = AppContainer.shared

    @AppStorage("pref.theme") private var theme: AppTheme = .system
    @AppStorage("pref.onboarding.done") private var onboardingCompleted: Bool = false
    /// V2.1.b — observée pour forcer la recréation du tree au changement
    /// (via `.id(appLanguage)` sur la racine).
    @AppStorage("pref.app.language") private var appLanguage: AppLanguage = .system

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
                } else {
                    RootTabView()
                        .environmentObject(container)
                        .preferredColorScheme(theme.colorScheme)
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
            // V2.1.b — recrée le tree au changement de langue : conjugué au
            // swizzle de Bundle.main, ça force chaque `Text("…")` à relire
            // sa traduction dans la nouvelle locale, sans relancer l'app.
            .id(appLanguage)
            .environment(\.locale, Locale(identifier: appLanguage.activeCode))
            .onAppear {
                DispatchQueue.main.asyncAfter(deadline: .now() + 2.6) {
                    showSplash = false
                }
            }
        }
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
