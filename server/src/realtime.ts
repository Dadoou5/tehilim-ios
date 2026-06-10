// Process « realtime » (phase 3) : serveur WebSocket self-hosté qui remplace
// Supabase Realtime. Lit les deltas via LISTEN/NOTIFY Postgres et les diffuse
// aux clients abonnés par chaîne.
//
// Pré-requis (sinon refus de démarrer) : DATABASE_URL (session 5432) +
// SUPABASE_JWT_SECRET. NE PAS retirer les tables de la publication
// supabase_realtime tant que d'anciennes versions de l'app sont en service
// (dual-run — cf. docs/04-tech/VPS_SELFHOST_MIGRATION.md).

import { env } from "./config/env.js";
import { log } from "./log.js";
import { createRealtimeServer } from "./realtime/wsServer.js";
import { startPgListener } from "./realtime/pgListener.js";

if (!env.databaseUrl) {
  log.error("realtime.missing_env", { missing: "DATABASE_URL" });
  process.exit(1);
}
if (!env.realtime.jwtSecret) {
  log.error("realtime.missing_env", { missing: "SUPABASE_JWT_SECRET" });
  process.exit(1);
}

const server = createRealtimeServer({ port: env.realtime.port, jwtSecret: env.realtime.jwtSecret });
const listener = startPgListener((d) => server.broadcast(d));

log.info("realtime.started", { port: env.realtime.port });

function shutdown(sig: string) {
  log.info("realtime.shutdown", { signal: sig });
  void (async () => {
    await listener.stop();
    await server.close();
    process.exit(0);
  })();
  setTimeout(() => process.exit(0), 5_000).unref();
}
process.on("SIGTERM", () => shutdown("SIGTERM"));
process.on("SIGINT", () => shutdown("SIGINT"));
