// Orchestrateur d'envoi push : reçoit le même payload que l'Edge Function
// d'origine, calcule les identifiants (JWT APNs / token OAuth FCM) UNE fois,
// puis parallélise les envois. Inerte si les secrets ne sont pas configurés.

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

export async function sendPush(payload: NotifyPayload): Promise<{ sent: number }> {
  const { event, value, chainName, chainId, tokens, delayMs } = payload;
  if (!Array.isArray(tokens) || tokens.length === 0) return { sent: 0 };

  // Délai optionnel (ex. invitation à distribuer 3 s après 100 %), borné à 10 s.
  if (typeof delayMs === "number" && delayMs > 0) {
    await new Promise((r) => setTimeout(r, Math.min(delayMs, 10_000)));
  }

  const hasIos = tokens.some((t) => t.platform === "ios");
  const hasAndroid = tokens.some((t) => t.platform === "android");
  let apnsToken: string | null = null;
  let fcmTok: string | null = null;
  const fcmProject = env.fcm.projectId || "";

  if (hasIos) {
    if (requireApnsConfigured()) {
      try {
        apnsToken = await apnsJwt(env.apns.keyP8!, env.apns.keyId!, env.apns.teamId!);
      } catch (e) {
        log.error("push.apns_jwt_failed", { error: String(e) });
      }
    } else {
      log.warn("push.apns_not_configured");
    }
  }
  if (hasAndroid) {
    if (requireFcmConfigured()) {
      try {
        fcmTok = await fcmAccessToken(JSON.parse(env.fcm.serviceAccount!));
      } catch (e) {
        log.error("push.fcm_token_failed", { error: String(e) });
      }
    } else {
      log.warn("push.fcm_not_configured");
    }
  }

  const results = await Promise.allSettled(
    tokens.map(async (t) => {
      const msg = messageFor(event, value ?? null, chainName ?? "", t.locale || "fr");
      if (t.platform === "ios") {
        if (!apnsToken) return false;
        await sendAPNs(t.token, msg, apnsToken, chainId ?? null);
        return true;
      } else if (t.platform === "android") {
        if (!fcmTok) return false;
        await sendFCM(t.token, msg, fcmTok, fcmProject, chainId ?? null);
        return true;
      }
      return false;
    }),
  );

  const sent = results.filter((r) => r.status === "fulfilled" && r.value === true).length;
  log.info("push.batch", { event, value, chainId: chainId ?? null, total: tokens.length, sent });
  return { sent };
}
