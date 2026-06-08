// ============================================================================
// Test de charge — flux « sélection d'un Tehilim dans une chaîne » (Supabase)
// ----------------------------------------------------------------------------
// Deux scénarios, sélectionnables via -e SCENARIO=distribution|collision|both :
//
//   • distribution : NUM_CHAINS chaînes × USERS_PER_CHAIN VUs (200 par défaut).
//       ramp-up → plateau → ramp-down. Chaque VU rejoint SA chaîne, lit l'état,
//       réserve des Tehilim (stripe dédié puis aide sur les restants), avec un
//       think-time réaliste. Objectif : couvrir les 150 Tehilim par chaîne.
//
//   • collision : COLLISION_VUS VUs sur UNE MÊME chaîne tentent de réserver
//       EXACTEMENT les mêmes psaumes au même instant. Vérifie l'atomicité du
//       verrou PRIMARY KEY (chain_id, psalm_id) : au plus 1 succès par psaume.
//
// La vérification finale de cohérence se fait EN BASE (tests/load/sql/), seule
// source de vérité fiable et cross-VU.
// ============================================================================

import { sleep } from 'k6';
import exec from 'k6/execution';
import { CONFIG, TOTAL_VUS, assertConfig } from './lib/config.js';
import {
  signInAnonymously, createChain, joinChain, fetchBoard, selectPsalm, M,
} from './lib/supabase.js';

const RUN_DISTRIB = CONFIG.SCENARIO === 'distribution' || CONFIG.SCENARIO === 'both';
const RUN_COLLISION = CONFIG.SCENARIO === 'collision' || CONFIG.SCENARIO === 'both';

// Pool de JWT pré-mintés (tokens.json via mint_tokens.sh). S'il est fourni, les
// VUs RÉUTILISENT ces identités (round-robin) au lieu de signup à la volée →
// on contourne le rate-limiter d'auth et on stresse réellement la couche DB.
// Vide => repli sur signInAnonymously() par VU (comportement le plus réaliste,
// mais plafonné par le limiteur de signup anonyme).
const TOKEN_POOL = (function loadPool() {
  const f = __ENV.TOKENS_FILE;
  if (!f) return [];
  try {
    const arr = JSON.parse(open(f));
    return Array.isArray(arr) ? arr.filter((t) => t && t.jwt && t.uid) : [];
  } catch (_e) { return []; }
})();

// --- options dynamiques ------------------------------------------------------
const scenarios = {};
if (RUN_DISTRIB) {
  scenarios.distribution = {
    executor: 'ramping-vus',
    exec: 'distribution',
    startVUs: 0,
    stages: [
      { duration: CONFIG.RAMP_UP, target: TOTAL_VUS },
      { duration: CONFIG.PLATEAU, target: TOTAL_VUS },
      { duration: CONFIG.RAMP_DOWN, target: 0 },
    ],
    gracefulRampDown: '30s',
    tags: { scenario: 'distribution' },
  };
}
if (RUN_COLLISION) {
  scenarios.collision = {
    executor: 'per-vu-iterations',
    exec: 'collision',
    vus: CONFIG.COLLISION_VUS,
    iterations: 1,
    startTime: RUN_DISTRIB ? '30s' : CONFIG.COLLISION_START,
    maxDuration: '3m',
    tags: { scenario: 'collision' },
  };
}

export const options = {
  scenarios,
  // setup() crée les chaînes (et, en mode collision, pré-provisionne les tokens) ;
  // sous throttling 429 de l'auth, le backoff peut allonger cette phase.
  setupTimeout: '180s',
  // Seuils = critères pass/fail automatiques du test.
  thresholds: {
    'ep_select_insert': ['p(95)<1500', 'p(99)<3000'],
    'ep_fetch_board': ['p(95)<1200'],
    'http_req_failed': ['rate<0.20'], // tolère les 409 métier comptés à part
    'biz_auth_error': ['count<10'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  discardResponseBodies: false,
};

function iso(d) { return d.toISOString(); }
function thinkTime() {
  const { THINK_MIN_MS: a, THINK_MAX_MS: b } = CONFIG;
  sleep((a + Math.random() * (b - a)) / 1000);
}

// ============================================================================
// SETUP — crée les chaînes une seule fois (créateur anonyme + RPC create_chain).
// ============================================================================
export function setup() {
  assertConfig();
  const now = new Date();
  const selDeadline = iso(new Date(now.getTime() + CONFIG.SELECTION_WINDOW_S * 1000));
  const readDeadline = iso(new Date(now.getTime() + (CONFIG.SELECTION_WINDOW_S + 3600) * 1000));
  const expiresAt = iso(new Date(now.getTime() + (CONFIG.SELECTION_WINDOW_S + 7200) * 1000));
  const stamp = now.toISOString().replace(/[:.]/g, '-');

  let poolCursor = 0;
  function nextCreator() {
    // Préfère une identité du pool (pas de signup) ; sinon signup à la volée.
    if (TOKEN_POOL.length) { const t = TOKEN_POOL[poolCursor % TOKEN_POOL.length]; poolCursor++; return t; }
    return signInAnonymously();
  }

  function makeChain(label) {
    const creator = nextCreator();
    if (!creator) throw new Error('setup: échec auth créateur');
    const id = createChain(creator.jwt, {
      name: `${CONFIG.TEST_TAG}_${label}_${stamp}`,
      intentionType: 'reussite',
      detail: 'load-test',
      creatorName: `${CONFIG.TEST_TAG}_creator`,
      selectionDeadlineISO: selDeadline,
      readingDeadlineISO: readDeadline,
      expiresAtISO: expiresAt,
    });
    if (!id) throw new Error(`setup: échec create_chain (${label})`);
    return id;
  }

  const data = { chains: [], collisionChainId: null, tag: CONFIG.TEST_TAG };

  if (RUN_DISTRIB) {
    for (let i = 0; i < CONFIG.NUM_CHAINS; i++) {
      data.chains.push(makeChain(`chain${String(i).padStart(2, '0')}`));
      sleep(0.2); // lisse la création (évite tout pic de signup au démarrage)
    }
  }
  if (RUN_COLLISION) {
    data.collisionChainId = makeChain('collision');
    // Pré-provisionne les JWT des « colliders » SÉQUENTIELLEMENT ici (hors du
    // moment de collision) → la course se joue sur l'INSERT, pas sur l'auth.
    // (On a observé que 20 signups SIMULTANÉS déclenchent la limite 429 de
    // /auth/v1/signup ; les espacer évite que l'auth masque la vraie collision.)
    data.collisionTokens = [];
    if (TOKEN_POOL.length) {
      // Distribue des identités DISTINCTES du pool aux colliders (vraie course N-voies).
      for (let i = 0; i < CONFIG.COLLISION_VUS; i++) {
        data.collisionTokens.push(TOKEN_POOL[(poolCursor + i) % TOKEN_POOL.length]);
      }
    } else {
      for (let i = 0; i < CONFIG.COLLISION_VUS; i++) {
        const u = signInAnonymously();
        if (u) data.collisionTokens.push(u);
        sleep(0.25);
      }
    }
    console.log(`SETUP collision: ${data.collisionTokens.length}/${CONFIG.COLLISION_VUS} tokens prêts`);
  }

  console.log(`SETUP OK — chains=${data.chains.length} collision=${data.collisionChainId || '-'}`);
  return data;
}

// ============================================================================
// État par VU (chaque VU k6 a sa propre copie du module → persiste entre iters).
// ============================================================================
let identity = null;     // { jwt, uid }
let joinedChain = null;  // id de chaîne rejointe
let myStripe = [];       // psaumes prioritaires de ce VU
let known = new Set();   // psaumes connus comme pris (vue locale)
let iterCount = 0;

function ensureIdentity() {
  if (identity) return identity;
  if (TOKEN_POOL.length) {
    identity = TOKEN_POOL[(exec.vu.idInInstance - 1) % TOKEN_POOL.length];
  } else {
    identity = signInAnonymously();
  }
  return identity;
}

// ============================================================================
// SCÉNARIO 1 — distribution (200 VUs : 10 chaînes × 20 users)
// ============================================================================
export function distribution(data) {
  const vu = exec.vu.idInInstance;                  // 1..TOTAL_VUS
  const chainIdx = Math.floor((vu - 1) / CONFIG.USERS_PER_CHAIN) % data.chains.length;
  const localIdx = (vu - 1) % CONFIG.USERS_PER_CHAIN; // 0..USERS_PER_CHAIN-1
  const chainId = data.chains[chainIdx];

  const id = ensureIdentity();
  if (!id) { sleep(1); return; }

  if (joinedChain !== chainId) {
    joinChain(id.jwt, id.uid, chainId, `${CONFIG.TEST_TAG}_u${vu}`);
    joinedChain = chainId;
    // Stripe dédié : psaumes p où (p-1) % USERS_PER_CHAIN == localIdx.
    // 20 users couvrent ainsi 1..150 sans recouvrement initial.
    myStripe = [];
    for (let p = 1 + localIdx; p <= 150; p += CONFIG.USERS_PER_CHAIN) myStripe.push(p);
    known = new Set();
  }

  // 1) Lire l'état courant (réaliste : on regarde le board avant de choisir).
  if (iterCount % 3 === 0) {
    known = fetchBoard(id.jwt, chainId);
  }
  iterCount++;

  // 2) Choisir un Tehilim : d'abord mon stripe, sinon n'importe quel libre
  //    (phase d'aide → contention sur les derniers psaumes).
  let target = myStripe.find((p) => !known.has(p));
  if (target === undefined) {
    for (let p = 1; p <= 150; p++) { if (!known.has(p)) { target = p; break; } }
  }
  if (target === undefined) {
    // Vue locale = chaîne complète. On rafraîchit pour confirmer puis on souffle.
    known = fetchBoard(id.jwt, chainId);
    if (known.size >= 150) { thinkTime(); return; }
    return;
  }

  // 3) Réserver. 201 = pris par moi ; 409 = un autre a gagné la course (PK).
  const r = selectPsalm(id.jwt, id.uid, chainId, target, `${CONFIG.TEST_TAG}_u${vu}`);
  known.add(target); // dans tous les cas le psaume est désormais pris

  // 4) Think-time réaliste entre deux actions.
  thinkTime();
}

// ============================================================================
// SCÉNARIO 2 — collision (20 users, MÊME chaîne, MÊMES psaumes, même instant)
// ============================================================================
export function collision(data) {
  const chainId = data.collisionChainId;
  const vu = exec.vu.idInInstance;
  // Token pré-provisionné (sinon repli sur un signup à la volée).
  const tokens = data.collisionTokens || [];
  const id = tokens.length ? tokens[(vu - 1) % tokens.length] : ensureIdentity();
  if (!id) { sleep(1); return; }

  joinChain(id.jwt, id.uid, chainId, `${CONFIG.TEST_TAG}_collider${vu}`);

  // Tous les VUs visent la MÊME séquence 1..COLLISION_PSALMS, sans délai, pour
  // maximiser la simultanéité. Attendu : exactement 1 succès par psaume.
  for (let p = 1; p <= CONFIG.COLLISION_PSALMS; p++) {
    const r = selectPsalm(id.jwt, id.uid, chainId, p, `${CONFIG.TEST_TAG}_collider${exec.vu.idInInstance}`);
    if (r === 'ok') M.collisionSuccess.add(1);
    else if (r === 'conflict') M.collisionConflict.add(1);
    else M.collisionOther.add(1);
  }
}

// ============================================================================
// Résumé : écrit un JSON exploitable + le summary texte standard.
// ============================================================================
export function handleSummary(summaryData) {
  const out = {};
  // Chemin relatif au CWD de k6 ; run.sh fournit en plus --summary-export horodaté.
  out['summary.json'] = JSON.stringify(summaryData, null, 2);
  // stdout par défaut de k6
  // eslint-disable-next-line import/no-unresolved
  return Object.assign(out, { stdout: textSummaryFallback(summaryData) });
}

function textSummaryFallback(d) {
  // Petit résumé maison (évite la dépendance jslib externe en environnement offline).
  const m = d.metrics || {};
  const line = (k) => {
    const v = m[k];
    if (!v) return `${k}: -`;
    if (v.values && v.values.count !== undefined && v.values['p(95)'] === undefined) {
      return `${k}: count=${v.values.count}`;
    }
    const val = v.values || {};
    return `${k}: avg=${fmt(val.avg)} p95=${fmt(val['p(95)'])} p99=${fmt(val['p(99)'])} max=${fmt(val.max)}`;
  };
  const keys = [
    'ep_auth_signup', 'ep_create_chain', 'ep_join_upsert', 'ep_fetch_board', 'ep_select_insert',
    'biz_select_success', 'biz_select_conflict', 'biz_select_rls_denied', 'biz_select_error',
    'biz_rate_limited_429', 'biz_auth_error',
    'collision_success', 'collision_conflict', 'collision_other',
    'http_reqs', 'http_req_duration', 'http_req_failed', 'iterations',
  ];
  return '\n=== RÉSUMÉ TEST DE CHARGE ===\n' + keys.map(line).join('\n') + '\n';
}
function fmt(x) { return x === undefined ? '-' : Math.round(x * 100) / 100; }
