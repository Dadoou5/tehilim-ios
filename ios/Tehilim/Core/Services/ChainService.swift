import Foundation
import FirebaseCore
import FirebaseFirestore
import FirebaseAuth

/// Couche d'accès Firestore pour la feature « Chaîne de Tehilim ».
///
/// Toutes les écritures passent par ici. Les lectures **temps réel** sont
/// fournies par `ChainSession` (un `ObservableObject` par chaîne ouverte).
/// L'identité est l'**uid anonyme** Firebase (stable par appareil).
final class ChainService {

    /// Firebase est-il configuré ? (false si `GoogleService-Info.plist` absent →
    /// la feature reste masquée, l'app fonctionne 100 % en local.)
    static var isAvailable: Bool { FirebaseApp.app() != nil }

    private var db: Firestore { Firestore.firestore() }

    // Noms de champs Firestore — centralisés pour cohérence iOS/Android.
    enum K {
        static let chains = "chains"
        static let participants = "participants"
        static let assignments = "assignments"
        static let name = "name", intentionType = "intentionType", intentionDetail = "intentionDetail"
        static let creatorUid = "creatorUid", creatorName = "creatorName"
        static let createdAt = "createdAt", selectionDeadline = "selectionDeadline"
        static let readingDeadline = "readingDeadline", distributed = "distributed", expiresAt = "expiresAt"
        static let isCreator = "isCreator", joinedAt = "joinedAt"
        static let uid = "uid", byCreator = "byCreator", assignedAt = "assignedAt"
    }

    /// Marge ajoutée à la fin de lecture avant suppression cloud (TTL).
    static let expiryGraceSeconds: TimeInterval = 7 * 24 * 3600

    var currentUid: String? { Auth.auth().currentUser?.uid }

    /// Retourne l'uid courant, en se connectant anonymement si besoin.
    @discardableResult
    func ensureSignedIn() async throws -> String {
        if let uid = Auth.auth().currentUser?.uid { return uid }
        let result = try await Auth.auth().signInAnonymously()
        return result.user.uid
    }

    // MARK: - Écritures

    /// Crée une chaîne (le créateur devient participant). Retourne l'id.
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

    /// Rejoint une chaîne (crée/maj le participant pour l'uid courant).
    func join(chainId: String, name: String) async throws {
        let uid = try await ensureSignedIn()
        try await db.collection(K.chains).document(chainId)
            .collection(K.participants).document(uid)
            .setData([K.name: name, K.isCreator: false, K.joinedAt: Date()], merge: true)
    }

    /// Réserve un Tehilim (transaction : échoue si déjà pris). `name` dénormalisé.
    func select(chainId: String, psalmId: Int, name: String) async throws {
        let uid = try await ensureSignedIn()
        let ref = assignmentRef(chainId: chainId, psalmId: psalmId)
        _ = try await db.runTransaction { txn, errorPtr -> Any? in
            let snap: DocumentSnapshot
            do { snap = try txn.getDocument(ref) }
            catch { errorPtr?.pointee = error as NSError; return nil }
            if snap.exists {
                errorPtr?.pointee = NSError(
                    domain: "ChainService", code: 409,
                    userInfo: [NSLocalizedDescriptionKey: "Tehilim déjà pris"]
                )
                return nil
            }
            txn.setData([
                K.uid: uid, K.name: name, K.byCreator: false, K.assignedAt: Date()
            ], forDocument: ref)
            return nil
        }
    }

    /// Libère un Tehilim que l'on a soi-même réservé (ou créateur, via règles).
    func deselect(chainId: String, psalmId: Int) async throws {
        try await ensureSignedIn()
        try await assignmentRef(chainId: chainId, psalmId: psalmId).delete()
    }

    /// (Créateur) Attribue tous les Tehilim restants à lui-même.
    func assignRemaining(chainId: String, name: String) async throws {
        let uid = try await ensureSignedIn()
        let col = db.collection(K.chains).document(chainId).collection(K.assignments)
        let existing = try await col.getDocuments()
        let taken = Set(existing.documents.compactMap { Int($0.documentID) })
        let batch = db.batch()
        for p in 1...TehilimChain.totalPsalms where !taken.contains(p) {
            batch.setData(
                [K.uid: uid, K.name: name, K.byCreator: true, K.assignedAt: Date()],
                forDocument: col.document("\(p)")
            )
        }
        try await batch.commit()
    }

    /// (Créateur) Marque la chaîne distribuée → verrouille la sélection.
    func distribute(chainId: String) async throws {
        try await ensureSignedIn()
        try await db.collection(K.chains).document(chainId)
            .updateData([K.distributed: true])
    }

    private func assignmentRef(chainId: String, psalmId: Int) -> DocumentReference {
        db.collection(K.chains).document(chainId)
            .collection(K.assignments).document("\(psalmId)")
    }

    // MARK: - Lecture ponctuelle

    /// Charge une chaîne une fois (utilisé à l'ouverture par lien avant la session live).
    func fetchChain(id: String) async throws -> TehilimChain? {
        let doc = try await db.collection(K.chains).document(id).getDocument()
        return Self.chain(from: doc)
    }

    // MARK: - Session temps réel

    /// Crée une session live (3 listeners) pour une chaîne. À retenir par la vue
    /// via `@StateObject` ; les listeners se détachent au `deinit`.
    @MainActor
    func session(chainId: String) -> ChainSession {
        ChainSession(chainId: chainId, db: db, currentUid: currentUid)
    }

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

    static func assignment(from doc: DocumentSnapshot) -> ChainAssignment? {
        guard let d = doc.data(), let uid = d[K.uid] as? String else { return nil }
        return ChainAssignment(
            id: doc.documentID,
            uid: uid,
            name: d[K.name] as? String ?? "—",
            byCreator: d[K.byCreator] as? Bool ?? false,
            assignedAt: (d[K.assignedAt] as? Timestamp)?.dateValue() ?? Date()
        )
    }
}

/// Session temps réel d'**une** chaîne ouverte : écoute le doc chaîne + les
/// participants + les attributions, et republie en `@Published` pour SwiftUI.
@MainActor
final class ChainSession: ObservableObject {
    @Published private(set) var chain: TehilimChain?
    @Published private(set) var participants: [ChainParticipant] = []
    /// psalmId (1..150) → attribution.
    @Published private(set) var assignments: [Int: ChainAssignment] = [:]
    @Published private(set) var loadError: String?

    let chainId: String
    let currentUid: String?

    private var listeners: [ListenerRegistration] = []

    init(chainId: String, db: Firestore, currentUid: String?) {
        self.chainId = chainId
        self.currentUid = currentUid
        let chainRef = db.collection(ChainService.K.chains).document(chainId)

        listeners.append(chainRef.addSnapshotListener { [weak self] snap, _ in
            guard let self, let snap else { return }
            if let c = ChainService.chain(from: snap) {
                self.chain = c
            } else if !snap.exists {
                self.loadError = "introuvable"
            }
        })

        listeners.append(chainRef.collection(ChainService.K.participants)
            .addSnapshotListener { [weak self] snap, _ in
                guard let self, let snap else { return }
                self.participants = snap.documents
                    .compactMap { ChainService.participant(from: $0) }
                    .sorted { $0.joinedAt < $1.joinedAt }
            })

        listeners.append(chainRef.collection(ChainService.K.assignments)
            .addSnapshotListener { [weak self] snap, _ in
                guard let self, let snap else { return }
                var map: [Int: ChainAssignment] = [:]
                for doc in snap.documents {
                    if let a = ChainService.assignment(from: doc) { map[a.psalmId] = a }
                }
                self.assignments = map
            })
    }

    deinit { listeners.forEach { $0.remove() } }

    // MARK: - Dérivés pour l'UI

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
    /// Numéros des Tehilim réservés par l'utilisateur courant.
    var myPsalmIds: [Int] {
        guard let uid = currentUid else { return [] }
        return assignments.values.filter { $0.uid == uid }.map(\.psalmId).sorted()
    }
}
