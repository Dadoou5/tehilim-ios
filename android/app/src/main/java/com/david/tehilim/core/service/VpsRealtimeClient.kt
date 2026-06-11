package com.david.tehilim.core.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

/**
 * Client WebSocket vers le **serveur realtime self-hosté** (VPS) — alternative à
 * Supabase Realtime, activée par le flag distant `realtime_source` (RPC).
 * Mirror Android de `VpsRealtimeClient.swift` (cf. `server/REALTIME_CONTRACT.md`).
 *
 * **Multiplexé** : une seule connexion WS partagée pour toutes les chaînes
 * écoutées (auth une fois, un `subscribe` par chaîne) — un téléphone = une
 * connexion, quel que soit le nombre de flows abonnés.
 *
 * Protocole :
 *   →  { "type":"auth", "token":<jwt supabase> }      ←  { "type":"auth_ok" }
 *   →  { "type":"subscribe", "chainId":… }            ←  { "type":"subscribed", … }
 *   ←  { "type":"delta", "chainId", "table", "op", "row", "old" }
 *
 * Reconnexion automatique (backoff 1 s → 30 s) tant qu'il reste des abonnés ;
 * à chaque (ré)abonnement réussi, `onResync()` est notifié (les flows refetchent
 * pour combler les évènements manqués). OkHttp répond automatiquement aux pings
 * serveur (30 s).
 */
object VpsRealtime {

    /** Endpoint public (Nginx → 127.0.0.1:3001). Même convention que
     *  `ChainShareLink.WEB_BASE_URL`. */
    const val ENDPOINT = "wss://tehilimapp.com/realtime"

    interface Listener {
        /** Delta brut d'une table de la chaîne (op = INSERT | UPDATE | DELETE). */
        fun onDelta(table: String, op: String, row: JsonObject?, old: JsonObject?)
        /** (Ré)abonnement réussi → refetch complet conseillé (trous comblés). */
        fun onResync()
    }

    /** Poignée d'abonnement ; `close()` désabonne (la connexion se ferme quand
     *  plus personne n'écoute). */
    fun interface Subscription : AutoCloseable

    private val json = Json { ignoreUnknownKeys = true }
    private val http by lazy {
        OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).build()
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val lock = Any()
    private val listeners = LinkedHashMap<String, MutableSet<Listener>>() // chainId → listeners
    private var tokenProvider: (suspend () -> String?)? = null
    private var socket: WebSocket? = null
    private var authed = false
    private var connecting = false
    private var reconnectJob: Job? = null
    private var backoffMs = 1_000L

    /** Abonne `listener` aux deltas de `chainId`. Thread-safe. */
    fun subscribe(chainId: String, provider: suspend () -> String?, listener: Listener): Subscription {
        synchronized(lock) {
            tokenProvider = provider
            listeners.getOrPut(chainId) { mutableSetOf() }.add(listener)
            when {
                socket == null && !connecting -> connect()
                authed -> sendSubscribe(chainId) // connexion déjà prête → abonne tout de suite
            }
        }
        return Subscription {
            synchronized(lock) {
                listeners[chainId]?.remove(listener)
                if (listeners[chainId]?.isEmpty() == true) {
                    listeners.remove(chainId)
                    if (authed) socket?.send("""{"type":"unsubscribe","chainId":"$chainId"}""")
                }
                if (listeners.isEmpty()) disconnect()
            }
        }
    }

    // MARK: - Connexion

    private fun connect() {
        connecting = true
        scope.launch {
            val token = runCatching { tokenProvider?.invoke() }.getOrNull()
            synchronized(lock) {
                if (listeners.isEmpty()) { connecting = false; return@launch }
                if (token == null) { connecting = false; scheduleReconnect(); return@launch }
                val request = Request.Builder().url(ENDPOINT).build()
                socket = http.newWebSocket(request, SocketListener(token))
            }
        }
    }

    private fun disconnect() {
        reconnectJob?.cancel(); reconnectJob = null
        socket?.close(1000, null); socket = null
        authed = false; connecting = false; backoffMs = 1_000L
    }

    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return
        val delayMs = backoffMs
        backoffMs = (backoffMs * 2).coerceAtMost(30_000L)
        reconnectJob = scope.launch {
            delay(delayMs)
            synchronized(lock) { if (listeners.isNotEmpty() && socket == null && !connecting) connect() }
        }
    }

    private fun sendSubscribe(chainId: String) {
        socket?.send("""{"type":"subscribe","chainId":"$chainId"}""")
    }

    private class SocketListener(private val token: String) : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            synchronized(lock) { connecting = false }
            val auth = buildJsonObject { put("type", "auth"); put("token", token) }
            webSocket.send(auth.toString())
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val obj = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return
            when (obj["type"]?.jsonPrimitive?.content) {
                "auth_ok" -> synchronized(lock) {
                    authed = true; backoffMs = 1_000L
                    listeners.keys.forEach { sendSubscribe(it) }
                }
                "subscribed" -> {
                    val chainId = obj["chainId"]?.jsonPrimitive?.content ?: return
                    snapshot(chainId).forEach { it.onResync() }
                }
                "delta" -> {
                    val chainId = obj["chainId"]?.jsonPrimitive?.content ?: return
                    val table = obj["table"]?.jsonPrimitive?.content ?: return
                    val op = obj["op"]?.jsonPrimitive?.content ?: return
                    val row = obj["row"] as? JsonObject
                    val old = obj["old"] as? JsonObject
                    snapshot(chainId).forEach { it.onDelta(table, op, row, old) }
                }
                "error" -> webSocket.close(1000, null) // auth/refus → reconnexion (token frais)
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            synchronized(lock) {
                if (socket === webSocket) { socket = null; authed = false; connecting = false }
                if (listeners.isNotEmpty()) scheduleReconnect()
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            synchronized(lock) {
                if (socket === webSocket) { socket = null; authed = false; connecting = false }
                if (listeners.isNotEmpty()) scheduleReconnect()
            }
        }

        private fun snapshot(chainId: String): List<Listener> =
            synchronized(lock) { listeners[chainId]?.toList() ?: emptyList() }
    }
}
