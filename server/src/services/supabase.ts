// Purge d'un token push mort de la table device_tokens, via l'API REST Supabase
// avec la clé service-role. Porté depuis pruneToken() de l'Edge Function.

import { env } from "../config/env.js";
import { log } from "../log.js";

export async function pruneToken(token: string): Promise<void> {
  const url = env.supabaseUrl;
  const key = env.supabaseServiceRoleKey;
  if (!url || !key) return;
  try {
    await fetch(`${url}/rest/v1/device_tokens?token=eq.${encodeURIComponent(token)}`, {
      method: "DELETE",
      headers: { apikey: key, authorization: `Bearer ${key}` },
    });
    log.info("push.prune", { token: token.slice(0, 12) + "…" });
  } catch (e) {
    log.error("push.prune_failed", { error: String(e) });
  }
}
