import Foundation
import FirebaseCore
import FirebaseFirestore
import FirebaseAuth

/// Couche d'accès Firestore pour la feature « Chaîne de Tehilim ».
///
/// **Optimisation quota** : l'état des attributions est stocké dans **un seul
/// document** `chains/{id}/state/board` (champ map `a` = psalmId → {u,n,c})
/// plutôt qu'en 150 docs. Charger la grille = **1 lecture** au lieu de 150
/// (crucial quand un lien WhatsApp est ouvert par des dizaines de personnes).
/// Le verrou reste atomique via une **transaction** sur ce doc unique.
final class ChainService {

    static var isAvailable: Bool { FirebaseApp.app() != nil }

    private var db: Firestore { Firestore.firestore() }

    enum K {
        static let chains = "chains"
        static let participants = "participants"
        static let state = "state", board = "board"
        // Clés courtes dans la map du board (limite la taille du doc).
        static let bMap = "a", bU = "u", bN = "n", bC = "c"
        static let name = "name", intentionType = "intentionType", intentionDetail = "intentionDetail"
        static let creatorUid = "creatorUid", creatorName = "creatorName"
        static let createdAt = "createdAt", selectionDeadline = "selectionDeadline"
        static let readingDeadline = "readingDeadline", distributed = "distributed", expiresAt = "expiresAt"
        static let isCreator = "isCreator", joinedAt = "joinedAt"
    }

    static let expiryGraceSeconds: TimeInterval = 7 * 24 * 3600

    var currentUid: String? { Auth.auth().currentUser?.uid }

    @discardableResult
    func ensureSignedIn() async throws -> String {
        if let uid = Auth.auth().currentUser?.uid { return uid }
        let result = try await Auth.auth().signInAnonymously()
        return result.user.uid
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
        let uid = try await ensureSignedIn()
        let now = Date()
        let chainRef = db.collection(K.chains).document()
        let data: [String: Any] = [
            K.name: name,
            K.intentionType: intention.rawValue,
            K.intentionDetail: detail,
            K.creatorUid: uid,
            K.creatorName: creatorName,
            K.createdAt: now,
            K.selectionDeadline: now.addingTimeInterval(selectionDuration),
            K.readingDeadline: readingDeadline,
            K.distributed: false,
            K.expiresAt: readingDeadline.addingTimeInterval(Self.expiryGraceSeconds)
        ]
        try await chainRef.setData(data)
        try await chainRef.collection(K.participants).document(uid).setData([
            K.name: creatorName, K.isCreator: true, K.joinedAt: now
        ])
        return chainRef.documentID
    }

    func join(chainId: String, name: String) async throws {
        let uid = try await ensureSignedIn()
        try await db.collection(K.chains).document(chainId)
            .collection(K.participants).document(uid)
            .setData([K.name: name, K.isCreator: false, K.joinedAt: Date()], merge: true)
    }

    /// Réserve un Tehilim (transaction sur le doc board : échoue si déjà pris).
    func select(chainId: String, psalmId: Int, name: String) async throws {
        let uid = try await ensureSignedIn()
        let ref = boardRef(chainId)
        _ = try await db.runTransaction { txn, errorPtr -> Any? in
            let snap: DocumentSnapshot
            do { snap = try txn.getDocument(ref) }
            catch { errorPtr?.pointee = error as NSError; return nil }
            var a = (snap.data()?[K.bMap] as? [String: [String: Any]]) ?? [:]
            if a["\(psalmId)"] != nil {
                errorPtr?.pointee = NSError(domain: "ChainService", code: 409,
                    userInfo: [NSLocalizedDescriptionKey: "Tehilim déjà pris"])
                return nil
            }
            a["\(psalmId)"] = [K.bU: uid, K.bN: name, K.bC: false]
            txn.setData([K.bMap: a], forDocument: ref, merge: true)
            return nil
        }
    }

    /// Libère un Tehilim réservé par soi-même.
    func deselect(chainId: String, psalmId: Int) async throws {
        let uid = try await ensureSignedIn()
        let ref = boardRef(chainId)
        _ = try await db.runTransaction { txn, errorPtr -> Any? in
            let snap: DocumentSnapshot
            do { snap = try txn.getDocument(ref) }
            catch { errorPtr?.pointee = error as NSError; return nil }
            var a = (snap.data()?[K.bMap] as? [String: [String: Any]]) ?? [:]
            if let entry = a["\(psalmId)"], (entry[K.bU] as? String) == uid {
                a.removeValue(forKey: "\(psalmId)")
                txn.setData([K.bMap: a], forDocument: ref, merge: true)
            }
            return nil
        }
    }

    /// (Créateur) Attribue tous les Tehilim restants à lui-même.
    func assignRemaining(chainId: String, name: String) async throws {
        let uid = try await ensureSignedIn()
        let ref = boardRef(chainId)
        _ = try await db.runTransaction { txn, errorPtr -> Any? in
            let snap: DocumentSnapshot
            do { snap = try txn.getDocument(ref) }
            catch { errorPtr?.pointee = error as NSError; return nil }
            var a = (snap.data()?[K.bMap] as? [String: [String: Any]]) ?? [:]
            for p in 1...TehilimChain.totalPsalms where a["\(p)"] == nil {
                a["\(p)"] = [K.bU: uid, K.bN: name, K.bC: true]
            }
            txn.setData([K.bMap: a], forDocument: ref, merge: true)
            return nil
        }
    }

    func distribute(chainId: String) async throws {
        try await ensureSignedIn()
        try await db.collection(K.chains).document(chainId)
            .updateData([K.distributed: true])
    }

    private func boardRef(_ chainId: String) -> DocumentReference {
        db.collection(K.chains).document(chainId).collection(K.state).document(K.board)
    }

    // MARK: - Lecture ponctuelle

    func fetchChain(id: String) async throws -> TehilimChain? {
        let doc = try await db.collection(K.chains).document(id).getDocument()
        return Self.chain(from: doc)
    }

    func fetchBoard(chainId: String) async throws -> [Int: ChainAssignment] {
        let snap = try await boardRef(chainId).getDocument()
        return Self.board(from: snap)
    }

    // MARK: - Session temps réel

    @MainActor
    func session(chainId: String) -> ChainSession { ChainSession(chainId: chainId) }

    // MARK: - Mapping Firestore → modèles

    static func chain(from doc: DocumentSnapshot) -> TehilimChain? {
        guard let d = doc.data(),
              let name = d[K.name] as? String,
              let intentionRaw = d[K.intentionType] as? String,
              let intention = ChainIntention(rawValue: intentionRaw),
              let creatorUid = d[K.creatorUid] as? String
        else { return nil }
        return TehilimChain(
            id: doc.documentID,
            name: name,
            intentionType: intention,
            intentionDetail: d[K.intentionDetail] as? String ?? "",
            creatorUid: creatorUid,
            creatorName: d[K.creatorName] as? String ?? "",
            createdAt: (d[K.createdAt] as? Timestamp)?.dateValue() ?? Date(),
            selectionDeadline: (d[K.selectionDeadline] as? Timestamp)?.dateValue() ?? Date(),
            readingDeadline: (d[K.readingDeadline] as? Timestamp)?.dateValue() ?? Date(),
            distributed: d[K.distributed] as? Bool ?? false,
            expiresAt: (d[K.expiresAt] as? Timestamp)?.dateValue() ?? Date()
        )
    }

    static func participant(from doc: DocumentSnapshot) -> ChainParticipant? {
        guard let d = doc.data() else { return nil }
        return ChainParticipant(
            id: doc.documentID,
            name: d[K.name] as? String ?? "—",
            isCreator: d[K.isCreator] as? Bool ?? false,
            joinedAt: (d[K.joinedAt] as? Timestamp)?.dateValue() ?? Date()
        )
    }

    /// Décode la map du doc board → dictionnaire psalmId → attribution.
    static func board(from snap: DocumentSnapshot) -> [Int: ChainAssignment] {
        guard let map = snap.data()?[K.bMap] as? [String: [String: Any]] else { return [:] }
        var out: [Int: ChainAssignment] = [:]
        for (key, v) in map {
            guard let pid = Int(key), let uid = v[K.bU] as? String else { continue }
            out[pid] = ChainAssignment(
                id: key, uid: uid,
                name: v[K.bN] as? String ?? "—",
                byCreator: v[K.bC] as? Bool ?? false,
                assignedAt: Date()
            )
        }
        return out
    }
}

/// Session temps réel d'**une** chaîne : écoute le doc chaîne + participants +
/// le doc board (attributions). `start()/stop()` permettent de couper l'écoute
/// en arrière-plan (économie de quota) et de la reprendre au premier plan.
@MainActor
final class ChainSession: ObservableObject {
    @Published private(set) var chain: TehilimChain?
    @Published private(set) var participants: [ChainParticipant] = []
    @Published private(set) var assignments: [Int: ChainAssignment] = [:]
    @Published private(set) var loadError: String?

    let chainId: String
    var currentUid: String? { Auth.auth().currentUser?.uid }

    private var listeners: [ListenerRegistration] = []

    init(chainId: String) {
        self.chainId = chainId
        start()
    }

    /// Attache les 3 listeners (idempotent).
    func start() {
        guard listeners.isEmpty else { return }
        let db = Firestore.firestore()
        let chainRef = db.collection(ChainService.K.chains).document(chainId)

        listeners.append(chainRef.addSnapshotListener { [weak self] snap, _ in
            guard let self, let snap else { return }
            if let c = ChainService.chain(from: snap) { self.chain = c }
            else if !snap.exists { self.loadError = "introuvable" }
        })

        listeners.append(chainRef.collection(ChainService.K.participants)
            .addSnapshotListener { [weak self] snap, _ in
                guard let self, let snap else { return }
                self.participants = snap.documents
                    .compactMap { ChainService.participant(from: $0) }
                    .sorted { $0.joinedAt < $1.joinedAt }
            })

        listeners.append(chainRef.collection(ChainService.K.state).document(ChainService.K.board)
            .addSnapshotListener { [weak self] snap, _ in
                guard let self, let snap else { return }
                self.assignments = ChainService.board(from: snap)
            })
    }

    /// Détache les listeners (arrière-plan / écran fermé) → plus aucune lecture.
    func stop() {
        listeners.forEach { $0.remove() }
        listeners.removeAll()
    }

    deinit { listeners.forEach { $0.remove() } }

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
