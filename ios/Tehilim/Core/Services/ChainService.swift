import Foundation
import Supabase

/// Couche d'accès **Supabase** (Postgres + Realtime + Auth anonyme) pour la
/// feature « Chaîne de Tehilim ».
///
/// Modèle **relationnel** (et non plus le « board » JSON unique de Firestore) :
/// une ligne par attribution dans `chain_assignments`, dont la **clé primaire
/// `(chain_id, psalm_id)`** fait office de **verrou exclusif** « 1 lecteur /
/// Tehilim » — un INSERT en double échoue ⇒ « déjà pris », atomiquement, sans
/// transaction applicative. La propriété (« je ne libère que MES cases ») est
/// imposée **côté serveur** par la RLS Postgres.
///
/// L'API publique de ce service (et de `ChainSession`) est **identique** à la
/// version Firebase précédente : la couche UI (SwiftUI) est inchangée.
final class ChainService {

    static var isAvailable: Bool { SupabaseManager.shared.client != nil }

    private var client: SupabaseClient? { SupabaseManager.shared.client }

    /// Fin de lecture + marge → champ `expires_at` (nettoyage cloud par le cron).
    static let expiryGraceSeconds: TimeInterval = 7 * 24 * 3600

    enum ChainError: LocalizedError {
        case notConfigured
        var errorDescription: String? { "Chaîne indisponible (Supabase non configuré)." }
    }

    // MARK: - Auth anonyme

    /// uid (uuid Postgres, minuscules) de l'utilisateur courant, ou `nil`.
    var currentUid: String? {
        client?.auth.currentUser?.id.uuidString.lowercased()
    }

    @discardableResult
    func ensureSignedIn() async throws -> String {
        guard let client else { throw ChainError.notConfigured }
        if let user = client.auth.currentUser { return user.id.uuidString.lowercased() }
        _ = try await client.auth.signInAnonymously()
        guard let user = client.auth.currentUser else { throw ChainError.notConfigured }
        return user.id.uuidString.lowercased()
    }

    // MARK: - Écritures

    func createChain(
        name: String,
        intention: ChainIntention,
        detail: String,
        selectionDuration: TimeInterval,
        readingDeadline: Date,
        creatorName: String
    ) async throws -> String {
        guard let client else { throw ChainError.notConfigured }
        _ = try await ensureSignedIn()
        let now = Date()
        let params = CreateChainParams(
            p_name: name,
            p_intention_type: intention.rawValue,
            p_intention_detail: detail,
            p_creator_name: creatorName,
            p_selection_deadline: Self.iso(now.addingTimeInterval(selectionDuration)),
            p_reading_deadline: Self.iso(readingDeadline),
            p_expires_at: Self.iso(readingDeadline.addingTimeInterval(Self.expiryGraceSeconds))
        )
        // RPC atomique : crée la chaîne + le créateur-participant, renvoie l'id.
        let newId: String = try await client.rpc("create_chain", params: params).execute().value
        return newId
    }

    func join(chainId: String, name: String) async throws {
        guard let client else { throw ChainError.notConfigured }
        let uid = try await ensureSignedIn()
        try await client.from(K.participants)
            .upsert(ParticipantUpsert(chain_id: chainId, uid: uid, name: name, is_creator: false),
                    onConflict: "chain_id,uid")
            .execute()
    }

    /// Réserve un Tehilim. INSERT : échoue (violation de PK) s'il est déjà pris.
    func select(chainId: String, psalmId: Int, name: String) async throws {
        guard let client else { throw ChainError.notConfigured }
        let uid = try await ensureSignedIn()
        try await client.from(K.assignments)
            .insert(AssignmentInsert(chain_id: chainId, psalm_id: psalmId,
                                     uid: uid, name: name, by_creator: false))
            .execute()
    }

    /// Libère un Tehilim réservé par soi-même (la RLS empêche de libérer celui
    /// d'un autre ; le `.eq("uid")` est une ceinture-bretelles côté client).
    func deselect(chainId: String, psalmId: Int) async throws {
        guard let client else { throw ChainError.notConfigured }
        let uid = try await ensureSignedIn()
        try await client.from(K.assignments)
            .delete()
            .eq("chain_id", value: chainId)
            .eq("psalm_id", value: psalmId)
            .eq("uid", value: uid)
            .execute()
    }

    /// (Créateur) attribue tous les Tehilim restants à lui-même (RPC, une requête).
    func assignRemaining(chainId: String, name: String) async throws {
        guard let client else { throw ChainError.notConfigured }
        _ = try await ensureSignedIn()
        try await client.rpc("assign_remaining",
                             params: AssignRemainingParams(p_chain_id: chainId, p_name: name))
            .execute()
    }

    func distribute(chainId: String) async throws {
        guard let client else { throw ChainError.notConfigured }
        _ = try await ensureSignedIn()
        try await client.from(K.chains)
            .update(DistributeUpdate(distributed: true))
            .eq("id", value: chainId)
            .execute()
    }

    /// (Créateur) supprime définitivement la chaîne (cascade → participants +
    /// attributions). La RLS n'autorise que le créateur.
    func deleteChain(chainId: String) async throws {
        guard let client else { throw ChainError.notConfigured }
        _ = try await ensureSignedIn()
        try await client.from(K.chains).delete().eq("id", value: chainId).execute()
    }

    /// Un participant quitte la chaîne : libère ses Tehilim puis se retire.
    func leaveChain(chainId: String) async throws {
        guard let client else { throw ChainError.notConfigured }
        let uid = try await ensureSignedIn()
        try await client.from(K.assignments).delete()
            .eq("chain_id", value: chainId).eq("uid", value: uid).execute()
        try await client.from(K.participants).delete()
            .eq("chain_id", value: chainId).eq("uid", value: uid).execute()
    }

    /// (Créateur) édite la chaîne (avant distribution). RLS : réservé au créateur.
    func updateChain(chainId: String, name: String, intention: ChainIntention, detail: String,
                     selectionDeadline: Date, readingDeadline: Date) async throws {
        guard let client else { throw ChainError.notConfigured }
        _ = try await ensureSignedIn()
        try await client.from(K.chains)
            .update(ChainUpdate(
                name: name, intention_type: intention.rawValue, intention_detail: detail,
                selection_deadline: Self.iso(selectionDeadline),
                reading_deadline: Self.iso(readingDeadline),
                expires_at: Self.iso(readingDeadline.addingTimeInterval(Self.expiryGraceSeconds))))
            .eq("id", value: chainId).execute()
    }

    /// (Créateur) retire un participant + libère ses cases (RPC).
    func removeParticipant(chainId: String, uid: String) async throws {
        guard let client else { throw ChainError.notConfigured }
        _ = try await ensureSignedIn()
        try await client.rpc("remove_participant",
            params: RemoveParticipantParams(p_chain_id: chainId, p_uid: uid)).execute()
    }

    /// (Créateur) prolonge la durée de sélection : repousse l'échéance, réarme
    /// les rappels et re-notifie les participants (RPC, réservé au créateur).
    func extendSelection(chainId: String, newDeadline: Date) async throws {
        guard let client else { throw ChainError.notConfigured }
        _ = try await ensureSignedIn()
        try await client.rpc("extend_chain_selection",
            params: ExtendSelectionParams(p_chain_id: chainId,
                                          p_new_deadline: Self.iso(newDeadline))).execute()
    }

    /// Enregistre / met à jour le token push de cet appareil (notifications de
    /// chaîne aux participants). Silencieux si non configuré.
    func registerDeviceToken(_ token: String, platform: String = "ios", locale: String) async {
        guard let client else { return }
        guard let uid = try? await ensureSignedIn() else { return }
        try? await client.from("device_tokens")
            .upsert(DeviceTokenRow(token: token, uid: uid, platform: platform, locale: locale),
                    onConflict: "token")
            .execute()
    }

    // MARK: - Lectures ponctuelles

    func fetchChain(id: String) async throws -> TehilimChain? {
        guard let client else { return nil }
        let rows: [ChainRow] = try await client.from(K.chains)
            .select().eq("id", value: id).limit(1).execute().value
        return rows.first.map(Self.chain(from:))
    }

    func fetchParticipants(chainId: String) async throws -> [ChainParticipant] {
        guard let client else { return [] }
        let rows: [ParticipantRow] = try await client.from(K.participants)
            .select().eq("chain_id", value: chainId).order("joined_at").execute().value
        return rows.map(Self.participant(from:))
    }

    func fetchBoard(chainId: String) async throws -> [Int: ChainAssignment] {
        guard let client else { return [:] }
        let rows: [AssignmentRow] = try await client.from(K.assignments)
            .select().eq("chain_id", value: chainId).execute().value
        var out: [Int: ChainAssignment] = [:]
        for r in rows { out[r.psalm_id] = Self.assignment(from: r) }
        return out
    }

    // MARK: - Constantes de tables

    enum K {
        static let chains = "chains"
        static let participants = "chain_participants"
        static let assignments = "chain_assignments"
    }

    // MARK: - DTO (colonnes Postgres en snake_case)

    private struct ChainRow: Decodable {
        let id: String
        let name: String
        let intention_type: String
        let intention_detail: String?
        let creator_uid: String
        let creator_name: String?
        let created_at: String
        let selection_deadline: String
        let reading_deadline: String
        let distributed: Bool
        let expires_at: String
    }
    private struct ParticipantRow: Decodable {
        let uid: String
        let name: String?
        let is_creator: Bool
        let joined_at: String
    }
    private struct AssignmentRow: Decodable {
        let psalm_id: Int
        let uid: String
        let name: String?
        let by_creator: Bool
        let assigned_at: String?
    }

    private struct CreateChainParams: Encodable, Sendable {
        let p_name: String
        let p_intention_type: String
        let p_intention_detail: String
        let p_creator_name: String
        let p_selection_deadline: String
        let p_reading_deadline: String
        let p_expires_at: String
    }
    private struct AssignRemainingParams: Encodable, Sendable {
        let p_chain_id: String
        let p_name: String
    }
    private struct ParticipantUpsert: Encodable, Sendable {
        let chain_id: String
        let uid: String
        let name: String
        let is_creator: Bool
    }
    private struct AssignmentInsert: Encodable, Sendable {
        let chain_id: String
        let psalm_id: Int
        let uid: String
        let name: String
        let by_creator: Bool
    }
    private struct DistributeUpdate: Encodable, Sendable {
        let distributed: Bool
    }
    private struct DeviceTokenRow: Encodable, Sendable {
        let token: String
        let uid: String
        let platform: String
        let locale: String
    }
    private struct ChainUpdate: Encodable, Sendable {
        let name: String
        let intention_type: String
        let intention_detail: String
        let selection_deadline: String
        let reading_deadline: String
        let expires_at: String
    }
    private struct RemoveParticipantParams: Encodable, Sendable {
        let p_chain_id: String
        let p_uid: String
    }
    private struct ExtendSelectionParams: Encodable, Sendable {
        let p_chain_id: String
        let p_new_deadline: String
    }

    // MARK: - Mapping DTO → modèles applicatifs (inchangés)

    private static func chain(from r: ChainRow) -> TehilimChain {
        TehilimChain(
            id: r.id,
            name: r.name,
            intentionType: ChainIntention(rawValue: r.intention_type) ?? .reussite,
            intentionDetail: r.intention_detail ?? "",
            creatorUid: r.creator_uid.lowercased(),
            creatorName: r.creator_name ?? "",
            createdAt: parseDate(r.created_at),
            selectionDeadline: parseDate(r.selection_deadline),
            readingDeadline: parseDate(r.reading_deadline),
            distributed: r.distributed,
            expiresAt: parseDate(r.expires_at)
        )
    }

    private static func participant(from r: ParticipantRow) -> ChainParticipant {
        ChainParticipant(
            id: r.uid.lowercased(),
            name: r.name ?? "—",
            isCreator: r.is_creator,
            joinedAt: parseDate(r.joined_at)
        )
    }

    private static func assignment(from r: AssignmentRow) -> ChainAssignment {
        ChainAssignment(
            id: String(r.psalm_id),
            uid: r.uid.lowercased(),
            name: r.name ?? "—",
            byCreator: r.by_creator,
            assignedAt: parseDate(r.assigned_at)
        )
    }

    // MARK: - Décodage des deltas Realtime (DTO/mappers privés → modèles)

    private static let rtDecoder = JSONDecoder()
    private struct AssignmentKey: Decodable { let psalm_id: Int }
    private struct ParticipantKey: Decodable { let uid: String }

    /// INSERT/UPDATE d'une attribution → (psalmId, modèle). nil sinon.
    func parsedAssignment(from action: AnyAction) -> (psalmId: Int, value: ChainAssignment)? {
        let row: AssignmentRow?
        switch action {
        case .insert(let a): row = try? a.decodeRecord(decoder: Self.rtDecoder)
        case .update(let a): row = try? a.decodeRecord(decoder: Self.rtDecoder)
        default: row = nil
        }
        guard let r = row else { return nil }
        return (r.psalm_id, Self.assignment(from: r))
    }
    /// DELETE d'une attribution → psalmId (PK dans oldRecord). nil sinon.
    func deletedAssignmentPsalmId(from action: AnyAction) -> Int? {
        guard case .delete(let a) = action,
              let k: AssignmentKey = try? a.decodeOldRecord(decoder: Self.rtDecoder) else { return nil }
        return k.psalm_id
    }
    /// INSERT/UPDATE d'un participant → modèle. nil sinon.
    func parsedParticipant(from action: AnyAction) -> ChainParticipant? {
        let row: ParticipantRow?
        switch action {
        case .insert(let a): row = try? a.decodeRecord(decoder: Self.rtDecoder)
        case .update(let a): row = try? a.decodeRecord(decoder: Self.rtDecoder)
        default: row = nil
        }
        return row.map { Self.participant(from: $0) }
    }
    /// DELETE d'un participant → uid (PK dans oldRecord). nil sinon.
    func deletedParticipantUid(from action: AnyAction) -> String? {
        guard case .delete(let a) = action,
              let k: ParticipantKey = try? a.decodeOldRecord(decoder: Self.rtDecoder) else { return nil }
        return k.uid.lowercased()
    }

    // MARK: - Dates (Postgres timestamptz ↔ Date)

    private static let isoFractional: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return f
    }()
    private static let isoPlain: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime]
        return f
    }()

    /// Sérialise une `Date` en ISO8601 (UTC) pour Postgres.
    static func iso(_ date: Date) -> String { isoFractional.string(from: date) }

    /// Parse un timestamptz Postgres. Postgres peut renvoyer des microsecondes
    /// (6 décimales) ; `ISO8601DateFormatter` n'en gère que 3 → on tronque la
    /// fraction à 3 chiffres avant le parsing, avec repli sans fraction.
    static func parseDate(_ s: String?) -> Date {
        guard let s, !s.isEmpty else { return Date() }
        let normalized = normalizeFraction(s)
        return isoFractional.date(from: normalized)
            ?? isoPlain.date(from: normalized)
            ?? isoPlain.date(from: stripFraction(normalized))
            ?? Date()
    }

    /// Tronque la partie fractionnaire des secondes à 3 chiffres (millisecondes).
    private static func normalizeFraction(_ s: String) -> String {
        guard let dot = s.firstIndex(of: ".") else { return s }
        var i = s.index(after: dot)
        var digits = 0
        while i < s.endIndex, s[i].isNumber {
            digits += 1; i = s.index(after: i)
            if digits == 3 { break }
        }
        // Saute les chiffres surnuméraires (4ᵉ, 5ᵉ, 6ᵉ…) jusqu'au fuseau (+/-/Z).
        var j = i
        while j < s.endIndex, s[j].isNumber { j = s.index(after: j) }
        return String(s[s.startIndex..<i]) + String(s[j..<s.endIndex])
    }

    /// Retire toute la fraction de secondes (`.123` → ``), repli ultime.
    private static func stripFraction(_ s: String) -> String {
        guard let dot = s.firstIndex(of: ".") else { return s }
        var i = s.index(after: dot)
        while i < s.endIndex, s[i].isNumber { i = s.index(after: i) }
        return String(s[s.startIndex..<dot]) + String(s[i..<s.endIndex])
    }
}

/// Session temps réel d'**une** chaîne : charge l'état (chaîne + participants +
/// attributions) et écoute les changements via **Supabase Realtime** (Postgres
/// changes). `start()/stop()` permettent de couper l'écoute en arrière-plan et
/// de la reprendre au premier plan — API identique à la version Firebase.
///
/// Sur chaque évènement realtime d'une table, on **recharge** la collection
/// concernée (SELECT léger ≤ 150 lignes) : simple et robuste, sans décodage de
/// delta fragile, parfaitement adapté à l'échelle (partage entre proches).
@MainActor
final class ChainSession: ObservableObject {
    @Published private(set) var chain: TehilimChain?
    @Published private(set) var participants: [ChainParticipant] = []
    @Published private(set) var assignments: [Int: ChainAssignment] = [:]
    @Published private(set) var loadError: String?

    let chainId: String
    private let service = ChainService()
    private var channel: RealtimeChannelV2?
    private var tasks: [Task<Void, Never>] = []

    var currentUid: String? { service.currentUid }

    init(chainId: String) {
        self.chainId = chainId
        start()
    }

    /// Charge l'état puis attache l'écoute realtime (idempotent).
    func start() {
        guard channel == nil, let client = SupabaseManager.shared.client else { return }

        tasks.append(Task { [weak self] in await self?.reloadAll() })

        let channel = client.realtimeV2.channel("chain:\(chainId)")
        self.channel = channel

        let chainChanges = channel.postgresChange(
            AnyAction.self, schema: "public", table: "chains", filter: "id=eq.\(chainId)")
        let partChanges = channel.postgresChange(
            AnyAction.self, schema: "public", table: "chain_participants", filter: "chain_id=eq.\(chainId)")
        let asgChanges = channel.postgresChange(
            AnyAction.self, schema: "public", table: "chain_assignments", filter: "chain_id=eq.\(chainId)")

        tasks.append(Task { await channel.subscribe() })
        // `chains` : évènement rare (création/distribution/prolongation/suppression)
        // → un refetch suffit. Les tables chaudes (participants, assignments)
        // appliquent des **deltas** issus du payload Realtime, sans refetch :
        // c'est ce qui supprime la « tempête » de requêtes pendant la sélection.
        tasks.append(Task { [weak self] in
            for await _ in chainChanges { await self?.reloadChain() }
        })
        tasks.append(Task { [weak self] in
            for await change in partChanges { await self?.applyParticipant(change) }
        })
        tasks.append(Task { [weak self] in
            for await change in asgChanges { await self?.applyAssignment(change) }
        })
    }

    // MARK: - Application des deltas Realtime (sans refetch)
    // Le décodage vit dans ChainService (qui détient les DTO/mappers privés) ;
    // ici on ne fait que muter l'état publié.

    private func applyAssignment(_ action: AnyAction) {
        if let id = service.deletedAssignmentPsalmId(from: action) {
            assignments.removeValue(forKey: id)
        } else if let pair = service.parsedAssignment(from: action) {
            assignments[pair.psalmId] = pair.value
        }
    }

    private func applyParticipant(_ action: AnyAction) {
        if let uid = service.deletedParticipantUid(from: action) {
            participants.removeAll { $0.id == uid }
        } else if let p = service.parsedParticipant(from: action) {
            if let i = participants.firstIndex(where: { $0.id == p.id }) { participants[i] = p }
            else { participants.append(p) }
            participants.sort { $0.joinedAt < $1.joinedAt }
        }
    }

    /// Détache l'écoute (arrière-plan / écran fermé).
    func stop() {
        tasks.forEach { $0.cancel() }
        tasks.removeAll()
        if let channel {
            Task { await channel.unsubscribe() }
        }
        channel = nil
    }

    deinit {
        tasks.forEach { $0.cancel() }
    }

    // MARK: - Chargements

    private func reloadAll() async {
        await reloadChain()
        await reloadParticipants()
        await reloadAssignments()
    }

    private func reloadChain() async {
        do {
            if let c = try await service.fetchChain(id: chainId) {
                self.chain = c
            } else {
                self.loadError = "introuvable"
            }
        } catch {
            // garde l'état courant
        }
    }

    private func reloadParticipants() async {
        if let list = try? await service.fetchParticipants(chainId: chainId) {
            self.participants = list.sorted { $0.joinedAt < $1.joinedAt }
        }
    }

    private func reloadAssignments() async {
        if let map = try? await service.fetchBoard(chainId: chainId) {
            self.assignments = map
        }
    }

    // MARK: - Mise à jour optimiste (UI instantanée, réconciliée par le realtime)

    /// Affiche immédiatement la case comme « à moi » avant la confirmation serveur.
    func optimisticSelect(_ psalmId: Int, uid: String, name: String) {
        assignments[psalmId] = ChainAssignment(
            id: String(psalmId), uid: uid, name: name, byCreator: false, assignedAt: Date())
    }
    /// Libère immédiatement la case en local.
    func optimisticDeselect(_ psalmId: Int) {
        assignments.removeValue(forKey: psalmId)
    }
    /// Recharge les attributions (réconciliation après échec d'une action optimiste).
    func refreshAssignments() async { await reloadAssignments() }

    // MARK: - Dérivés pour l'UI (inchangés)

    var assignedCount: Int { assignments.count }
    var progress: Double { Double(assignedCount) / Double(TehilimChain.totalPsalms) }
    var isFullyAssigned: Bool { assignedCount >= TehilimChain.totalPsalms }

    var isCurrentUserParticipant: Bool {
        guard let uid = currentUid else { return false }
        return participants.contains { $0.id == uid }
    }
    var isCurrentUserCreator: Bool {
        guard let uid = currentUid, let chain else { return false }
        return chain.creatorUid == uid
    }
    var myPsalmIds: [Int] {
        guard let uid = currentUid else { return [] }
        return assignments.values.filter { $0.uid == uid }.map(\.psalmId).sorted()
    }
}
