import SwiftUI

/// Contenu de la liste — utilisable comme destination dans n'importe quelle pile.
/// Refondu en V1.8.1 : grille adaptive de cartes au lieu d'une List, pour mieux
/// exploiter la largeur de l'iPad tout en restant lisible sur iPhone.
struct LifeCasesListView: View {
    @EnvironmentObject private var container: AppContainer
    @Environment(\.horizontalSizeClass) private var hSize

    /// Grille adaptative : 2 colonnes sur iPhone, et autant que la largeur le
    /// permet sur iPad (3 en portrait, 4 en paysage) — sans coder l'orientation.
    /// regularMin à 240 : ≤254 garantit 3 colonnes en portrait iPad (786pt de
    /// contenu) ; 4 colonnes en paysage (≈1100pt).
    private var columns: [GridItem] {
        AdaptiveLayout.adaptiveColumns(for: hSize, compactMin: 160, regularMin: 240)
    }

    var body: some View {
        Group {
            if container.lifeCaseRepository.cases.isEmpty {
                EmptyStateView(
                    symbol: "heart.text.square",
                    title: "Catégories en cours de validation",
                    message: "Le contenu sera disponible après validation éditoriale."
                )
            } else {
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 20, pinnedViews: []) {
                        ForEach(container.lifeCaseRepository.grouped) { group in
                            sectionView(for: group)
                        }
                    }
                    .padding(.horizontal, AdaptiveLayout.horizontalPadding(for: hSize))
                    .padding(.vertical, 16)
                    .readingWidth(maxWidth: AdaptiveLayout.dashboardMaxWidth)
                }
                .background(Color.bgPrimary)
            }
        }
        .navigationTitle("Cas de la vie")
    }

    @ViewBuilder
    private func sectionView(for group: LifeCaseRepository.Group) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(group.localizedTitle)
                .font(.title3.weight(.semibold))
                .foregroundStyle(.primary)
                .accessibilityAddTraits(.isHeader)
                .padding(.horizontal, 2)

            LazyVGrid(columns: columns, spacing: 12) {
                ForEach(group.cases) { c in
                    NavigationLink(destination: LifeCaseDetailView(caseId: c.id)) {
                        LifeCaseCard(lifeCase: c)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }
}

/// Variante pour l'onglet racine — fournit la NavigationStack et son path.
struct LifeCasesTabView: View {
    @Binding var path: NavigationPath

    var body: some View {
        NavigationStack(path: $path) {
            LifeCasesListView()
        }
    }
}
