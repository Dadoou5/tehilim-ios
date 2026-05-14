import SwiftUI

/// Onglet « Tehilim » — picker (Livres/Tous/Favoris) + liste filtrée.
///
/// V1.9.0 : sur iPad (regular size class) on bascule en **NavigationSplitView**
/// avec la liste à gauche et le Tehilim sélectionné à droite. Sur iPhone et
/// iPad portrait, on garde la NavigationStack classique (push standard).
struct PsalmsTabView: View {
    @Binding var path: NavigationPath
    @EnvironmentObject private var router: TabRouter
    @Environment(\.horizontalSizeClass) private var hSize

    /// Sélection persistée — pilote la detail column en mode SplitView.
    @State private var selectedPsalmId: Int? = nil

    var body: some View {
        Group {
            if hSize == .regular {
                splitLayout
            } else {
                stackLayout
            }
        }
    }

    // MARK: - iPhone & iPad portrait : NavigationStack classique

    private var stackLayout: some View {
        NavigationStack(path: $path) {
            sidebarContent(selection: nil)
        }
    }

    // MARK: - iPad regular : NavigationSplitView

    private var splitLayout: some View {
        NavigationSplitView {
            NavigationStack(path: $path) {
                sidebarContent(selection: $selectedPsalmId)
            }
        } detail: {
            NavigationStack {
                if let id = selectedPsalmId {
                    PsalmDetailView(psalmId: id)
                        .id(id) // force le rebuild quand la sélection change
                } else {
                    welcomeDetail
                }
            }
        }
        .navigationSplitViewStyle(.balanced)
    }

    @ViewBuilder
    private func sidebarContent(selection: Binding<Int?>?) -> some View {
        VStack(spacing: 0) {
            Picker("", selection: $router.psalmsSegment) {
                Text("Livres").tag(0)
                Text("Tous").tag(1)
                Text("Favoris").tag(2)
            }
            .pickerStyle(.segmented)
            .padding(.horizontal, 16)
            .padding(.top, 8)

            Group {
                switch router.psalmsSegment {
                case 0: BookListView(selection: selection)
                case 1: PsalmListView(book: nil, selection: selection)
                default: FavoritesListView(selection: selection)
                }
            }
        }
        .navigationTitle("Tehilim")
    }

    @ViewBuilder
    private var welcomeDetail: some View {
        VStack(spacing: 16) {
            Image(systemName: "book.closed")
                .font(.system(size: 60))
                .foregroundStyle(Color.accentMain.opacity(0.4))
            Text("Sélectionne un Tehilim")
                .font(.title2.weight(.semibold))
                .foregroundStyle(.secondary)
            Text("Choisis un Tehilim dans la liste pour commencer à lire.")
                .font(.callout)
                .foregroundStyle(.tertiary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.bgPrimary)
    }
}
