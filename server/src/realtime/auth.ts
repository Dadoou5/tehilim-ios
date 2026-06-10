// Vérification du JWT Supabase (HS256) présenté par le client mobile.
// On exige le rôle `authenticated` (anonyme inclus) + un `sub` (uid), pour
// répliquer la RLS `to authenticated` (lecture des chaînes = using(true)).
//
// Note : si le projet migre vers des JWT asymétriques (ES256/JWKS), remplacer
// la vérif HS256 par une vérif via le JWKS Supabase.

import jwt from "jsonwebtoken";

export interface AuthInfo {
  uid: string;
  role: string;
}

export function verifySupabaseJwt(token: string, secret: string): AuthInfo {
  const payload = jwt.verify(token, secret, { algorithms: ["HS256"] }) as jwt.JwtPayload;
  const uid = typeof payload.sub === "string" ? payload.sub : "";
  const role = typeof payload.role === "string" ? payload.role : "";
  if (!uid) throw new Error("JWT sans sub");
  if (role !== "authenticated") throw new Error(`rôle non autorisé: ${role || "(vide)"}`);
  return { uid, role };
}
