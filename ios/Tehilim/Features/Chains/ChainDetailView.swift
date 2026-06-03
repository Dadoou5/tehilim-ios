import SwiftUI
import UIKit

private struct PsalmNav: Identifiable, Hashable { let id: Int }

/// Détail temps réel d'une chaîne : participants, compte à rebours, grille de
/// sélection 1→150 avec verrous, contrôles créateur, partage WhatsApp, lecture.
struct ChainDetailView: View {
    @EnvironmentObject private var container: AppContainer
    let chainId: String
    /// Non nil quand la vue est présentée modalement (ouverture via lien) →
    /// affiche un bouton « Fermer ». Nil quand poussée dans une NavigationStack
    /// (le bouton retour natif suffit).
    var onClose: (() -> Void)? = nil

    @Environment(\.horizontalSizeClass) private var hSize
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.dismiss) private var dismiss
    @StateObject private var session: ChainSession
    @State private var showJoin = false
    @State private var showDeleteConfirm = false
    @State private var reading: PsalmNav?
    @State private var nowTick = Date()
    @State private var errorMessage: String?
    @State private var working = false

    init(chainId: String, onClose: (() -> Void)? = nil) {
        self.chainId = chainId
        self.onClose = onClose
        _session = StateObject(wrappedValue: ChainSession(chainId: chainId))
    }

    private let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {
        Group {
            if let chain = session.chain {
                content(chain)
            } else if session.loadError != nil {
                EmptyStateView(symbol: "link", title: "Chaîne introuvable",
                               message: "Ce lien n'est plus valable ou la chaîne a été clôturée.")
            } else {
                ProgressView("Chargement…")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .background(Color.bgPrimary)
        .navigationTitle("Chaîne de Tehilim")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if let onClose {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Fermer") { onClose() }
                }
            }
            if let chain = session.chain {
                ToolbarItem(placement: .topBarTrailing) {
                    ShareLink(item: ChainShareLink.shareMessage(for: chain)) {
                        Image(systemName: "square.and.arrow.up")
                    }
                }
            }
        }
        .onReceive(timer) { nowTick = $0 }
        .onAppear {
            container.chainArchive.remember(chainId)
            PushRegistrar.request()   // notifs push (participants) — demande contextuelle
        }
        // Économie de ressources : on coupe l'écoute Realtime (Supabase) en
        // arrière-plan et on la reprend au premier plan.
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { session.start() } else { session.stop() }
        }
        .sheet(isPresented: $showJoin) {
            JoinChainSheet { name in Task { await join(name) } }
        }
        .navigationDestination(item: $reading) { nav in
            PsalmDetailView(psalmId: nav.id, siblings: session.myPsalmIds)
        }
        .alert("Supprimer la chaîne ?", isPresented: $showDeleteConfirm) {
            Button("Annuler", role: .cancel) {}
            Button("Supprimer", role: .destructive) { Task { await deleteChain() } }
        } message: {
            Text("La chaîne sera définitivement supprimée pour tous les participants. Action irréversible.")
        }
    }

    // MARK: - Contenu

    @ViewBuilder
    private func content(_ chain: TehilimChain) -> some View {
        let open = chain.isSelectionOpen(now: nowTick)
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                headerCard(chain)
                countdownCard(chain, open: open)
                participantsCard
                progressCard

                if !session.isCurrentUserParticipant {
                    Button {
                        showJoin = true
                    } label: {
                        Label("Rejoindre la chaîne", systemImage: "person.badge.plus")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.accentMain)
                    .controlSize(.large)
                } else {
                    selectionSection(chain, open: open)
                    if session.isCurrentUserCreator {
                        creatorSection(chain, open: open)
                    }
                }

                if let errorMessage {
                    Text(LocalizedStringKey(errorMessage)).foregroundStyle(.red).font(.callout)
                }
            }
            .padding(.horizontal, AdaptiveLayout.horizontalPadding(for: hSize))
            .padding(.vertical, 16)
            .readingWidth(maxWidth: AdaptiveLayout.dashboardMaxWidth)
        }
    }

    @ViewBuilder
    private func headerCard(_ chain: TehilimChain) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Label(LocalizedStringKey(chain.intentionType.titleKey), systemImage: chain.intentionType.symbol)
                .font(.caption.weight(.semibold))
                .foregroundStyle(.white)
                .padding(.horizontal, 10).padding(.vertical, 4)
                .background(Color.accentMain, in: Capsule())
            Text(chain.subjectLine)
                .font(.title2.weight(.semibold))
            Text("Créée par \(chain.creatorName)")
                .font(.caption).foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16).appCard()
    }

    @ViewBuilder
    private func countdownCard(_ chain: TehilimChain, open: Bool) -> some View {
        let deadline = open ? chain.selectionDeadline : chain.readingDeadline
        HStack(spacing: 14) {
            Image(systemName: open ? "timer" : "book")
                .font(.title3).foregroundStyle(Color.accentMain)
            VStack(alignment: .leading, spacing: 2) {
                Text(open ? "Fin de la sélection dans" : (chain.distributed ? "Distribuée · lecture jusqu'au" : "Sélection close · lecture jusqu'au"))
                    .font(.caption).foregroundStyle(.secondary)
                if open {
                    Text(remaining(until: deadline)).font(.headline.monospacedDigit())
                } else {
                    Text(deadline.formatted(date: .abbreviated, time: .shortened)).font(.headline)
                }
            }
            Spacer()
        }
        .padding(16).appCard()
    }

    @ViewBuilder
    private var participantsCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Label("Participants", systemImage: "person.2.fill").font(.subheadline.weight(.semibold))
                Spacer()
                Text("\(session.participants.count)").font(.headline).foregroundStyle(Color.accentMain)
            }
            if !session.participants.isEmpty {
                Text(session.participants.map(\.name).joined(separator: " · "))
                    .font(.caption).foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16).appCard()
    }

    @ViewBuilder
    private var progressCard: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text("Avancement").font(.subheadline.weight(.semibold))
                Spacer()
                Text("\(session.assignedCount)/\(TehilimChain.totalPsalms)")
                    .font(.subheadline.weight(.medium)).foregroundStyle(.secondary)
            }
            ProgressView(value: session.progress).tint(.accentMain)
        }
        .padding(16).appCard()
    }

    @ViewBuilder
    private func selectionSection(_ chain: TehilimChain, open: Bool) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(open ? "Choisis tes Tehilim" : "Répartition").font(.headline)
                Spacer()
                if !session.myPsalmIds.isEmpty {
                    Text("Toi : \(session.myPsalmIds.count)").font(.caption.weight(.medium))
                        .foregroundStyle(Color.accentMain)
                }
            }
            if open {
                Text("Touche un numéro libre pour t'y engager. Touche un de tes numéros pour le libérer.")
                    .font(.caption).foregroundStyle(.secondary)
            } else {
                Text("La sélection est close. Touche un de tes Tehilim pour le lire.")
                    .font(.caption).foregroundStyle(.secondary)
            }
            ChainPsalmGrid(
                assignments: session.assignments,
                currentUid: session.currentUid,
                selectionOpen: open,
                onToggle: { id in Task { await toggle(id) } },
                onRead: { id in reading = PsalmNav(id: id) },
                minutesFor: { container.psalmRepository.psalm(id: $0)?.estimatedReadingMinutes ?? 1 }
            )
        }
    }

    @ViewBuilder
    private func creatorSection(_ chain: TehilimChain, open: Bool) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Divider()
            Text("Maître de la chaîne").font(.headline)
            if open && !session.isFullyAssigned {
                Button {
                    Task { await assignRemaining(chain) }
                } label: {
                    Label("M'attribuer les \(TehilimChain.totalPsalms - session.assignedCount) restants",
                          systemImage: "tray.and.arrow.down.fill")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered).tint(.accentMain)
            }
            if open && !chain.distributed {
                Button {
                    Task { await distribute(chain) }
                } label: {
                    Label("Clôturer et distribuer", systemImage: "checkmark.seal.fill")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent).tint(.accentMain)
            }
            // Compte rendu partageable (toujours dispo pour le créateur).
            ShareLink(item: reportText(chain)) {
                Label("Partager le compte rendu", systemImage: "square.and.arrow.up.on.square")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)

            Button(role: .destructive) {
                showDeleteConfirm = true
            } label: {
                Label("Supprimer la chaîne", systemImage: "trash")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)
        }
        .disabled(working)
    }

    // MARK: - Actions

    private func join(_ name: String) async {
        do { try await container.chains.join(chainId: chainId, name: name) }
        catch { errorMessage = "Impossible de rejoindre." }
    }

    private func toggle(_ id: Int) async {
        errorMessage = nil
        guard let uid = session.currentUid else { return }
        let mine = session.assignments[id]?.uid == uid
        // Retour haptique immédiat + mise à jour optimiste : la grille réagit
        // instantanément, le serveur (et le realtime) réconcilient ensuite.
        UISelectionFeedbackGenerator().selectionChanged()
        if mine {
            withAnimation(.easeOut(duration: 0.15)) { session.optimisticDeselect(id) }
            do { try await container.chains.deselect(chainId: chainId, psalmId: id) }
            catch {
                await session.refreshAssignments()
                errorMessage = "Impossible de libérer ce Tehilim."
            }
        } else if session.assignments[id] == nil {
            let myName = myDisplayName()
            withAnimation(.easeOut(duration: 0.15)) { session.optimisticSelect(id, uid: uid, name: myName) }
            do { try await container.chains.select(chainId: chainId, psalmId: id, name: myName) }
            catch {
                await session.refreshAssignments()
                errorMessage = "Ce Tehilim vient d'être pris."
            }
        }
    }

    private func assignRemaining(_ chain: TehilimChain) async {
        working = true; defer { working = false }
        do { try await container.chains.assignRemaining(chainId: chainId, name: chain.creatorName) }
        catch { errorMessage = "Attribution impossible." }
    }

    private func distribute(_ chain: TehilimChain) async {
        working = true; defer { working = false }
        do {
            try await container.chains.distribute(chainId: chainId)
            saveArchive(chain)
        } catch { errorMessage = "Distribution impossible." }
    }

    private func deleteChain() async {
        working = true; defer { working = false }
        do {
            try await container.chains.deleteChain(chainId: chainId)
            container.chainArchive.forget(chainId)
            if let onClose { onClose() } else { dismiss() }
        } catch { errorMessage = "Suppression impossible." }
    }

    // MARK: - Helpers

    private func myDisplayName() -> String {
        session.participants.first { $0.id == session.currentUid }?.name ?? "—"
    }

    private func remaining(until date: Date) -> String {
        let secs = max(0, Int(date.timeIntervalSince(nowTick)))
        let d = secs / 86400, h = (secs % 86400) / 3600, m = (secs % 3600) / 60, s = secs % 60
        let dU = AppLocale.code == "en" ? "d" : "j"
        if d > 0 { return "\(d) \(dU) \(h) h" }
        if h > 0 { return "\(h) h \(m) min" }
        if m > 0 { return "\(m) min \(s) s" }
        return "\(s) s"
    }

    /// Compte rendu texte (groupé par participant) — partageable WhatsApp.
    private func reportText(_ chain: TehilimChain) -> String {
        var byUid: [String: (name: String, ids: [Int])] = [:]
        for (psalmId, a) in session.assignments {
            byUid[a.uid, default: (a.name, [])].ids.append(psalmId)
        }
        let en = AppLocale.code == "en"
        var lines = [(en ? "Tehilim chain — " : "Chaîne de Tehilim — ") + chain.subjectLine, ""]
        for entry in byUid.values.sorted(by: { $0.name < $1.name }) {
            let nums = TehilimChain.compressRanges(entry.ids, separator: en ? "to" : "à")
            lines.append("• \(entry.name) : \(nums)")
        }
        let assigned = session.assignedCount
        if assigned < TehilimChain.totalPsalms {
            let left = TehilimChain.totalPsalms - assigned
            lines.append("")
            lines.append(en ? "Remaining: \(left) unassigned Tehilim."
                            : "Restants : \(left) Tehilim non attribués.")
        }
        return lines.joined(separator: "\n")
    }

    private func saveArchive(_ chain: TehilimChain) {
        var map: [String: String] = [:]
        for (psalmId, a) in session.assignments { map["\(psalmId)"] = a.name }
        container.chainArchive.saveArchive(ChainArchiveSnapshot(
            id: chain.id, name: chain.name, intentionRaw: chain.intentionType.rawValue,
            detail: chain.intentionDetail, creatorName: chain.creatorName,
            readingDeadline: chain.readingDeadline, archivedAt: Date(), assignments: map
        ))
    }
}
