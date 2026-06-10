// Envoi FCM v1 (Android) via HTTPS classique (HTTP/1.1 suffit → fetch).
// Porté depuis l'Edge Function. `chainId` passé en `data` (valeurs string) →
// extras de l'Activity au tap. Purge des tokens morts (404 / UNREGISTERED).

import { log } from "../../log.js";
import type { PushMessage } from "./messages.js";
import { pruneToken } from "../supabase.js";

export async function sendFCM(
  token: string,
  msg: PushMessage,
  accessToken: string,
  projectId: string,
  chainId: string | null,
): Promise<void> {
  const message: Record<string, unknown> = { token, notification: msg };
  if (chainId) message.data = { chainId };
  const res = await fetch(`https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`, {
    method: "POST",
    headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
    body: JSON.stringify({ message }),
  });
  if (res.status < 300) return;
  const txt = await res.text();
  log.error("fcm.send_failed", { status: res.status, text: txt.slice(0, 200) });
  if (res.status === 404 || txt.includes("UNREGISTERED")) await pruneToken(token);
}
