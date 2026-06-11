// Orchestrateur d'envoi push. Calcule les identifiants UNE fois et les MET EN
// CACHE entre les requêtes — APNs renvoie « 429 TooManyProviderTokenUpdates » si
// on régénère le provider token trop souvent ; on le réutilise donc ~40 min
// (validité 1 h). Idem token OAuth FCM (~50 min). Inerte si secrets absents.

import { env, requireApnsConfigured, requireFcmConfigured } from "../../config/env.js";
import { log } from "../../log.js";
import { messageFor } from "./messages.js";
import { apnsJwt, fcmAccessToken } from "./jwt.js";
import { sendAPNs } from "./apns.js";
import { sendFCM } from "./fcm.js";

export interface DeviceToken {
  token: string;
  platform: "ios" | "android" | string;
  locale: string;
}

export interface NotifyPayload {
  event: string;
  value: number | null;
  chainName: string;
  chainId?: string | null;
  tokens: DeviceToken[];
  delayMs?: number;
}

// --- Caches d'identifiants (clé du fix « TooManyProviderTokenUpdates ») ---
let apnsCache: { token: string; expMs: number } | null = null;
async function cachedApnsJwt(): Promise<string | null> {
  if (!requireApnsConfigured()) {
    log.warn("push.apns_not_configured");
    return null;
  }
  const now = Date.now();
  if (apnsCache && apnsCache.expMs > now) return apnsCache.token;
  try {
    const t = await apnsJwt(env.apns.keyP8!, env.apns.keyId!, env.apns.teamId!);
    apnsCache = { token: t, expMs: now + 40 * 60 * 1000 }; // 40 min (< 1 h de validité)
    return t;
  } catch (e) {
    log.error("push.apns_jwt_failed", { error: String(e) });
    return null;
  }
}

let fcmCache: { token: string; expMs: number } | null = null;
async function cachedFcmToken(): Promise<string | null> {
  if (!requireFcmConfigured()) {
    log.warn("push.fcm_not_configured");
    return null;
  }
  const now = Date.now();
  if (fcmCache && fcmCache.expMs > now) return fcmCache.token;
  try {
    const t = await fcmAccessToken(JSON.parse(env.fcm.serviceAccount!));
    fcmCache = { token: t, expMs: now + 50 * 60 * 1000 }; // 50 min (validité 1 h)
    return t;
  } catch (e) {
    log.error("push.fcm_token_failed", { error: String(e) });
    return null;
  }
}

export async function sendPush(payload: NotifyPayload): Promise<{ sent: number }> {
  const { event, value, chainName, chainId, tokens, delayMs } = payload;
  if (!Array.isArray(tokens) || tokens.length === 0) return { sent: 0 };

  if (typeof delayMs === "number" && delayMs > 0) {
    await new Promise((r) => setTimeout(r, Math.min(delayMs, 10_000)));
  }

  const hasIos = tokens.some((t) => t.platform === "ios");
  const hasAndroid = tokens.some((t) => t.platform === "android");
  const apnsToken = hasIos ? await cachedApnsJwt() : null;
  const fcmTok = hasAndroid ? await cachedFcmToken() : null;
  const fcmProject = env.fcm.projectId || "";

  // `sent` = livraisons RÉELLES (2xx), pas tentatives.
  const results = await Promise.allSettled(
    tokens.map(async (t): Promise<boolean> => {
      const msg = messageFor(event, value ?? null, chainName ?? "", t.locale || "fr");
      if (t.platform === "ios") {
        return apnsToken ? sendAPNs(t.token, msg, apnsToken, chainId ?? null) : false;
      } else if (t.platform === "android") {
        return fcmTok ? sendFCM(t.token, msg, fcmTok, fcmProject, chainId ?? null) : false;
      }
      return false;
    }),
  );

  const sent = results.filter((r) => r.status === "fulfilled" && r.value === true).length;
  const failed = tokens.length - sent;
  log.info("push.batch", { event, value, chainId: chainId ?? null, total: tokens.length, sent, failed });
  return { sent };
}
