import SwiftUI

/// Écran d'accueil de la feature : créer une chaîne et retrouver ses chaînes,
/// classées en trois états clairs — Sélection en cours, Lecture en cours,
/// Terminées. Le classement est recalculé en continu (bascule automatique).
struct MyChainsView: View {
    @EnvironmentObject private var container: AppContainer
    @EnvironmentObject private var archive: ChainArchiveStore

    @State private var showCreate = false
    @State private var openChain: ChainBox?
    @State private var cloud: [TehilimChain] = []
    @State private var now = Date()

    private let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    private enum Category { case selection, reading, closed }
    private struct ChainEntry: Identifiable {
        let id: String
        let title: String
        let category: Category
        let target: Date?       // nil si terminée
        let hasArchive: Bool
    }

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
        .onReceive(timer) { now = $0 }
        .task(id: archive.knownChainIds) {
            var result: [TehilimChain] = []
            for id in archive.knownChainIds {
                if let c = try? await container.chains.fetchChain(id: id) { result.append(c) }
            }
            cloud = result
        }
        .sheet(isPresented: $showCreate) {
            CreateChainView { id in openChain = ChainBox(id: id) }
                .environmentObject(container)
        }
        .navigationDestination(item: $openChain) { box in
            ChainDetailView(chainId: box.id)
        }
    }

    // MARK: - Catégorisation

    private var entries: [ChainEntry] {
        let archiveIds = Set(archive.archives.map { $0.id })
        let cloudIds = Set(cloud.map { $0.id })
        var out: [ChainEntry] = []
        for c in cloud {
            let cat: Category = (!c.distributed && now < c.selectionDeadline) ? .selection
                : (now < c.readingDeadline ? .reading : .closed)
            let target: Date? = cat == .selection ? c.selectionDeadline
                : (cat == .reading ? c.readingDeadline : nil)
            out.append(ChainEntry(id: c.id, title: c.subjectLine, category: cat,
                                  target: target, hasArchive: archiveIds.contains(c.id)))
        }
        // Archives dont le cloud a disparu (TTL) → lecture (échéance future) ou terminée.
        for a in archive.archives where !cloudIds.contains(a.id) {
            let cat: Category = now < a.readingDeadline ? .reading : .closed
            out.append(ChainEntry(id: a.id, title: a.subjectLine, category: cat,
                                  target: cat == .reading ? a.readingDeadline : nil, hasArchive: true))
        }
        return out
    }

    @ViewBuilder
    private var list: some View {
        let selection = entries.filter { $0.category == .selection }
        let reading = entries.filter { $0.category == .reading }
        let closed = entries.filter { $0.category == .closed }

        List {
            Section {
                Button { showCreate = true } label: {
                    Label("Créer une chaîne", systemImage: "link.badge.plus").font(.headline)
                }
            } footer: {
                Text("Crée une chaîne, partage le lien sur WhatsApp : chacun choisit les Tehilim qu'il s'engage à lire, en temps réel.")
            }

            if !selection.isEmpty {
                Section("Sélection en cours") {
                    ForEach(selection) { e in row(e) }
                }
            }
            if !reading.isEmpty {
                Section("Lecture en cours") {
                    ForEach(reading) { e in row(e) }
                    .onDelete { remove(reading, $0) }
                }
            }
            if !closed.isEmpty {
                Section("Terminées") {
                    ForEach(closed) { e in row(e) }
                    .onDelete { remove(closed, $0) }
                }
            }
        }
        .listStyle(.insetGrouped)
        .appBackground()
    }

    @ViewBuilder
    private func row(_ e: ChainEntry) -> some View {
        NavigationLink {
            destination(e)
        } label: {
            VStack(alignment: .leading, spacing: 2) {
                Text(e.title).font(.subheadline.weight(.medium))
                if let target = e.target {
                    Text(remaining(until: target))
                        .font(.title2.weight(.bold))
                        .foregroundStyle(Color.accentMain)
                        .monospacedDigit()
                    Text(e.category == .selection ? "avant la fin de la sélection"
                                                  : "avant la fin de la lecture")
                        .font(.caption2).foregroundStyle(.secondary)
                }
            }
        }
    }

    @ViewBuilder
    private func destination(_ e: ChainEntry) -> some View {
        // Sélection → détail live. Lecture/terminée → lecteur hors-ligne si dispo.
        if e.category != .selection, let snap = archive.archives.first(where: { $0.id == e.id }) {
            ChainArchiveReaderView(snapshot: snap)
        } else {
            ChainDetailView(chainId: e.id)
        }
    }

    private func remove(_ list: [ChainEntry], _ offsets: IndexSet) {
        for id in offsets.map({ list[$0].id }) {
            archive.forget(id)
            archive.deleteArchive(id)
        }
    }

    private func remaining(until date: Date) -> String {
        ChainCountdown.format(seconds: max(0, Int(date.timeIntervalSince(now))))
    }
}

private struct ChainBox: Identifiable, Hashable { let id: String }
