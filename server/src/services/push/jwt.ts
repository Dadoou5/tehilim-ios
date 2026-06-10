// Signatures JWT pour APNs (ES256) et FCM (RS256 → échange OAuth2).
// Porté depuis l'Edge Function (Web Crypto). En Node 22, `crypto.subtle` est
// disponible globalement ; on utilise Buffer pour le base64url (plus idiomatique).

const enc = (s: string) => new TextEncoder().encode(s);
const b64url = (bytes: Uint8Array | ArrayBuffer) =>
  Buffer.from(bytes instanceof ArrayBuffer ? new Uint8Array(bytes) : bytes).toString("base64url");
const b64urlStr = (s: string) => Buffer.from(s, "utf8").toString("base64url");

function pemToDer(pem: string): ArrayBuffer {
  const body = pem
    .replace(/-----BEGIN [^-]+-----/g, "")
    .replace(/-----END [^-]+-----/g, "")
    .replace(/\\n/g, "") // \n littéraux (clé .p8 collée inline sur une ligne)
    .replace(/\s+/g, "");
  const buf = Buffer.from(body, "base64");
  // Copie dans un ArrayBuffer autonome (TS 5.7 exige un ArrayBuffer strict,
  // pas un Uint8Array<ArrayBufferLike> issu de Buffer).
  const ab = new ArrayBuffer(buf.length);
  new Uint8Array(ab).set(buf);
  return ab;
}

async function importPkcs8(
  pem: string,
  algo: EcKeyImportParams | RsaHashedImportParams,
): Promise<CryptoKey> {
  return crypto.subtle.importKey("pkcs8", pemToDer(pem), algo, false, ["sign"]);
}

/** JWT ES256 pour APNs (provider token, valable ~1 h, réutilisable). */
export async function apnsJwt(p8: string, keyId: string, teamId: string): Promise<string> {
  const key = await importPkcs8(p8, { name: "ECDSA", namedCurve: "P-256" });
  const header = { alg: "ES256", kid: keyId };
  const claims = { iss: teamId, iat: Math.floor(Date.now() / 1000) };
  const input = `${b64urlStr(JSON.stringify(header))}.${b64urlStr(JSON.stringify(claims))}`;
  const sig = await crypto.subtle.sign({ name: "ECDSA", hash: { name: "SHA-256" } }, key, enc(input));
  return `${input}.${b64url(sig)}`;
}

/** Access token OAuth2 FCM v1 via service account (assertion JWT RS256). */
export async function fcmAccessToken(sa: { client_email: string; private_key: string }): Promise<string> {
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
  const input = `${b64urlStr(JSON.stringify(header))}.${b64urlStr(JSON.stringify(claims))}`;
  const sig = await crypto.subtle.sign({ name: "RSASSA-PKCS1-v1_5" }, key, enc(input));
  const assertion = `${input}.${b64url(sig)}`;
  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${assertion}`,
  });
  const json = (await res.json()) as { access_token?: string };
  if (!json.access_token) throw new Error("FCM token exchange failed: " + JSON.stringify(json));
  return json.access_token;
}
