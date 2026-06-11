// Envoi FCM v1 (Android) via HTTPS (fetch). `chainId` en `data` (deep-link au tap).
// Purge des tokens morts (404 / UNREGISTERED). Retourne true sur livraison (2xx).

import { log } from "../../log.js";
import type { PushMessage } from "./messages.js";
import { pruneToken } from "../supabase.js";

export async function sendFCM(
  token: string,
  msg: PushMessage,
  accessToken: string,
  projectId: string,
  chainId: string | null,
): Promise<boolean> {
  const message: Record<string, unknown> = { token, notification: msg };
  if (chainId) message.data = { chainId };
  let res: Response;
  try {
    res = await fetch(`https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`, {
      method: "POST",
      headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
      body: JSON.stringify({ message }),
    });
  } catch (e) {
    log.error("fcm.request_error", { error: String(e) });
    return false;
  }
  if (res.status < 300) return true;
  const txt = await res.text();
  log.error("fcm.send_failed", { status: res.status, text: txt.slice(0, 200) });
  if (res.status === 404 || txt.includes("UNREGISTERED")) await pruneToken(token);
  return false;
}
