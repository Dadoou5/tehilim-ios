-- ============================================================================
-- Tehilim — feature « Chaîne de Tehilim » — schéma Supabase (Postgres)
-- ----------------------------------------------------------------------------
-- Remplace Firebase Firestore. Modèle RELATIONNEL (et non plus le « board »
-- JSON unique de Firestore) : Postgres ne facture pas la lecture par ligne et
-- offre des contraintes + une RLS par ligne. On y gagne donc en sécurité :
--   • verrou exclusif « 1 lecteur / Tehilim » = PRIMARY KEY (chain_id, psalm_id)
--     → un INSERT en double échoue (23505) ⇒ « déjà pris », atomique, sans
--       transaction applicative.
--   • la propriété (« je ne modifie/libère que MES cases ») est enfin imposée
--     CÔTÉ SERVEUR par la RLS — ce que le board Firestore ne savait pas faire
--     (c'était le « compromis assumé » documenté côté Firebase).
--
-- Idempotent : ré-exécutable sans erreur (IF NOT EXISTS / OR REPLACE / gardes).
-- ============================================================================

-- 1) TABLES -------------------------------------------------------------------

create table if not exists public.chains (
  id                 uuid        primary key default gen_random_uuid(),
  name               text        not null,
  intention_type     text        not null check (intention_type in ('lelouy','refoua','reussite')),
  intention_detail   text        not null default '',
  creator_uid        uuid        not null,
  creator_name       text        not null default '',
  created_at         timestamptz not null default now(),
  selection_deadline timestamptz not null,
  reading_deadline   timestamptz not null,
  distributed        boolean     not null default false,
  -- fin de lecture + marge ; le cron supprime les chaînes au-delà (ex-TTL).
  expires_at         timestamptz not null
);

create table if not exists public.chain_participants (
  chain_id   uuid        not null references public.chains(id) on delete cascade,
  uid        uuid        not null,
  name       text        not null,
  is_creator boolean     not null default false,
  joined_at  timestamptz not null default now(),
  primary key (chain_id, uid)
);

create table if not exists public.chain_assignments (
  chain_id    uuid        not null references public.chains(id) on delete cascade,
  psalm_id    int         not null check (psalm_id between 1 and 150),
  uid         uuid        not null,
  name        text        not null,
  by_creator  boolean     not null default false,
  assigned_at timestamptz not null default now(),
  -- VERROU exclusif : un seul lecteur par (chaîne, Tehilim).
  primary key (chain_id, psalm_id)
);

create index if not exists chain_assignments_chain_idx  on public.chain_assignments(chain_id);
create index if not exists chain_participants_chain_idx on public.chain_participants(chain_id);
create index if not exists chains_expires_idx           on public.chains(expires_at);

-- 2) PRIVILÈGES + RLS ---------------------------------------------------------
-- Les rôles `authenticated` (utilisateurs anonymes inclus) passent par la RLS.
grant usage on schema public to anon, authenticated;
grant select, insert, update, delete
  on public.chains, public.chain_participants, public.chain_assignments
  to authenticated;

alter table public.chains             enable row level security;
alter table public.chain_participants enable row level security;
alter table public.chain_assignments  enable row level security;

-- CHAINS : le secret est l'id (non devinable) → lecture pour tout authentifié.
drop policy if exists chains_read   on public.chains;
drop policy if exists chains_insert on public.chains;
drop policy if exists chains_update on public.chains;
drop policy if exists chains_delete on public.chains;
create policy chains_read   on public.chains for select to authenticated using (true);
create policy chains_insert on public.chains for insert to authenticated with check (creator_uid = auth.uid());
create policy chains_update on public.chains for update to authenticated using (creator_uid = auth.uid()) with check (creator_uid = auth.uid());
create policy chains_delete on public.chains for delete to authenticated using (creator_uid = auth.uid());

-- PARTICIPANTS : chacun n'écrit QUE son propre doc ; lecture pour tout authentifié.
drop policy if exists parts_read   on public.chain_participants;
drop policy if exists parts_insert on public.chain_participants;
drop policy if exists parts_update on public.chain_participants;
drop policy if exists parts_delete on public.chain_participants;
create policy parts_read   on public.chain_participants for select to authenticated using (true);
create policy parts_insert on public.chain_participants for insert to authenticated with check (uid = auth.uid());
create policy parts_update on public.chain_participants for update to authenticated using (uid = auth.uid()) with check (uid = auth.uid());
create policy parts_delete on public.chain_participants for delete to authenticated using (uid = auth.uid());

-- ASSIGNMENTS : verrou par PK + propriété imposée par la RLS.
--   • INSERT  : seulement MA case, pendant la fenêtre de sélection (ou créateur).
--   • DELETE  : ma case, OU le créateur (pour redistribuer).
--   • pas d'UPDATE (sélection = INSERT, désélection = DELETE).
drop policy if exists asg_read   on public.chain_assignments;
drop policy if exists asg_insert on public.chain_assignments;
drop policy if exists asg_delete on public.chain_assignments;
create policy asg_read on public.chain_assignments for select to authenticated using (true);
create policy asg_insert on public.chain_assignments for insert to authenticated
  with check (
    uid = auth.uid()
    and exists (
      select 1 from public.chains c
      where c.id = chain_id
        and ( c.creator_uid = auth.uid()
              or (c.distributed = false and c.selection_deadline > now()) )
    )
  );
create policy asg_delete on public.chain_assignments for delete to authenticated
  using (
    uid = auth.uid()
    or exists (select 1 from public.chains c where c.id = chain_id and c.creator_uid = auth.uid())
  );

-- 3) RPC ----------------------------------------------------------------------

-- Création atomique : la chaîne + le créateur comme premier participant.
-- security invoker ⇒ la RLS s'applique (creator_uid = auth.uid()).
create or replace function public.create_chain(
  p_name               text,
  p_intention_type     text,
  p_intention_detail   text,
  p_creator_name       text,
  p_selection_deadline timestamptz,
  p_reading_deadline   timestamptz,
  p_expires_at         timestamptz
) returns uuid
language plpgsql
security invoker
set search_path = public
as $$
declare
  v_id uuid;
begin
  insert into public.chains (name, intention_type, intention_detail, creator_uid,
                             creator_name, selection_deadline, reading_deadline,
                             distributed, expires_at)
  values (p_name, p_intention_type, p_intention_detail, auth.uid(),
          p_creator_name, p_selection_deadline, p_reading_deadline, false, p_expires_at)
  returning id into v_id;

  insert into public.chain_participants (chain_id, uid, name, is_creator)
  values (v_id, auth.uid(), p_creator_name, true);

  return v_id;
end;
$$;

-- (Créateur uniquement) attribue d'office tous les Tehilim restants à lui-même.
-- Une seule requête : INSERT … SELECT generate_series(1,150) … ON CONFLICT.
create or replace function public.assign_remaining(p_chain_id uuid, p_name text)
returns void
language plpgsql
security invoker
set search_path = public
as $$
begin
  if not exists (select 1 from public.chains
                 where id = p_chain_id and creator_uid = auth.uid()) then
    raise exception 'only the chain creator can assign remaining psalms';
  end if;

  insert into public.chain_assignments (chain_id, psalm_id, uid, name, by_creator)
  select p_chain_id, gs, auth.uid(), p_name, true
  from   generate_series(1, 150) as gs
  on conflict (chain_id, psalm_id) do nothing;
end;
$$;

revoke all on function public.create_chain(text,text,text,text,timestamptz,timestamptz,timestamptz) from public;
revoke all on function public.assign_remaining(uuid,text) from public;
grant execute on function public.create_chain(text,text,text,text,timestamptz,timestamptz,timestamptz) to authenticated;
grant execute on function public.assign_remaining(uuid,text) to authenticated;

-- 4) NETTOYAGE (remplace le TTL Firestore) -----------------------------------
-- Supprime les chaînes expirées (cascade → participants + assignments). Appelée
-- quotidiennement par le cron GitHub (qui sert AUSSI de keep-alive 7 j du free
-- tier). SECURITY DEFINER + garde `expires_at < now()` ⇒ idempotent et sûr même
-- appelée avec la simple anon key : ne touche QUE des données déjà expirées.
create or replace function public.cleanup_expired_chains()
returns integer
language plpgsql
security definer
set search_path = public
as $$
declare
  v_count integer;
begin
  delete from public.chains where expires_at < now();
  get diagnostics v_count = row_count;
  return v_count;
end;
$$;

revoke all on function public.cleanup_expired_chains() from public;
grant execute on function public.cleanup_expired_chains() to anon, authenticated;

-- 5) REALTIME -----------------------------------------------------------------
-- Publie les 3 tables dans la publication realtime (idempotent).
do $$
begin
  if not exists (select 1 from pg_publication_tables
                 where pubname='supabase_realtime' and schemaname='public' and tablename='chains') then
    alter publication supabase_realtime add table public.chains;
  end if;
  if not exists (select 1 from pg_publication_tables
                 where pubname='supabase_realtime' and schemaname='public' and tablename='chain_participants') then
    alter publication supabase_realtime add table public.chain_participants;
  end if;
  if not exists (select 1 from pg_publication_tables
                 where pubname='supabase_realtime' and schemaname='public' and tablename='chain_assignments') then
    alter publication supabase_realtime add table public.chain_assignments;
  end if;
end $$;

-- DELETE realtime : la PK suffit, mais REPLICA IDENTITY FULL fait porter aussi
-- le détail (nom) dans le payload `old` → mise à jour locale plus simple/robuste.
alter table public.chain_assignments  replica identity full;
alter table public.chain_participants replica identity full;
