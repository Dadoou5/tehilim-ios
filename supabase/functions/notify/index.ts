import "jsr:@supabase/functions-js/edge-runtime.d.ts";

// ============================================================================
// Edge Function « notify » — envoie les notifications push aux participants.
// Appelée par les triggers Postgres (pg_net) avec un secret partagé.
// Corps reçu : { event, value, chainName, chainId?, tokens:[{token,platform,locale}], delayMs? }
//   event ∈ 'threshold' (value=70|80|90) | 'complete' | 'distribute_prompt'
//         | 'distributed' | 'selection_reminder' | 'final_reminder'
//         | 'selection_extended' | 'deleted'
//   chainId : relayé en clé custom APNs / data FCM → tap ouvre l'écran de la chaîne
//   delayMs : attente avant envoi (ex. invitation à distribuer 3 s après le 100 %)
// Secrets (Edge Function → Settings → Secrets) :
//   NOTIFY_SHARED_SECRET, APNS_KEY_P8, APNS_KEY_ID, APNS_TEAM_ID,
//   APNS_BUNDLE_ID, APNS_HOST(=api.push.apple.com), FCM_SERVICE_ACCOUNT, FCM_PROJECT_ID
// La fonction est inerte (401/no-op) tant que les secrets ne sont pas définis.
// ============================================================================

const enc = (s: string) => new TextEncoder().encode(s);

function b64urlFromBytes(bytes: Uint8Array): string {
  let bin = "";
  for (const b of bytes) bin += String.fromCharCode(b);
  return btoa(bin).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}
const b64urlFromStr = (s: string) => b64urlFromBytes(enc(s));

function pemToDer(pem: string): Uint8Array {
  const body = pem
    .replace(/-----BEGIN [^-]+-----/g, "")
    .replace(/-----END [^-]+-----/g, "")
    .replace(/\s+/g, "");
  return Uint8Array.from(atob(body), (c) => c.charCodeAt(0));
}

async function importPkcs8(pem: string, algo: EcKeyImportParams | RsaHashedImportParams) {
  return await crypto.subtle.importKey("pkcs8", pemToDer(pem), algo, false, ["sign"]);
}

// JWT ES256 pour APNs (provider token).
async function apnsJwt(p8: string, keyId: string, teamId: string): Promise<string> {
  const key = await importPkcs8(p8, { name: "ECDSA", namedCurve: "P-256" });
  const header = { alg: "ES256", kid: keyId };
  const claims = { iss: teamId, iat: Math.floor(Date.now() / 1000) };
  const input = `${b64urlFromStr(JSON.stringify(header))}.${b64urlFromStr(JSON.stringify(claims))}`;
  const sig = await crypto.subtle.sign({ name: "ECDSA", hash: { name: "SHA-256" } }, key, enc(input));
  return `${input}.${b64urlFromBytes(new Uint8Array(sig))}`;
}

// OAuth2 access token FCM v1 via service account (JWT RS256).
async function fcmAccessToken(sa: { client_email: string; private_key: string }): Promise<string> {
  const pem = sa.private_key.replace(/\\n/g, "\n");
  const key = await importPkcs8(pem, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" });
  const now = Math.floor(Date.now() / 1000);
  const header = { alg: "RS256", typ: "JWT" };
  const claims = {
    iss: sa.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  };
  const input = `${b64urlFromStr(JSON.stringify(header))}.${b64urlFromStr(JSON.stringify(claims))}`;
  const sig = await crypto.subtle.sign({ name: "RSASSA-PKCS1-v1_5" }, key, enc(input));
  const assertion = `${input}.${b64urlFromBytes(new Uint8Array(sig))}`;
  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${assertion}`,
  });
  const json = await res.json();
  if (!json.access_token) throw new Error("FCM token exchange failed: " + JSON.stringify(json));
  return json.access_token as string;
}

function messageFor(event: string, value: number | null, chainName: string, locale: string) {
  const en = (locale || "fr").toLowerCase().startsWith("en");
  if (event === "threshold") {
    return {
      title: en ? "Tehilim chain" : "Chaîne de Tehilim",
      body: en ? `“${chainName}” is ${value}% complete` : `« ${chainName} » est complétée à ${value} %`,
    };
  }
  if (event === "distributed") {
    return {
      title: en ? "Chain distributed 🙏" : "Chaîne distribuée 🙏",
      body: en ? `“${chainName}” has been distributed — happy reading` : `« ${chainName} » a été distribuée — bonne lecture`,
    };
  }
  if (event === "complete") {
    return {
      title: en ? "Chain complete 🎉" : "Chaîne complétée 🎉",
      body: en ? `“${chainName}” — all 150 Tehilim are assigned!` : `« ${chainName} » — les 150 Tehilim sont attribués !`,
    };
  }
  if (event === "distribute_prompt") {
    return {
      title: en ? "Your turn to distribute 🙏" : "À toi de distribuer 🙏",
      body: en ? `“${chainName}” is complete — distribute it to start the reading` : `« ${chainName} » est complète — distribue-la pour lancer la lecture`,
    };
  }
  if (event === "selection_reminder") {
    return {
      title: en ? "Selection closing soon" : "Sélection bientôt close",
      body: en ? `“${chainName}”: ${value} Tehilim left to pick` : `« ${chainName} » : il reste ${value} Tehilim à prendre`,
    };
  }
  if (event === "final_reminder") {
    return {
      title: en ? "Last chance ⏳" : "Dernière chance ⏳",
      body: en
        ? `“${chainName}” closes very soon — ${value} Tehilim still free`
        : `« ${chainName} » ferme très bientôt — ${value} Tehilim encore libres`,
    };
  }
  if (event === "selection_extended") {
    return {
      title: en ? "Selection extended ⏰" : "Sélection prolongée ⏰",
      body: en
        ? `“${chainName}”: more time to pick your Tehilim`
        : `« ${chainName} » : plus de temps pour choisir vos Tehilim`,
    };
  }
  return {
    title: en ? "Chain deleted" : "Chaîne supprimée",
    body: en ? `“${chainName}” was deleted by its creator` : `« ${chainName} » a été supprimée par son créateur`,
  };
}

// Supprime un token mort de la base (clé service role auto-injectée par Supabase).
async function pruneToken(token: string) {
  const url = Deno.env.get("SUPABASE_URL");
  const key = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!url || !key) return;
  try {
    await fetch(`${url}/rest/v1/device_tokens?token=eq.${encodeURIComponent(token)}`, {
      method: "DELETE",
      headers: { apikey: key, authorization: `Bearer ${key}` },
    });
  } catch (e) { console.error("prune failed", String(e)); }
}

async function sendAPNs(token: string, msg: { title: string; body: string }, jwt: string, chainId: string | null) {
  const topic = Deno.env.get("APNS_BUNDLE_ID") || "";
  // Bascule auto sandbox⇄production (token mauvais env). Si le token est mort sur
  // les DEUX (BadDeviceToken / 410 Unregistered) → on le purge de la base.
  const primary = Deno.env.get("APNS_HOST") || "api.push.apple.com";
  const fallback = primary.includes("sandbox") ? "api.push.apple.com" : "api.sandbox.push.apple.com";
  // `chainId` en clé custom (sœur de `aps`) → lu au tap pour ouvrir la chaîne.
  const body = JSON.stringify({ aps: { alert: msg, sound: "default" }, ...(chainId ? { chainId } : {}) });
  let dead = false;
  for (const host of [primary, fallback]) {
    const res = await fetch(`https://${host}/3/device/${token}`, {
      method: "POST",
      headers: { authorization: `bearer ${jwt}`, "apns-topic": topic, "apns-push-type": "alert" },
      body,
    });
    if (res.status < 300) return;
    const txt = await res.text();
    if (res.status === 410 || (res.status === 400 && txt.includes("BadDeviceToken"))) { dead = true; continue; }
    console.error("APNs", host, res.status, txt);
    return;
  }
  if (dead) await pruneToken(token);
}

async function sendFCM(token: string, msg: { title: string; body: string }, accessToken: string, projectId: string, chainId: string | null) {
  // `chainId` en `data` (valeurs FCM = strings) → livré dans les extras de
  // l'Activity au tap (arrière-plan) / lu par onMessageReceived (premier plan).
  const message: Record<string, unknown> = { token, notification: msg };
  if (chainId) message.data = { chainId };
  const res = await fetch(`https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`, {
    method: "POST",
    headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
    body: JSON.stringify({ message }),
  });
  if (res.status < 300) return;
  const txt = await res.text();
  console.error("FCM", res.status, txt);
  if (res.status === 404 || txt.includes("UNREGISTERED")) await pruneToken(token);  // token mort → purge
}

Deno.serve(async (req: Request) => {
  const secret = Deno.env.get("NOTIFY_SHARED_SECRET");
  if (!secret || req.headers.get("x-notify-secret") !== secret) {
    return new Response("unauthorized", { status: 401 });
  }

  let payload: { event: string; value: number | null; chainName: string; chainId?: string | null; tokens: Array<{ token: string; platform: string; locale: string }>; delayMs?: number };
  try {
    payload = await req.json();
  } catch {
    return new Response("bad request", { status: 400 });
  }
  const { event, value, chainName, chainId, tokens, delayMs } = payload;
  if (!Array.isArray(tokens) || tokens.length === 0) {
    return new Response(JSON.stringify({ sent: 0 }), { headers: { "Content-Type": "application/json" } });
  }

  // Délai d'envoi optionnel (ex. invitation à distribuer postée 3 s après le 100 %).
  // Borné à 10 s pour rester sous le timeout de l'Edge Function.
  if (typeof delayMs === "number" && delayMs > 0) {
    await new Promise((r) => setTimeout(r, Math.min(delayMs, 10000)));
  }

  // Les identifiants (JWT APNs, token OAuth FCM) sont calculés UNE fois puis
  // réutilisés. Les envois sont ensuite parallélisés (Promise.allSettled) pour
  // ne pas dépasser le timeout sur les chaînes à nombreux participants.
  const hasIos = tokens.some((t) => t.platform === "ios");
  const hasAndroid = tokens.some((t) => t.platform === "android");
  let apnsToken: string | null = null;
  let fcmToken: string | null = null;
  const fcmProject = Deno.env.get("FCM_PROJECT_ID") || "";

  if (hasIos) {
    const p8 = Deno.env.get("APNS_KEY_P8");
    const kid = Deno.env.get("APNS_KEY_ID");
    const team = Deno.env.get("APNS_TEAM_ID");
    if (p8 && kid && team) {
      try { apnsToken = await apnsJwt(p8, kid, team); }
      catch (e) { console.error("apns jwt failed", String(e)); }
    } else { console.warn("APNs not configured"); }
  }
  if (hasAndroid) {
    const saRaw = Deno.env.get("FCM_SERVICE_ACCOUNT");
    if (saRaw && fcmProject) {
      try { fcmToken = await fcmAccessToken(JSON.parse(saRaw)); }
      catch (e) { console.error("fcm token failed", String(e)); }
    } else { console.warn("FCM not configured"); }
  }

  const results = await Promise.allSettled(tokens.map(async (t) => {
    const msg = messageFor(event, value ?? null, chainName ?? "", t.locale || "fr");
    if (t.platform === "ios") {
      if (!apnsToken) return false;
      await sendAPNs(t.token, msg, apnsToken, chainId ?? null);
      return true;
    } else if (t.platform === "android") {
      if (!fcmToken) return false;
      await sendFCM(t.token, msg, fcmToken, fcmProject, chainId ?? null);
      return true;
    }
    return false;
  }));
  const sent = results.filter((r) => r.status === "fulfilled" && r.value === true).length;

  return new Response(JSON.stringify({ sent }), { headers: { "Content-Type": "application/json" } });
});
