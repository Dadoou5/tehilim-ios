import SwiftUI

/// Contenu de la liste — utilisable comme destination dans n'importe quelle pile.
struct LifeCasesListView: View {
    @EnvironmentObject private var container: AppContainer

    var body: some View {
        Group {
            if container.lifeCaseRepository.cases.isEmpty {
                EmptyStateView(
                    symbol: "heart.text.square",
                    title: "Catégories en cours de validation",
                    message: "Le contenu sera disponible après validation éditoriale."
                )
            } else {
                List {
                    ForEach(container.lifeCaseRepository.grouped) { group in
                        Section(group.title) {
                            ForEach(group.cases) { c in
                                NavigationLink(destination: LifeCaseDetailView(caseId: c.id)) {
                                    HStack(spacing: 12) {
                                        Image(systemName: c.symbol)
                                            .foregroundStyle(Color.accentMain)
                                            .frame(width: 28)
                                        VStack(alignment: .leading) {
                                            Text(c.title).font(.headline)
                                            Text("\(c.psalms.count) Tehilim").font(.caption).foregroundStyle(.secondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                .listStyle(.insetGrouped)
                .appBackground()
            }
        }
        .navigationTitle("Cas de la vie")
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
