import SwiftUI

struct RootTabView: View {
    @EnvironmentObject private var container: AppContainer
    @StateObject private var router = TabRouter()
    @ObservedObject private var notifications = NotificationManager.shared
    @Environment(\.scenePhase) private var scenePhase

    @State private var homePath = NavigationPath()
    @State private var psalmsPath = NavigationPath()
    @State private var dailyPath = NavigationPath()
    @State private var lifeCasesPath = NavigationPath()
    @State private var settingsPath = NavigationPath()

    var body: some View {
        TabView(selection: tabBinding) {
            HomeView(path: $homePath)
                .tag(TabRouter.Tab.home)
                .tabItem { Label("Accueil", systemImage: "house") }

            PsalmsTabView(path: $psalmsPath)
                .tag(TabRouter.Tab.psalms)
                .tabItem { Label("Tehilim", systemImage: "book.closed") }

            DailyView(path: $dailyPath)
                .tag(TabRouter.Tab.daily)
                .tabItem { Label("Aujourd'hui", systemImage: "sun.max") }

            LifeCasesTabView(path: $lifeCasesPath)
                .tag(TabRouter.Tab.lifeCases)
                .tabItem { Label("Cas de la vie", systemImage: "heart.text.square") }

            SettingsView(path: $settingsPath)
                .tag(TabRouter.Tab.settings)
                .tabItem { Label("Réglages", systemImage: "gearshape") }
        }
        .tint(.accentMain)
        .environmentObject(router)
        .environmentObject(container.favorites)
        .onAppear { applyPendingRoute() }
        .onChange(of: notifications.pendingRoute) { _, _ in applyPendingRoute() }
        .onChange(of: router.pendingPathReset) { _, target in applyPathReset(target) }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active {
                Task { await notifications.refreshPermission() }
                applyPendingRoute()
            }
        }
        .onOpenURL { url in handleDeepLink(url) }
    }

    private func handleDeepLink(_ url: URL) {
        guard url.scheme == "tehilim" else { return }
        // tehilim://daily → onglet Aujourd'hui, pile vide
        if url.host == "daily" {
            router.go(.daily, resetPath: true)
        }
    }

    /// Binding personnalisé pour TabView :
    /// - changement d'onglet normal → met à jour `router.selected` (re-render TabBar OK)
    /// - re-tap sur l'onglet actif → déclenche un reset de la pile de cet onglet (pop-to-root)
    private var tabBinding: Binding<TabRouter.Tab> {
        Binding(
            get: { router.selected },
            set: { newTab in
                if newTab == router.selected {
                    // Re-tap : on reset la pile de l'onglet actif.
                    router.pendingPathReset = newTab
                } else {
                    router.selected = newTab
                }
            }
        )
    }

    private func applyPendingRoute() {
        guard let route = notifications.pendingRoute else { return }
        resetPath(of: route)
        router.go(route)
        notifications.pendingRoute = nil
    }

    private func applyPathReset(_ target: TabRouter.Tab?) {
        guard let target else { return }
        resetPath(of: target)
        router.pendingPathReset = nil
    }

    private func resetPath(of tab: TabRouter.Tab) {
        switch tab {
        case .home:      homePath = NavigationPath()
        case .psalms:    psalmsPath = NavigationPath()
        case .daily:     dailyPath = NavigationPath()
        case .lifeCases: lifeCasesPath = NavigationPath()
        case .settings:  settingsPath = NavigationPath()
        }
    }
}

#Preview {
    RootTabView().environmentObject(AppContainer.shared)
}
