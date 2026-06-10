// Serveur WebSocket : authentifie les clients (JWT Supabase), gère les
// abonnements par chaîne, et diffuse les deltas reçus de Postgres.
//
// Protocole (JSON, un objet par message) :
//   client → serveur :
//     { "type":"auth", "token":"<jwt supabase>" }
//     { "type":"subscribe",   "chainId":"<uuid>" }
//     { "type":"unsubscribe", "chainId":"<uuid>" }
//   serveur → client :
//     { "type":"auth_ok" } | { "type":"error", "error":"..." }
//     { "type":"subscribed", "chainId":"..." } | { "type":"unsubscribed", "chainId":"..." }
//     { "type":"delta", "chainId":"...", "table":"...", "op":"...", "row":{...}, "old":{...} }
//
// Autorisation : tout JWT `authenticated` valide peut s'abonner à n'importe
// quelle chaîne (réplique la RLS lecture = using(true) ; l'id de chaîne EST le
// secret d'accès, partagé par lien/QR).

import http from "node:http";
import { WebSocketServer, WebSocket } from "ws";
import { log } from "../log.js";
import { verifySupabaseJwt } from "./auth.js";
import type { Delta } from "./pgListener.js";

const AUTH_TIMEOUT_MS = 8_000;
const HEARTBEAT_MS = 30_000;
const MAX_SUBSCRIPTIONS = 20;

interface ClientState {
  authed: boolean;
  uid: string | null;
  alive: boolean;
  subs: Set<string>;
  authTimer: NodeJS.Timeout | null;
}

export interface RealtimeServer {
  broadcast: (d: Delta) => void;
  close: () => Promise<void>;
}

export function createRealtimeServer(opts: { port: number; jwtSecret: string }): RealtimeServer {
  const clients = new Map<WebSocket, ClientState>();
  const topics = new Map<string, Set<WebSocket>>(); // chainId → sockets

  const httpServer = http.createServer((req, res) => {
    if (req.url === "/healthz") {
      res.writeHead(200, { "Content-Type": "application/json" });
      res.end(JSON.stringify({ ok: true, service: "tehilim-realtime", clients: clients.size }));
      return;
    }
    res.writeHead(426, { "Content-Type": "text/plain" });
    res.end("Upgrade Required");
  });

  const wss = new WebSocketServer({ server: httpServer });

  function send(ws: WebSocket, obj: unknown) {
    if (ws.readyState === WebSocket.OPEN) ws.send(JSON.stringify(obj));
  }

  function subscribe(ws: WebSocket, st: ClientState, chainId: string) {
    if (st.subs.size >= MAX_SUBSCRIPTIONS) {
      send(ws, { type: "error", error: "too many subscriptions" });
      return;
    }
    st.subs.add(chainId);
    let set = topics.get(chainId);
    if (!set) {
      set = new Set();
      topics.set(chainId, set);
    }
    set.add(ws);
    send(ws, { type: "subscribed", chainId });
  }

  function unsubscribe(ws: WebSocket, st: ClientState, chainId: string) {
    st.subs.delete(chainId);
    const set = topics.get(chainId);
    if (set) {
      set.delete(ws);
      if (set.size === 0) topics.delete(chainId);
    }
    send(ws, { type: "unsubscribed", chainId });
  }

  function cleanup(ws: WebSocket, st: ClientState) {
    if (st.authTimer) clearTimeout(st.authTimer);
    for (const chainId of st.subs) {
      const set = topics.get(chainId);
      if (set) {
        set.delete(ws);
        if (set.size === 0) topics.delete(chainId);
      }
    }
    clients.delete(ws);
  }

  wss.on("connection", (ws: WebSocket) => {
    const st: ClientState = { authed: false, uid: null, alive: true, subs: new Set(), authTimer: null };
    clients.set(ws, st);

    // Doit s'authentifier rapidement, sinon fermeture.
    st.authTimer = setTimeout(() => {
      if (!st.authed) {
        send(ws, { type: "error", error: "auth timeout" });
        ws.close();
      }
    }, AUTH_TIMEOUT_MS);

    ws.on("pong", () => { st.alive = true; });

    ws.on("message", (data) => {
      let msg: { type?: string; token?: string; chainId?: string };
      try {
        msg = JSON.parse(data.toString());
      } catch {
        send(ws, { type: "error", error: "invalid json" });
        return;
      }

      if (msg.type === "auth") {
        try {
          const info = verifySupabaseJwt(msg.token ?? "", opts.jwtSecret);
          st.authed = true;
          st.uid = info.uid;
          if (st.authTimer) { clearTimeout(st.authTimer); st.authTimer = null; }
          send(ws, { type: "auth_ok" });
        } catch (e) {
          send(ws, { type: "error", error: "auth failed" });
          log.warn("ws.auth_failed", { error: String(e) });
          ws.close();
        }
        return;
      }

      if (!st.authed) {
        send(ws, { type: "error", error: "not authenticated" });
        return;
      }

      if (msg.type === "subscribe" && msg.chainId) {
        subscribe(ws, st, msg.chainId);
      } else if (msg.type === "unsubscribe" && msg.chainId) {
        unsubscribe(ws, st, msg.chainId);
      } else {
        send(ws, { type: "error", error: "unknown message" });
      }
    });

    ws.on("close", () => cleanup(ws, st));
    ws.on("error", () => cleanup(ws, st));
  });

  // Heartbeat : termine les connexions mortes.
  const heartbeat = setInterval(() => {
    for (const [ws, st] of clients) {
      if (!st.alive) {
        ws.terminate();
        continue;
      }
      st.alive = false;
      try { ws.ping(); } catch { /* noop */ }
    }
  }, HEARTBEAT_MS);

  httpServer.listen(opts.port, "127.0.0.1", () => {
    log.info("ws.listening", { port: opts.port, host: "127.0.0.1" });
  });

  return {
    broadcast: (d: Delta) => {
      const set = topics.get(d.chain_id);
      if (!set || set.size === 0) return;
      const frame = JSON.stringify({
        type: "delta",
        chainId: d.chain_id,
        table: d.table,
        op: d.op,
        row: d.row ?? null,
        old: d.old ?? null,
      });
      for (const ws of set) {
        if (ws.readyState === WebSocket.OPEN) ws.send(frame);
      }
    },
    close: async () => {
      clearInterval(heartbeat);
      for (const ws of clients.keys()) ws.terminate();
      await new Promise<void>((resolve) => wss.close(() => resolve()));
      await new Promise<void>((resolve) => httpServer.close(() => resolve()));
    },
  };
}
