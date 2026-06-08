// Client Supabase minimal pour k6 (HTTP brut) + métriques par endpoint.
// Reproduit fidèlement les requêtes de ChainService.swift.

import http from 'k6/http';
import { Trend, Counter } from 'k6/metrics';
import { CONFIG, anonHeaders, authHeaders } from './config.js';

// --- Métriques métier (p50/p95/p99 via les Trends) ---------------------------
export const M = {
  authSignup: new Trend('ep_auth_signup', true),
  createChain: new Trend('ep_create_chain', true),
  joinUpsert: new Trend('ep_join_upsert', true),
  fetchBoard: new Trend('ep_fetch_board', true),
  selectInsert: new Trend('ep_select_insert', true),

  selectSuccess: new Counter('biz_select_success'), // 201 : Tehilim réservé
  selectConflict: new Counter('biz_select_conflict'), // 409 : déjà pris (PK)
  selectRlsDenied: new Counter('biz_select_rls_denied'), // 401/403/empty by RLS
  selectError: new Counter('biz_select_error'), // 5xx / réseau / autre
  authError: new Counter('biz_auth_error'),
  rateLimited: new Counter('biz_rate_limited_429'),

  collisionSuccess: new Counter('collision_success'),
  collisionConflict: new Counter('collision_conflict'),
  collisionOther: new Counter('collision_other'),
};

function classifyError(status) {
  if (status === 429) M.rateLimited.add(1);
}

// --- Auth anonyme ------------------------------------------------------------
// POST /auth/v1/signup {} → { access_token, user:{id} }.  C'est ce que fait
// supabase-swift `signInAnonymously()`.
export function signInAnonymously(retries = 5) {
  const url = `${CONFIG.SUPABASE_URL}/auth/v1/signup`;
  let backoff = 500;
  for (let attempt = 0; attempt <= retries; attempt++) {
    const res = http.post(url, '{}', {
      headers: anonHeaders(),
      tags: { ep: 'auth_signup' },
    });
    M.authSignup.add(res.timings.duration);
    classifyError(res.status);
    if (res.status === 200) {
      try {
        const body = JSON.parse(res.body);
        if (body.access_token && body.user && body.user.id) {
          return { jwt: body.access_token, uid: String(body.user.id).toLowerCase() };
        }
      } catch (_e) { /* tombe dans le retry */ }
    }
    // 429 / 5xx : backoff exponentiel borné (la limite de signup anonyme est
    // elle-même un goulot potentiel — on la mesure via biz_rate_limited_429).
    if (attempt < retries) {
      const jitter = Math.floor(Math.random() * 250);
      sleepMs(Math.min(backoff + jitter, 8000));
      backoff *= 2;
    }
  }
  M.authError.add(1);
  return null;
}

// --- RPC create_chain --------------------------------------------------------
export function createChain(jwt, { name, intentionType, detail, creatorName,
  selectionDeadlineISO, readingDeadlineISO, expiresAtISO }) {
  const url = `${CONFIG.SUPABASE_URL}/rest/v1/rpc/create_chain`;
  const payload = JSON.stringify({
    p_name: name,
    p_intention_type: intentionType,
    p_intention_detail: detail,
    p_creator_name: creatorName,
    p_selection_deadline: selectionDeadlineISO,
    p_reading_deadline: readingDeadlineISO,
    p_expires_at: expiresAtISO,
  });
  const res = http.post(url, payload, { headers: authHeaders(jwt), tags: { ep: 'create_chain' } });
  M.createChain.add(res.timings.duration);
  classifyError(res.status);
  if (res.status === 200) {
    // RPC scalaire renvoie l'uuid soit "..." soit ["..."] selon Accept.
    let id = res.body.trim().replace(/^"|"$/g, '');
    if (id.startsWith('[')) { try { id = JSON.parse(res.body)[0]; } catch (_e) { /* */ } }
    return String(id).replace(/^"|"$/g, '');
  }
  return null;
}

// --- Rejoindre (upsert participant) -----------------------------------------
export function joinChain(jwt, uid, chainId, name) {
  const url = `${CONFIG.SUPABASE_URL}/rest/v1/chain_participants`;
  const payload = JSON.stringify({ chain_id: chainId, uid, name, is_creator: false });
  const res = http.post(url, payload, {
    headers: Object.assign(authHeaders(jwt), {
      Prefer: 'resolution=merge-duplicates,return=minimal',
    }),
    tags: { ep: 'join_upsert' },
  });
  M.joinUpsert.add(res.timings.duration);
  classifyError(res.status);
  return res.status >= 200 && res.status < 300;
}

// --- État courant (fetchBoard) ----------------------------------------------
// GET /rest/v1/chain_assignments?chain_id=eq.<id>&select=psalm_id,uid
export function fetchBoard(jwt, chainId) {
  const url = `${CONFIG.SUPABASE_URL}/rest/v1/chain_assignments` +
    `?chain_id=eq.${chainId}&select=psalm_id,uid`;
  const res = http.get(url, { headers: authHeaders(jwt), tags: { ep: 'fetch_board' } });
  M.fetchBoard.add(res.timings.duration);
  classifyError(res.status);
  const taken = new Set();
  if (res.status === 200) {
    try { JSON.parse(res.body).forEach((r) => taken.add(r.psalm_id)); } catch (_e) { /* */ }
  }
  return taken;
}

// --- Sélection (insert) ------------------------------------------------------
// POST /rest/v1/chain_assignments → 201 OK | 409 (violation PK = déjà pris).
// Retourne 'ok' | 'conflict' | 'rls' | 'error'.
export function selectPsalm(jwt, uid, chainId, psalmId, name) {
  const url = `${CONFIG.SUPABASE_URL}/rest/v1/chain_assignments`;
  const payload = JSON.stringify({
    chain_id: chainId, psalm_id: psalmId, uid, name, by_creator: false,
  });
  const res = http.post(url, payload, {
    headers: Object.assign(authHeaders(jwt), { Prefer: 'return=minimal' }),
    tags: { ep: 'select_insert' },
  });
  M.selectInsert.add(res.timings.duration);
  classifyError(res.status);

  if (res.status === 201) { M.selectSuccess.add(1); return 'ok'; }
  if (res.status === 409) { M.selectConflict.add(1); return 'conflict'; }
  if (res.status === 401 || res.status === 403) { M.selectRlsDenied.add(1); return 'rls'; }
  // 403 + code PGRST sur RLS peut aussi remonter en 401 ; les 4xx PostgREST de
  // RLS arrivent parfois en 403. Tout le reste = erreur.
  M.selectError.add(1);
  return 'error';
}

// k6 n'expose pas de sleep en ms hors du module 'k6'; on l'importe ici.
import { sleep } from 'k6';
export function sleepMs(ms) { sleep(ms / 1000); }
