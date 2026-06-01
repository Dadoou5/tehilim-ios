import SwiftUI

private struct ChainBox: Identifiable, Hashable { let id: String }

/// Écran d'accueil de la feature : créer une chaîne, retrouver les chaînes
/// connues (créées/rejointes) et les archives (comptes rendus conservés).
struct MyChainsView: View {
    @EnvironmentObject private var container: AppContainer
    @EnvironmentObject private var archive: ChainArchiveStore

    @State private var showCreate = false
    @State private var openChain: ChainBox?

    var body: some View {
        Group {
            if !ChainService.isAvailable {
                EmptyStateView(symbol: "icloud.slash",
                               title: "Indisponible",
                               message: "Les chaînes de Tehilim nécessitent une connexion (service non configuré sur cette build).")
            } else {
                list
            }
        }
        .navigationTitle("Chaînes de Tehilim")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button { showCreate = true } label: { Image(systemName: "plus") }
                    .accessibilityLabel("Créer une chaîne")
            }
        }
        .sheet(isPresented: $showCreate) {
            CreateChainView { id in openChain = ChainBox(id: id) }
                .environmentObject(container)
        }
        .navigationDestination(item: $openChain) { box in
            ChainDetailView(chainId: box.id)
        }
    }

    @ViewBuilder
    private var list: some View {
        List {
            Section {
                Button {
                    showCreate = true
                } label: {
                    Label("Créer une chaîne", systemImage: "link.badge.plus")
                        .font(.headline)
                }
            } footer: {
                Text("Crée une chaîne, partage le lien sur WhatsApp : chacun choisit les Tehilim qu'il s'engage à lire, en temps réel.")
            }

            if !archive.knownChainIds.isEmpty {
                Section("Mes chaînes") {
                    ForEach(archive.knownChainIds, id: \.self) { id in
                        NavigationLink {
                            ChainDetailView(chainId: id)
                        } label: {
                            ChainKnownRow(chainId: id)
                        }
                    }
                }
            }

            if !archive.archives.isEmpty {
                Section("Comptes rendus") {
                    ForEach(archive.archives) { snap in
                        ShareLink(item: archiveReport(snap)) {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(snap.subjectLine).font(.subheadline.weight(.medium))
                                Text("Archivé le \(snap.archivedAt.formatted(date: .abbreviated, time: .omitted))")
                                    .font(.caption).foregroundStyle(.secondary)
                            }
                        }
                    }
                    .onDelete { offsets in
                        offsets.map { archive.archives[$0].id }.forEach(archive.deleteArchive)
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .appBackground()
    }

    private func archiveReport(_ snap: ChainArchiveSnapshot) -> String {
        var byName: [String: [Int]] = [:]
        for (k, name) in snap.assignments { byName[name, default: []].append(Int(k) ?? 0) }
        var lines = ["Chaîne de Tehilim — \(snap.subjectLine)", ""]
        for (name, ids) in byName.sorted(by: { $0.key < $1.key }) {
            lines.append("• \(name) : \(ids.sorted().map(String.init).joined(separator: ", "))")
        }
        return lines.joined(separator: "\n")
    }
}

/// Ligne « Mes chaînes » : charge le sujet de la chaîne en temps différé.
private struct ChainKnownRow: View {
    @EnvironmentObject private var container: AppContainer
    let chainId: String
    @State private var title: String?
    @State private var loaded = false

    var body: some View {
        HStack {
            Image(systemName: "link").foregroundStyle(Color.accentMain)
            Text(title ?? (loaded ? "Chaîne clôturée" : "Chargement…"))
                .foregroundStyle(title == nil && loaded ? .secondary : .primary)
        }
        .task(id: chainId) {
            let chain = try? await container.chains.fetchChain(id: chainId)
            title = chain?.subjectLine
            loaded = true
        }
    }
}
