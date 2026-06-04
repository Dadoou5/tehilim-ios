import SwiftUI
import UIKit

private struct PsalmNav: Identifiable, Hashable { let id: Int }

/// Participant que le maître s'apprête à retirer (confirmation avant action).
private struct PendingRemoval: Identifiable { let id: String; let name: String }

/// Détail temps réel d'une chaîne : participants, compte à rebours, grille de
/// sélection 1→150 (filtrable, par livres), contrôles créateur, partage, lecture.
struct ChainDetailView: View {
    @EnvironmentObject private var container: AppContainer
    let chainId: String
    /// Non nil quand la vue est présentée modalement (ouverture via lien) →
    /// affiche un bouton « Fermer ».
    var onClose: (() -> Void)? = nil

    @Environment(\.horizontalSizeClass) private var hSize
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.dismiss) private var dismiss
    @StateObject private var session: ChainSession
    @State private var showJoin = false
    @State private var showDeleteConfirm = false
    @State private var showLeaveConfirm = false
    @State private var showEdit = false
    @State private var showInvite = false
    @State private var gridFilter: ChainGridFilter = .all
    @State private var reading: PsalmNav?
    @State private var nowTick = Date()
    @State private var errorMessage: String?
    @State private var working = false
    /// Participant en attente de confirmation de retrait (maître).
    @State private var participantToRemove: PendingRemoval?

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
                skeleton
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
        // Économie de ressources : on coupe l'écoute Realtime en arrière-plan.
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { session.start() } else { session.stop() }
        }
        .sheet(isPresented: $showJoin) {
            JoinChainSheet { name in Task { await join(name) } }
        }
        .sheet(isPresented: $showEdit) {
            if let chain = session.chain {
                CreateChainView(editing: chain) { _ in }
            }
        }
        .sheet(isPresented: $showInvite) {
            if let chain = session.chain {
                ChainInviteSheet(chain: chain)
            }
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
        .alert("Quitter la chaîne ?", isPresented: $showLeaveConfirm) {
            Button("Annuler", role: .cancel) {}
            Button("Quitter", role: .destructive) { Task { await leave() } }
        } message: {
            Text("Tes Tehilim seront libérés pour les autres participants.")
        }
        // Retrait d'un participant par le maître (pendant la sélection) — confirmation.
        .alert("Retirer ce participant ?", isPresented: Binding(
            get: { participantToRemove != nil },
            set: { if !$0 { participantToRemove = nil } }
        ), presenting: participantToRemove) { p in
            Button("Annuler", role: .cancel) {}
            Button("Retirer", role: .destructive) { Task { await removeParticipant(p.id) } }
        } message: { p in
            Text(String(format: String(localized: "Les Tehilim de %@ seront libérés et redeviendront disponibles. Cette personne sera retirée de la chaîne."), p.name))
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
                Button { showInvite = true } label: {
                    Label("Inviter des participants", systemImage: "person.badge.plus")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered).tint(.accentMain).controlSize(.large)
                progressCard
                if session.assignedCount > 0 { breakdownCard }

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
                    } else {
                        Button(role: .destructive) {
                            showLeaveConfirm = true
                        } label: {
                            Label("Quitter la chaîne", systemImage: "rectangle.portrait.and.arrow.right")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.bordered)
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
        // Compteur + filtre « collants » en haut, pendant qu'on est participant.
        .safeAreaInset(edge: .top, spacing: 0) {
            if session.isCurrentUserParticipant { filterBar }
        }
    }

    // MARK: - Cartes

    @ViewBuilder
    private func headerCard(_ chain: TehilimChain) -> some View {
        let tint = chain.intentionType.tint
        VStack(alignment: .leading, spacing: 8) {
            Label(LocalizedStringKey(chain.intentionType.titleKey), systemImage: chain.intentionType.symbol)
                .font(.caption.weight(.semibold))
                .foregroundStyle(.white)
                .padding(.horizontal, 10).padding(.vertical, 4)
                .background(tint, in: Capsule())
            Text(chain.subjectLine)
                .font(.title2.weight(.semibold))
            Text("Créée par \(chain.creatorName)")
                .font(.caption).foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(tint.opacity(0.10), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(alignment: .leading) {
            RoundedRectangle(cornerRadius: 3).fill(tint).frame(width: 4).padding(.vertical, 12)
        }
    }

    @ViewBuilder
    private func countdownCard(_ chain: TehilimChain, open: Bool) -> some View {
        let deadline = open ? chain.selectionDeadline : chain.readingDeadline
        HStack(spacing: 14) {
            Image(systemName: open ? "timer" : "book")
                .font(.title2).foregroundStyle(chain.intentionType.tint)
            VStack(alignment: .leading, spacing: 2) {
                Text(open ? "Fin de la sélection dans" : (chain.distributed ? "Distribuée · lecture jusqu'au" : "Sélection close · lecture jusqu'au"))
                    .font(.caption).foregroundStyle(.secondary)
                if open {
                    Text(remaining(until: deadline))
                        .font(.system(.title2, design: .rounded).weight(.bold).monospacedDigit())
                } else {
                    Text(deadline.formatted(date: .abbreviated, time: .shortened))
                        .font(.headline)
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
                Button { showInvite = true } label: {
                    Image(systemName: "person.crop.circle.badge.plus").font(.title3)
                }
                .buttonStyle(.plain).foregroundStyle(Color.accentMain)
                .accessibilityLabel("Inviter des participants")
            }
            if session.isCurrentUserCreator {
                // Le créateur peut retirer un participant (libère ses cases).
                ForEach(session.participants) { p in
                    HStack {
                        Text(p.name).font(.caption)
                        if p.isCreator { Text("· maître").font(.caption2).foregroundStyle(.secondary) }
                        Spacer()
                        Text("\(count(for: p.id))").font(.caption.monospacedDigit()).foregroundStyle(.secondary)
                        // Une fois la chaîne distribuée, le maître ne peut plus
                        // retirer un participant (lecture en cours, figée).
                        if !p.isCreator && !(session.chain?.distributed ?? false) {
                            Button {
                                participantToRemove = PendingRemoval(id: p.id, name: p.name)
                            } label: {
                                Image(systemName: "person.fill.xmark").font(.caption)
                            }
                            .buttonStyle(.plain).foregroundStyle(.red)
                            .accessibilityLabel("Retirer \(p.name)")
                        }
                    }
                }
            } else if !session.participants.isEmpty {
                Text(session.participants.map(\.name).joined(separator: " · "))
                    .font(.caption).foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16).appCard()
        .disabled(working)
    }

    @ViewBuilder
    private var progressCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("Avancement").font(.subheadline.weight(.semibold))
                Spacer()
                Text("\(session.assignedCount)/\(TehilimChain.totalPsalms)")
                    .font(.subheadline.weight(.medium).monospacedDigit()).foregroundStyle(.secondary)
            }
            segmentedBar
        }
        .padding(16).appCard()
    }

    /// Barre segmentée par participant (qui a pris combien).
    private var segmentedBar: some View {
        let segs = participantSegments()
        return GeometryReader { geo in
            HStack(spacing: 1) {
                ForEach(Array(segs.enumerated()), id: \.offset) { _, seg in
                    Rectangle().fill(seg.color)
                        .frame(width: max(2, geo.size.width * CGFloat(seg.count) / CGFloat(TehilimChain.totalPsalms)))
                }
                Spacer(minLength: 0)
            }
        }
        .frame(height: 10)
        .background(Color.bgSurface)
        .clipShape(Capsule())
    }

    /// Répartition par personne (in-app, repliable).
    @ViewBuilder
    private var breakdownCard: some View {
        DisclosureGroup {
            VStack(alignment: .leading, spacing: 8) {
                ForEach(Array(breakdown().enumerated()), id: \.offset) { _, row in
                    HStack(alignment: .top, spacing: 8) {
                        Text(row.name).font(.caption.weight(.medium))
                        Spacer(minLength: 8)
                        Text(row.ranges)
                            .font(.caption.monospacedDigit()).foregroundStyle(.secondary)
                            .multilineTextAlignment(.trailing)
                    }
                }
            }
            .padding(.top, 8)
        } label: {
            Label("Répartition par personne", systemImage: "list.bullet.rectangle")
                .font(.subheadline.weight(.semibold))
        }
        .tint(.accentMain)
        .padding(16).appCard()
    }

    /// Barre filtre + compteur (collante en haut de la grille).
    private var filterBar: some View {
        HStack(spacing: 12) {
            Picker("", selection: $gridFilter) {
                ForEach(ChainGridFilter.allCases) { f in
                    Text(LocalizedStringKey(f.titleKey)).tag(f)
                }
            }
            .pickerStyle(.segmented)
            Text("\(session.assignedCount)/\(TehilimChain.totalPsalms)")
                .font(.caption.weight(.semibold).monospacedDigit())
                .foregroundStyle(.secondary)
        }
        .padding(.horizontal, AdaptiveLayout.horizontalPadding(for: hSize))
        .padding(.vertical, 8)
        .background(.bar)
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
            Text(open ? "Touche un numéro libre pour t'y engager. Touche un de tes numéros pour le libérer."
                      : "La sélection est close. Touche un de tes Tehilim pour le lire.")
                .font(.caption).foregroundStyle(.secondary)
            ChainPsalmGrid(
                assignments: session.assignments,
                currentUid: session.currentUid,
                selectionOpen: open,
                onToggle: { id in Task { await toggle(id) } },
                onRead: { id in reading = PsalmNav(id: id) },
                minutesFor: { container.psalmRepository.psalm(id: $0)?.estimatedReadingMinutes ?? 1 },
                filter: gridFilter
            )
        }
    }

    @ViewBuilder
    private func creatorSection(_ chain: TehilimChain, open: Bool) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Divider()
            Text("Maître de la chaîne").font(.headline)
            if open && !chain.distributed {
                Button { showEdit = true } label: {
                    Label("Modifier la chaîne", systemImage: "pencil").frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered).tint(.accentMain)
            }
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

    // MARK: - Skeleton de chargement

    private var skeleton: some View {
        VStack(alignment: .leading, spacing: 20) {
            ForEach(0..<3, id: \.self) { _ in
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(Color.bgSurface).frame(height: 64)
            }
            LazyVGrid(columns: AdaptiveLayout.adaptiveColumns(for: hSize, compactMin: 58, regularMin: 80, spacing: 8), spacing: 8) {
                ForEach(0..<36, id: \.self) { _ in
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .fill(Color.bgSurface).frame(height: 44)
                }
            }
        }
        .padding(.horizontal, AdaptiveLayout.horizontalPadding(for: hSize))
        .padding(.vertical, 16)
        .redacted(reason: .placeholder)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .accessibilityLabel("Chargement…")
    }

    // MARK: - Données dérivées

    private func count(for uid: String) -> Int {
        session.assignments.values.reduce(0) { $0 + ($1.uid == uid ? 1 : 0) }
    }

    private func participantSegments() -> [(name: String, count: Int, color: Color)] {
        let palette: [Color] = [.accentMain, .blue, .green, .orange, .purple, .pink, .teal, .indigo, .brown, .mint]
        var out: [(String, Int, Color)] = []
        for (i, p) in session.participants.enumerated() {
            let c = count(for: p.id)
            if c > 0 { out.append((p.name, c, palette[i % palette.count])) }
        }
        return out.map { (name: $0.0, count: $0.1, color: $0.2) }
    }

    private func breakdown() -> [(name: String, ranges: String)] {
        var byUid: [String: (name: String, ids: [Int])] = [:]
        for (psalmId, a) in session.assignments {
            byUid[a.uid, default: (a.name, [])].ids.append(psalmId)
        }
        let en = AppLocale.code == "en"
        return byUid.values.sorted { $0.name < $1.name }.map {
            (name: $0.name, ranges: TehilimChain.compressRanges($0.ids, separator: en ? "to" : "à"))
        }
    }

    // MARK: - Actions

    private func join(_ name: String) async {
        do { try await container.chains.join(chainId: chainId, name: name) }
        catch { errorMessage = "Impossible de rejoindre." }
    }

    private func leave() async {
        working = true; defer { working = false }
        do {
            try await container.chains.leaveChain(chainId: chainId)
            container.chainArchive.forget(chainId)
            if let onClose { onClose() } else { dismiss() }
        } catch { errorMessage = "Impossible de quitter." }
    }

    private func removeParticipant(_ uid: String) async {
        working = true; defer { working = false }
        do { try await container.chains.removeParticipant(chainId: chainId, uid: uid) }
        catch { errorMessage = "Retrait impossible." }
    }

    private func toggle(_ id: Int) async {
        errorMessage = nil
        guard let uid = session.currentUid else { return }
        let mine = session.assignments[id]?.uid == uid
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

extension ChainIntention {
    /// Teinte thématique de l'intention (en-tête + accents).
    var tint: Color {
        switch self {
        case .lelouy:   return Color(red: 0.45, green: 0.42, blue: 0.70)  // violet — mémoire
        case .refoua:   return Color(red: 0.82, green: 0.33, blue: 0.42)  // rose — guérison
        case .reussite: return Color(red: 0.86, green: 0.62, blue: 0.18)  // doré — réussite
        }
    }
}
