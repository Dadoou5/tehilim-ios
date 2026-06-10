// Pool Postgres vers Supabase (connexion directe, session mode 5432).
// Utilisé par le scheduler pour appeler les mêmes fonctions SQL que pg_cron.
// La même connexion servira au LISTEN/NOTIFY de la phase 3 (realtime).

import { Pool } from "pg";
import { env } from "../config/env.js";

let pool: Pool | null = null;

export function getPool(): Pool {
  if (pool) return pool;
  if (!env.databaseUrl) {
    throw new Error("DATABASE_URL manquant : le scheduler nécessite la connexion Postgres Supabase.");
  }
  pool = new Pool({
    connectionString: env.databaseUrl,
    // Supabase impose TLS ; le certificat n'est pas dans le store local du VPS.
    ssl: { rejectUnauthorized: false },
    max: 4,
    idleTimeoutMillis: 30_000,
    connectionTimeoutMillis: 10_000,
  });
  return pool;
}
