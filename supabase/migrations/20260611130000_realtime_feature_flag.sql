-- ============================================================================
-- PHASE 3 — Feature flag distant : piloter la bascule realtime (Supabase ↔ VPS)
-- PAR COHORTE, sans nouvelle release mobile.
--
-- Le client appelle `rpc/realtime_source` au lancement → 'vps' ou 'supabase',
-- et se connecte à la source correspondante. L'opérateur change la cible en
-- éditant la ligne `app_flags.realtime` (SQL / dashboard). 100 % non destructif.
-- ============================================================================

-- Table de flags lus par le client (lecture publique, AUCUNE écriture via API).
create table if not exists public.app_flags (
  key        text        primary key,
  value      jsonb       not null default '{}'::jsonb,
  updated_at timestamptz not null default now()
);

alter table public.app_flags enable row level security;
grant select on public.app_flags to anon, authenticated;
drop policy if exists app_flags_read on public.app_flags;
create policy app_flags_read on public.app_flags for select to anon, authenticated using (true);
-- (pas de policy insert/update/delete ⇒ modifiable uniquement en SQL/dashboard)

-- Valeur initiale : tout le monde sur Supabase (état actuel).
--   source      : 'supabase' | 'vps' | 'percent'
--   rollout_pct : 0..100 (utilisé si source='percent', cohorte STABLE par uid)
--   allow_uids  : liste d'uids forcés sur 'vps' (cohorte pilote / tests)
insert into public.app_flags(key, value)
values ('realtime', jsonb_build_object('source', 'supabase', 'rollout_pct', 0, 'allow_uids', '[]'::jsonb))
on conflict (key) do nothing;

-- Décision serveur : renvoie la source realtime pour un uid (cohorte stable).
create or replace function public.realtime_source(p_uid uuid default auth.uid())
returns text
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  cfg jsonb;
  src text;
  pct int;
begin
  select value into cfg from public.app_flags where key = 'realtime';
  if cfg is null then return 'supabase'; end if;

  -- Allowlist explicite (toujours prioritaire) → cohorte pilote / appareils de test.
  if p_uid is not null and (cfg->'allow_uids') ? p_uid::text then
    return 'vps';
  end if;

  src := coalesce(cfg->>'source', 'supabase');
  if src = 'vps' then
    return 'vps';
  elsif src = 'percent' then
    pct := coalesce((cfg->>'rollout_pct')::int, 0);
    if p_uid is not null and (abs(hashtextextended(p_uid::text, 0)) % 100) < pct then
      return 'vps';
    end if;
  end if;

  return 'supabase';
end;
$$;
revoke all on function public.realtime_source(uuid) from public;
grant execute on function public.realtime_source(uuid) to anon, authenticated;

-- ----------------------------------------------------------------------------
-- PILOTAGE (exemples — à exécuter au fil du déploiement) :
--
--   -- Cohorte pilote : forcer 2 appareils de test sur le VPS
--   update public.app_flags
--      set value = jsonb_set(value, '{allow_uids}', '["<uid1>","<uid2>"]'::jsonb),
--          updated_at = now()
--    where key = 'realtime';
--
--   -- Déploiement progressif : 10 % → 25 % → 50 % → 100 %
--   update public.app_flags
--      set value = jsonb_build_object('source','percent','rollout_pct',10,'allow_uids','[]'::jsonb),
--          updated_at = now()
--    where key = 'realtime';
--
--   -- Bascule totale
--   update public.app_flags
--      set value = jsonb_set(value, '{source}', '"vps"'), updated_at = now()
--    where key = 'realtime';
--
--   -- ROLLBACK immédiat : tout le monde revient sur Supabase
--   update public.app_flags
--      set value = jsonb_set(value, '{source}', '"supabase"'), updated_at = now()
--    where key = 'realtime';
--
-- Vérifier : select public.realtime_source('<uid>'::uuid);
-- ----------------------------------------------------------------------------

-- ROLLBACK (suppression complète) :
-- drop function if exists public.realtime_source(uuid);
-- drop table if exists public.app_flags;
