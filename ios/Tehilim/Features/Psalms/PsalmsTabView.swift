import SwiftUI

/// Onglet « Tehilim » — V1.9.5 architecture refondue iPad.
///
/// - **iPhone (compact)** : NavigationStack avec BookListView → PsalmListView push
///   classique, picker en haut. Inchangé depuis V1.0.
/// - **iPad (regular)** : NavigationSplitView 2 colonnes.
///   - Sidebar : `IPadPsalmsSidebar` qui affiche les 150 Tehilim directement
///     (groupés par livre / liste plate / favoris selon le segment).
///     Une seule `List(selection:)` à la racine — pas de push, pas de drill-down.
///   - Detail : PsalmDetailView du Tehilim sélectionné, ou welcome view.
struct PsalmsTabView: View {
    @Binding var path: NavigationPath
    @EnvironmentObject private var router: TabRouter
    @Environment(\.horizontalSizeClass) private var hSize

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

    // MARK: - iPhone & iPad compact (NavigationStack)

    private var stackLayout: some View {
        NavigationStack(path: $path) {
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
                    case 0: BookListView()
                    case 1: PsalmListView(book: nil)
                    default: FavoritesListView()
                    }
                }
            }
            .navigationTitle("Tehilim")
        }
    }

    // MARK: - iPad regular (NavigationSplitView 2 colonnes)

    private var splitLayout: some View {
        NavigationSplitView {
            IPadPsalmsSidebar(selectedPsalmId: $selectedPsalmId)
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
