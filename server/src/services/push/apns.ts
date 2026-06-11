// Envoi APNs (iOS). HTTP/2 via `node:http2` (le fetch de Node ne fait pas HTTP/2).
// Session réutilisée par hôte, RECRÉÉE + 1 retry sur erreur transport (ECONNRESET
// / GOAWAY sur session idle). Bascule auto sandbox⇄production et purge des tokens
// morts. Retourne `true` UNIQUEMENT sur livraison réelle (2xx).

import http2 from "node:http2";
import { env } from "../../config/env.js";
import { log } from "../../log.js";
import type { PushMessage } from "./messages.js";
import { pruneToken } from "../supabase.js";

const { HTTP2_HEADER_METHOD, HTTP2_HEADER_PATH, HTTP2_HEADER_STATUS } = http2.constants;

const sessions = new Map<string, http2.ClientHttp2Session>();

function getSession(host: string): http2.ClientHttp2Session {
  let s = sessions.get(host);
  if (s && !s.closed && !s.destroyed) return s;
  s = http2.connect(`https://${host}`);
  s.on("error", () => sessions.delete(host));
  s.on("close", () => sessions.delete(host));
  sessions.set(host, s);
  return s;
}

interface ApnsResult { status: number; text: string }

function doPost(host: string, token: string, jwt: string, topic: string, body: string): Promise<ApnsResult> {
  return new Promise((resolve, reject) => {
    const session = getSession(host);
    const req = session.request({
      [HTTP2_HEADER_METHOD]: "POST",
      [HTTP2_HEADER_PATH]: `/3/device/${token}`,
      authorization: `bearer ${jwt}`,
      "apns-topic": topic,
      "apns-push-type": "alert",
    });
    let status = 0;
    let data = "";
    req.setTimeout(10_000, () => req.close(http2.constants.NGHTTP2_CANCEL));
    req.on("response", (h) => { status = Number(h[HTTP2_HEADER_STATUS] ?? 0); });
    req.on("data", (c) => (data += c));
    req.on("end", () => resolve({ status, text: data }));
    req.on("error", (e) => reject(e));
    req.end(body);
  });
}

// 1 retry sur erreur transport : on jette la session (potentiellement morte) et
// on rejoue sur une session fraîche.
async function apnsPost(host: string, token: string, jwt: string, topic: string, body: string): Promise<ApnsResult> {
  try {
    return await doPost(host, token, jwt, topic, body);
  } catch (e) {
    sessions.get(host)?.destroy();
    sessions.delete(host);
    try {
      return await doPost(host, token, jwt, topic, body);
    } catch (e2) {
      log.error("apns.request_error", { host, error: String(e2) });
      throw e2;
    }
  }
}

/** Retourne true si la notif a été acceptée par APNs (2xx). */
export async function sendAPNs(
  token: string,
  msg: PushMessage,
  jwt: string,
  chainId: string | null,
): Promise<boolean> {
  const topic = env.apns.bundleId || "";
  const primary = env.apns.host || "api.push.apple.com";
  const fallback = primary.includes("sandbox") ? "api.push.apple.com" : "api.sandbox.push.apple.com";
  const body = JSON.stringify({ aps: { alert: msg, sound: "default" }, ...(chainId ? { chainId } : {}) });

  let dead = false;
  for (const host of [primary, fallback]) {
    try {
      const res = await apnsPost(host, token, jwt, topic, body);
      if (res.status < 300) return true;
      if (res.status === 410 || (res.status === 400 && res.text.includes("BadDeviceToken"))) {
        dead = true;
        continue; // mauvais environnement → on tente l'autre hôte
      }
      log.error("apns.send_failed", { host, status: res.status, text: res.text.slice(0, 200) });
      return false;
    } catch {
      return false; // déjà loggé dans apnsPost
    }
  }
  if (dead) await pruneToken(token); // mort sur les deux environnements → purge
  return false;
}
