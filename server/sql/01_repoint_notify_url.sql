-- ============================================================================
-- PHASE 1 — Bascule de l'envoi push vers le VPS.
-- À appliquer MANUELLEMENT (Supabase SQL editor ou MCP) UNIQUEMENT quand :
--   1. le process tehilim-api tourne sur le VPS,
--   2. NOTIFY_SHARED_SECRET (.env VPS) == private.config.notify_secret,
--   3. un test end-to-end a réussi (voir docs).
-- Les triggers Postgres (pg_net) POSTeront alors vers le VPS au lieu de l'Edge
-- Function. Le payload et l'en-tête x-notify-secret sont inchangés.
-- ============================================================================

update private.config
   set value = 'https://tehilimapp.com/api/internal/notify'
 where key = 'notify_url';

-- Vérification :
-- select key, value from private.config where key in ('notify_url','notify_secret');

-- ----------------------------------------------------------------------------
-- ROLLBACK (retour à l'Edge Function Supabase, immédiat) :
-- update private.config
--    set value = 'https://ymhbmhnuniaxhhsckdaq.supabase.co/functions/v1/notify'
--  where key = 'notify_url';
-- ----------------------------------------------------------------------------
