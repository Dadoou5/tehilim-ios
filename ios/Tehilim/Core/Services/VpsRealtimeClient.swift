import Foundation

/// Client WebSocket vers le **serveur realtime self-hosté** (VPS) — alternative
/// à Supabase Realtime, activée par le flag distant `realtime_source` (RPC).
///
/// Protocole (cf. `server/REALTIME_CONTRACT.md`) :
///   →  { "type":"auth", "token":<jwt supabase> }
///   ←  { "type":"auth_ok" } | { "type":"error", … }
///   →  { "type":"subscribe", "chainId":… }      ← { "type":"subscribed", … }
///   ←  { "type":"delta", "chainId", "table", "op", "row", "old" }
///
/// Politique d'application (mirror du chemin Supabase de `ChainSession`) :
///   - `chain_assignments` (table chaude) → **delta** sans refetch ;
///   - `chain_participants` / `chains` (rares) → simple **refetch** ;
///   - à chaque (ré)abonnement réussi → `onResync` (refetch complet, comble
///     les évènements manqués pendant une coupure).
///
/// Reconnexion automatique avec backoff (1 s → 30 s) tant que `stop()` n'a pas
/// été appelé. Le serveur ping toutes les 30 s (pong automatique URLSession).
final class VpsRealtimeClient: NSObject {

    /// Endpoint public (Nginx → 127.0.0.1:3001). Même convention de constante
    /// que `ChainShareLink.webBaseURL`.
    static let endpoint = URL(string: "wss://tehilimapp.com/realtime")!

    struct Delta {
        let table: String
        let op: String          // INSERT | UPDATE | DELETE
        let row: [String: Any]?
        let old: [String: Any]?
    }

    private let chainId: String
    /// JWT Supabase frais (la session WS peut survivre à l'expiration du token
    /// → on redemande un token à chaque (re)connexion).
    private let tokenProvider: () async -> String?
    private let onDelta: @MainActor (Delta) -> Void
    private let onResync: @MainActor () -> Void

    private var task: URLSessionWebSocketTask?
    private var stopped = false
    private var backoff: TimeInterval = 1
    private var loop: Task<Void, Never>?

    init(chainId: String,
         tokenProvider: @escaping () async -> String?,
         onDelta: @escaping @MainActor (Delta) -> Void,
         onResync: @escaping @MainActor () -> Void) {
        self.chainId = chainId
        self.tokenProvider = tokenProvider
        self.onDelta = onDelta
        self.onResync = onResync
    }

    func start() {
        guard loop == nil else { return }
        stopped = false
        loop = Task { [weak self] in await self?.runLoop() }
    }

    func stop() {
        stopped = true
        loop?.cancel()
        loop = nil
        task?.cancel(with: .normalClosure, reason: nil)
        task = nil
    }

    deinit { task?.cancel(with: .normalClosure, reason: nil) }

    // MARK: - Boucle connexion / réception

    private func runLoop() async {
        while !stopped && !Task.isCancelled {
            do {
                try await connectAndListen()
            } catch {
                // coupure réseau / serveur — on retombe sur le backoff
            }
            guard !stopped && !Task.isCancelled else { return }
            try? await Task.sleep(nanoseconds: UInt64(backoff * 1_000_000_000))
            backoff = min(backoff * 2, 30)
        }
    }

    private func connectAndListen() async throws {
        guard let token = await tokenProvider() else {
            throw URLError(.userAuthenticationRequired)
        }
        let task = URLSession.shared.webSocketTask(with: Self.endpoint)
        self.task = task
        task.resume()

        try await send(["type": "auth", "token": token])

        var authed = false
        var subscribed = false

        while !stopped && !Task.isCancelled {
            let message = try await task.receive()
            guard let obj = Self.json(from: message), let type = obj["type"] as? String else { continue }

            switch type {
            case "auth_ok":
                authed = true
                try await send(["type": "subscribe", "chainId": chainId])
            case "subscribed":
                subscribed = true
                backoff = 1
                // Resync : comble tout évènement manqué avant/pendant l'abonnement.
                await MainActor.run { onResync() }
            case "delta":
                guard authed, subscribed,
                      obj["chainId"] as? String == chainId,
                      let table = obj["table"] as? String,
                      let op = obj["op"] as? String else { continue }
                let delta = Delta(table: table, op: op,
                                  row: obj["row"] as? [String: Any],
                                  old: obj["old"] as? [String: Any])
                await MainActor.run { onDelta(delta) }
            case "error":
                // auth refusée / message invalide → on ferme, le backoff rejouera
                // avec un token frais.
                throw URLError(.userAuthenticationRequired)
            default:
                break
            }
        }
    }

    // MARK: - Encodage / décodage

    private func send(_ payload: [String: Any]) async throws {
        let data = try JSONSerialization.data(withJSONObject: payload)
        guard let text = String(data: data, encoding: .utf8) else { return }
        try await task?.send(.string(text))
    }

    private static func json(from message: URLSessionWebSocketTask.Message) -> [String: Any]? {
        let data: Data?
        switch message {
        case .string(let s): data = s.data(using: .utf8)
        case .data(let d): data = d
        @unknown default: data = nil
        }
        guard let data else { return nil }
        return (try? JSONSerialization.jsonObject(with: data)) as? [String: Any]
    }
}
