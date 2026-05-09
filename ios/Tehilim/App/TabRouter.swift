import Foundation
import SwiftUI

/// Source de vérité pour la navigation entre onglets et certaines sous-sélections.
@MainActor
final class TabRouter: ObservableObject {

    enum Tab: Hashable {
        case home, psalms, daily, lifeCases, settings
    }

    @Published var selected: Tab = .home
    /// 0 = Livres, 1 = Tous, 2 = Favoris (cf. PsalmsTabView)
    @Published var psalmsSegment: Int = 0
    /// Signal observé par RootTabView : si non nil, la pile de cet onglet est remise à la racine.
    @Published var pendingPathReset: Tab? = nil

    /// Bascule sur un onglet.
    /// - parameter resetPath: si vrai, vide la pile de l'onglet ciblé. Utilisé par les cartes
    ///   de l'écran d'accueil pour garantir un atterrissage propre. Le tap d'onglet via la
    ///   tab bar n'utilise jamais ce paramètre (le binding $selected ne passe pas par `go`).
    func go(_ tab: Tab, psalmsSegment: Int? = nil, resetPath: Bool = false) {
        if let segment = psalmsSegment {
            self.psalmsSegment = segment
        }
        if resetPath {
            self.pendingPathReset = tab
        }
        self.selected = tab
    }
}
