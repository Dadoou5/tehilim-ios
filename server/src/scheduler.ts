// Process « scheduler » : remplace les jobs pg_cron Supabase en appelant les
// MÊMES fonctions SQL, aux MÊMES horaires (UTC), via l'API REST Supabase (RPC)
// avec la clé service_role. Pas de connexion Postgres directe requise.
// Les fonctions sont SECURITY DEFINER → comportement identique à `select fn()`
// exécuté par pg_cron.
//
// Jobs miroir (cf. cron.job Supabase) :
//   */5 * * * *  → public.process_chain_reminders()
//   */5 * * * *  → public.process_chain_lifecycle()
//   17 3 * * *   → public.cleanup_expired_chains()
//
// Bascule : tant que pg_cron est actif, ce process tourne EN SHADOW (double
// exécution sans effet de bord grâce aux flags notified_*). Désactiver pg_cron
// (server/sql/02) une fois la parité confirmée dans les logs.

import cron from "node-cron";
import { env } from "./config/env.js";
import { log } from "./log.js";

const TZ = "Etc/UTC"; // parité avec pg_cron (UTC)

type JobFn = "process_chain_reminders" | "process_chain_lifecycle" | "cleanup_expired_chains";

const inFlight = new Set<JobFn>();

async function runFn(fn: JobFn): Promise<void> {
  if (inFlight.has(fn)) {
    log.warn("scheduler.skip_overlap", { fn });
    return;
  }
  if (!env.supabaseUrl || !env.supabaseServiceRoleKey) {
    log.error("scheduler.missing_env", { fn, need: "SUPABASE_URL + SUPABASE_SERVICE_ROLE_KEY" });
    return;
  }
  inFlight.add(fn);
  const started = Date.now();
  try {
    const res = await fetch(`${env.supabaseUrl}/rest/v1/rpc/${fn}`, {
      method: "POST",
      headers: {
        apikey: env.supabaseServiceRoleKey,
        authorization: `Bearer ${env.supabaseServiceRoleKey}`,
        "content-type": "application/json",
      },
      body: "{}",
    });
    if (res.ok) {
      log.info("scheduler.job_ok", { fn, ms: Date.now() - started, status: res.status });
    } else {
      log.error("scheduler.job_failed", {
        fn,
        ms: Date.now() - started,
        status: res.status,
        text: (await res.text()).slice(0, 200),
      });
    }
  } catch (e) {
    log.error("scheduler.job_error", { fn, error: String(e) });
  } finally {
    inFlight.delete(fn);
  }
}

cron.schedule("*/5 * * * *", () => void runFn("process_chain_reminders"), { timezone: TZ });
cron.schedule("*/5 * * * *", () => void runFn("process_chain_lifecycle"), { timezone: TZ });
cron.schedule("17 3 * * *", () => void runFn("cleanup_expired_chains"), { timezone: TZ });

log.info("scheduler.started", {
  tz: TZ,
  mode: "supabase-rpc",
  jobs: ["process_chain_reminders */5", "process_chain_lifecycle */5", "cleanup_expired_chains 17 3"],
});

function shutdown(sig: string) {
  log.info("scheduler.shutdown", { signal: sig });
  setTimeout(() => process.exit(0), 1000).unref();
}
process.on("SIGTERM", () => shutdown("SIGTERM"));
process.on("SIGINT", () => shutdown("SIGINT"));
