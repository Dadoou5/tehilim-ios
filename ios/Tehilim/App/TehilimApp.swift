import SwiftUI

@main
struct TehilimApp: App {
    @StateObject private var container = AppContainer.shared

    @AppStorage("pref.theme") private var theme: AppTheme = .system
    @AppStorage("pref.onboarding.done") private var onboardingCompleted: Bool = false

    @State private var showSplash = true

    init() {
        // Force l'init du gestionnaire de notifications au plus tôt :
        // installe le delegate et permet le tap-to-route depuis cold launch.
        _ = NotificationManager.shared
        // Migration une fois : si l'utilisateur avait un dailyMode dans .standard
        // (V<=1.5), le copier dans le conteneur App Group partagé.
        Self.migrateSharedPreferences()
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

    private static func migrateSharedPreferences() {
        let group = AppGroup.userDefaults
        let standard = UserDefaults.standard
        let key = AppGroup.Keys.dailyMode
        if group.object(forKey: key) == nil,
           let oldValue = standard.string(forKey: key) {
            group.set(oldValue, forKey: key)
        }
    }
}
