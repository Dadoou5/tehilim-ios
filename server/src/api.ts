// Process « api » : serveur HTTP REST.
//   GET  /healthz           → sonde de vie (publique)
//   POST /internal/notify   → reçoit le payload des triggers Postgres (pg_net),
//                             protégé par x-notify-secret, envoie APNs/FCM.
// Exposé publiquement sous https://tehilimapp.com/api/ (proxy Nginx → :3000).

import express, { type Request, type Response } from "express";
import { env } from "./config/env.js";
import { log } from "./log.js";
import { sendPush, type NotifyPayload } from "./services/push/index.js";

const app = express();
app.disable("x-powered-by");
app.use(express.json({ limit: "256kb" }));

app.get("/healthz", (_req: Request, res: Response) => {
  res.json({ ok: true, service: "tehilim-api", time: new Date().toISOString() });
});

app.post("/internal/notify", async (req: Request, res: Response) => {
  // Secret partagé obligatoire (comme l'Edge Function : inerte/401 sinon).
  const secret = env.notifySharedSecret;
  if (!secret || req.header("x-notify-secret") !== secret) {
    return res.status(401).send("unauthorized");
  }

  const body = req.body as Partial<NotifyPayload> | undefined;
  if (!body || typeof body.event !== "string" || !Array.isArray(body.tokens)) {
    return res.status(400).json({ error: "bad request" });
  }

  try {
    const result = await sendPush({
      event: body.event,
      value: body.value ?? null,
      chainName: body.chainName ?? "",
      chainId: body.chainId ?? null,
      tokens: body.tokens,
      delayMs: body.delayMs,
    });
    res.json(result);
  } catch (e) {
    log.error("api.notify_error", { error: String(e) });
    res.status(500).json({ error: "internal error" });
  }
});

app.use((_req: Request, res: Response) => res.status(404).json({ error: "not found" }));

const server = app.listen(env.port, "127.0.0.1", () => {
  log.info("api.listening", { port: env.port, host: "127.0.0.1" });
});

function shutdown(sig: string) {
  log.info("api.shutdown", { signal: sig });
  server.close(() => process.exit(0));
  setTimeout(() => process.exit(0), 5_000).unref();
}
process.on("SIGTERM", () => shutdown("SIGTERM"));
process.on("SIGINT", () => shutdown("SIGINT"));
