// Chargement + validation des variables d'environnement.
// Aucune valeur secrète en dur ; tout vient de .env (voir .env.example).
// Le worker push reste « inerte » (no-op) si les secrets push manquent —
// exactement comme l'Edge Function d'origine.

import fs from "node:fs";

function opt(name: string): string | undefined {
  const v = process.env[name];
  return v && v.trim() !== "" ? v : undefined;
}

// Résout un secret depuis un FICHIER (<NAME>_PATH, recommandé pour .p8/JSON
// multi-lignes) ou, à défaut, depuis la variable inline <NAME>.
function fileOr(name: string): string | undefined {
  const path = opt(`${name}_PATH`);
  if (path) {
    try {
      return fs.readFileSync(path, "utf8");
    } catch (e) {
      throw new Error(`Lecture de ${name}_PATH (${path}) impossible : ${String(e)}`);
    }
  }
  return opt(name);
}

function req(name: string): string {
  const v = opt(name);
  if (!v) throw new Error(`Variable d'environnement requise manquante : ${name}`);
  return v;
}

export const env = {
  // --- Serveur HTTP (process api) ---
  port: Number(opt("PORT") ?? 3000),
  // Secret partagé protégeant POST /internal/notify (en-tête x-notify-secret).
  notifySharedSecret: opt("NOTIFY_SHARED_SECRET"),

  // --- Supabase (purge des tokens morts via REST service-role) ---
  supabaseUrl: opt("SUPABASE_URL"),
  supabaseServiceRoleKey: opt("SUPABASE_SERVICE_ROLE_KEY"),

  // --- APNs (iOS) ---
  apns: {
    keyP8: fileOr("APNS_KEY_P8"), // APNS_KEY_P8_PATH (fichier .p8) ou inline
    keyId: opt("APNS_KEY_ID"),
    teamId: opt("APNS_TEAM_ID"),
    bundleId: opt("APNS_BUNDLE_ID"),
    host: opt("APNS_HOST") ?? "api.push.apple.com",
  },

  // --- FCM (Android) ---
  fcm: {
    serviceAccount: fileOr("FCM_SERVICE_ACCOUNT"), // FCM_SERVICE_ACCOUNT_PATH (.json) ou inline
    projectId: opt("FCM_PROJECT_ID"),
  },

  // --- Scheduler (process scheduler) : connexion Postgres directe Supabase ---
  // Session mode (port 5432) requis pour LISTEN/NOTIFY (phase 3).
  databaseUrl: opt("DATABASE_URL"),

  // --- Realtime (process realtime, phase 3) ---
  realtime: {
    port: Number(opt("REALTIME_PORT") ?? 3001),
    // Secret JWT Supabase (HS256) pour vérifier les tokens des clients mobiles.
    // Supabase → Project Settings → API → JWT Settings → JWT Secret.
    jwtSecret: opt("SUPABASE_JWT_SECRET"),
  },
} as const;

export function requireApnsConfigured() {
  return Boolean(env.apns.keyP8 && env.apns.keyId && env.apns.teamId && env.apns.bundleId);
}

export function requireFcmConfigured() {
  return Boolean(env.fcm.serviceAccount && env.fcm.projectId);
}

export { req };
