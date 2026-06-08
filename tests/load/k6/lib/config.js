// Configuration centrale du test de charge — tout vient de l'environnement
// (k6 `-e KEY=value` ou variables shell). Aucune valeur secrète en dur.
//
// Le flux testé reproduit EXACTEMENT les appels du client mobile
// (ios/Tehilim/Core/Services/ChainService.swift) contre l'API Supabase :
//   • Auth anonyme :   POST /auth/v1/signup            (signInAnonymously)
//   • Créer chaîne :   POST /rest/v1/rpc/create_chain  (RPC create_chain)
//   • Rejoindre :      POST /rest/v1/chain_participants (upsert)
//   • État courant :   GET  /rest/v1/chain_assignments (fetchBoard)
//   • Sélection :      POST /rest/v1/chain_assignments (insert → 201 / 409)

function envInt(name, def) {
  const v = __ENV[name];
  return v === undefined || v === '' ? def : parseInt(v, 10);
}
function envStr(name, def) {
  const v = __ENV[name];
  return v === undefined || v === '' ? def : v;
}

export const CONFIG = {
  // --- Cible Supabase -------------------------------------------------------
  SUPABASE_URL: envStr('SUPABASE_URL', '').replace(/\/+$/, ''),
  SUPABASE_ANON_KEY: envStr('SUPABASE_ANON_KEY', ''),

  // --- Topologie de charge --------------------------------------------------
  NUM_CHAINS: envInt('NUM_CHAINS', 10),          // chaînes parallèles
  USERS_PER_CHAIN: envInt('USERS_PER_CHAIN', 20), // VUs par chaîne
  // => VUS total = NUM_CHAINS * USERS_PER_CHAIN (200 par défaut)

  // --- Durées (format k6 : '2m', '30s'...) ----------------------------------
  RAMP_UP: envStr('RAMP_UP', '2m'),
  PLATEAU: envStr('PLATEAU', '10m'),
  RAMP_DOWN: envStr('RAMP_DOWN', '2m'),

  // --- Scénario de collision ------------------------------------------------
  COLLISION_VUS: envInt('COLLISION_VUS', 20),       // utilisateurs simultanés
  COLLISION_PSALMS: envInt('COLLISION_PSALMS', 30), // nb de psaumes disputés
  COLLISION_START: envStr('COLLISION_START', '0s'), // décalage de démarrage

  // --- Comportement utilisateur --------------------------------------------
  THINK_MIN_MS: envInt('THINK_MIN_MS', 500),  // délai mini entre actions
  THINK_MAX_MS: envInt('THINK_MAX_MS', 2000), // délai maxi entre actions

  // 'distribution' | 'collision' | 'both'
  SCENARIO: envStr('SCENARIO', 'distribution'),

  // Préfixe de toutes les données de test → cleanup fiable et ciblé.
  TEST_TAG: envStr('TEST_TAG', 'LOADTEST'),

  // Fenêtre de sélection ouverte (secondes) : doit couvrir tout le test.
  SELECTION_WINDOW_S: envInt('SELECTION_WINDOW_S', 60 * 60), // 1 h
};

export const TOTAL_VUS = CONFIG.NUM_CHAINS * CONFIG.USERS_PER_CHAIN;

export function assertConfig() {
  if (!CONFIG.SUPABASE_URL) throw new Error('SUPABASE_URL manquant (-e SUPABASE_URL=...)');
  if (!CONFIG.SUPABASE_ANON_KEY) throw new Error('SUPABASE_ANON_KEY manquant (-e SUPABASE_ANON_KEY=...)');
}

// En-têtes PostgREST/GoTrue. `apikey` = clé anon (publique, protégée par la RLS).
export function anonHeaders() {
  return {
    apikey: CONFIG.SUPABASE_ANON_KEY,
    Authorization: `Bearer ${CONFIG.SUPABASE_ANON_KEY}`,
    'Content-Type': 'application/json',
  };
}

export function authHeaders(jwt) {
  return {
    apikey: CONFIG.SUPABASE_ANON_KEY,
    Authorization: `Bearer ${jwt}`,
    'Content-Type': 'application/json',
  };
}
