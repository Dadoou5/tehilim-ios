import SwiftUI

/// Boîte Identifiable pour piloter la présentation `.sheet(item:)` d'une chaîne.
private struct ChainOpenBox: Identifiable { let id: String }

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

    /// Miroir LOCAL de `container.pendingPrayerImport`. Indispensable :
    /// `.sheet(item:)` lié à une valeur déjà non-nil AVANT l'apparition de la
    /// vue (cold-start) ne se présente pas de façon fiable. En passant par un
    /// `@State` mis à jour via `.onReceive`/`.onAppear`, le changement survient
    /// pendant que la vue est vivante → la feuille se présente à coup sûr.
    @State private var importPayload: PrayerShareLink.Payload?
    /// Chaîne à ouvrir via lien (`/c/?id=…`), miroir de `container.pendingChainOpen`.
    @State private var chainOpen: ChainOpenBox?

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
        .environmentObject(container.savedPrayers)
        .environmentObject(container.chainArchive)
        .onAppear { applyPendingRoute() }
        .onChange(of: notifications.pendingRoute) { _, _ in applyPendingRoute() }
        .onChange(of: router.pendingPathReset) { _, target in applyPathReset(target) }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active {
                Task { await notifications.refreshPermission() }
                applyPendingRoute()
            }
        }
        // NB : plus de `.onOpenURL` ici — tous les liens sont gérés au niveau
        // App (TehilimApp.handleIncomingURL), source unique, pour survivre au
        // cold-start et éviter le conflit de plusieurs gestionnaires.
        // On reflète la valeur du container dans un @State local (cf. plus haut)
        // pour une présentation fiable, y compris quand elle est posée avant
        // l'apparition de la vue.
        .onReceive(container.$pendingPrayerImport) { payload in
            if importPayload?.id != payload?.id { importPayload = payload }
        }
        .onAppear {
            if container.pendingPrayerImport != nil {
                importPayload = container.pendingPrayerImport
            }
        }
        .sheet(item: $importPayload, onDismiss: {
            container.pendingPrayerImport = nil
        }) { payload in
            PrayerImportView(payload: payload)
                .environmentObject(container)
                .environmentObject(container.savedPrayers)
        }
        // Ouverture d'une chaîne via lien (`/c/?id=…`) — présentée en sheet avec
        // sa propre NavigationStack (la lecture d'un Tehilim s'y empile).
        .onReceive(container.$pendingChainOpen) { id in
            if let id { chainOpen = ChainOpenBox(id: id) }
        }
        .onAppear {
            if let id = container.pendingChainOpen { chainOpen = ChainOpenBox(id: id) }
        }
        // Plein écran (et non sheet) → sur iPad la grille des 150 exploite toute
        // la largeur en paysage (un sheet iPad est une carte centrée étroite).
        .fullScreenCover(item: $chainOpen, onDismiss: {
            container.pendingChainOpen = nil
        }) { box in
            NavigationStack {
                ChainDetailView(chainId: box.id, onClose: {
                    chainOpen = nil
                    container.pendingChainOpen = nil
                })
            }
            .environmentObject(container)
            .environmentObject(container.chainArchive)
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
