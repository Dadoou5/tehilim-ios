import SwiftUI

@main
struct TehilimApp: App {
    @StateObject private var container = AppContainer.shared

    @AppStorage("pref.theme") private var theme: AppTheme = .system
    @AppStorage("pref.onboarding.done") private var onboardingCompleted: Bool = false

    @State private var showSplash = true

    init() {
        // Doit s'exécuter en premier — modifie AppleLanguages avant que SwiftUI lise le Bundle.
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
            .onAppear {
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.8) {
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
