// Écouteur Postgres : tient UNE connexion dédiée en LISTEN sur le canal unique
// `chain_delta` (alimenté par les triggers additifs, cf. sql/03_realtime_delta_triggers.sql).
// Chaque notification = un delta JSON routé ensuite par chain_id vers les abonnés WS.
// Reconnexion automatique avec backoff.

import { Client } from "pg";
import { env } from "../config/env.js";
import { log } from "../log.js";

export interface Delta {
  chain_id: string;
  table: "chains" | "chain_participants" | "chain_assignments" | string;
  op: "INSERT" | "UPDATE" | "DELETE" | string;
  row?: Record<string, unknown> | null;
  old?: Record<string, unknown> | null;
}

export interface PgListener {
  stop: () => Promise<void>;
}

export function startPgListener(onDelta: (d: Delta) => void): PgListener {
  let client: Client | null = null;
  let stopped = false;
  let backoff = 1000;
  let timer: NodeJS.Timeout | null = null;

  function scheduleReconnect() {
    if (stopped || timer) return;
    timer = setTimeout(() => {
      timer = null;
      void connect();
    }, backoff);
    backoff = Math.min(backoff * 2, 30_000);
  }

  async function connect() {
    if (stopped) return;
    const c = new Client({
      connectionString: env.databaseUrl,
      ssl: { rejectUnauthorized: false },
    });
    client = c;
    c.on("error", (e) => {
      log.error("pglistener.error", { error: String(e) });
      try { c.removeAllListeners(); } catch { /* noop */ }
      if (client === c) client = null;
      scheduleReconnect();
    });
    c.on("notification", (msg) => {
      if (msg.channel !== "chain_delta" || !msg.payload) return;
      try {
        onDelta(JSON.parse(msg.payload) as Delta);
      } catch (e) {
        log.error("pglistener.parse_failed", { error: String(e) });
      }
    });
    try {
      await c.connect();
      await c.query("LISTEN chain_delta");
      backoff = 1000;
      log.info("pglistener.listening", { channel: "chain_delta" });
    } catch (e) {
      log.error("pglistener.connect_failed", { error: String(e) });
      if (client === c) client = null;
      scheduleReconnect();
    }
  }

  void connect();

  return {
    stop: async () => {
      stopped = true;
      if (timer) clearTimeout(timer);
      try {
        await client?.end();
      } catch { /* noop */ }
    },
  };
}
