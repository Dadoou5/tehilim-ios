-- ============================================================================
-- Tehilim — Notifications push aux PARTICIPANTS d'une chaîne
-- ----------------------------------------------------------------------------
-- Événements notifiés (une seule fois) : complétude 70/80/90 %, distribution,
-- suppression (hors expiration automatique). Seuls les participants reçoivent.
--
-- Mécanique : des triggers (SECURITY DEFINER) détectent l'événement, rassemblent
-- les tokens push des participants (device_tokens ⋈ chain_participants) et
-- appellent l'Edge Function `notify` en HTTP asynchrone via pg_net. L'Edge
-- Function envoie ensuite APNs (iOS) / FCM (Android).
-- ============================================================================

create extension if not exists pg_net;

-- Config privée (NON exposée via l'API REST) : URL + secret partagé de l'Edge
-- Function. Le secret protège l'appel (vérifié dans la fonction).
create schema if not exists private;
revoke all on schema private from anon, authenticated;

create table if not exists private.config (
  key   text primary key,
  value text not null
);

insert into private.config(key, value)
values ('notify_url', 'https://ymhbmhnuniaxhhsckdaq.supabase.co/functions/v1/notify')
on conflict (key) do update set value = excluded.value;

-- Secret aléatoire généré une fois ; à reporter dans les secrets de l'Edge
-- Function (variable NOTIFY_SHARED_SECRET).
insert into private.config(key, value)
values ('notify_secret', encode(extensions.gen_random_bytes(24), 'hex'))
on conflict (key) do nothing;

-- Tokens push par appareil. Un user (uid) peut avoir plusieurs appareils.
create table if not exists public.device_tokens (
  token      text        primary key,
  uid        uuid        not null,
  platform   text        not null check (platform in ('ios','android')),
  locale     text        not null default 'fr',
  updated_at timestamptz not null default now()
);
create index if not exists device_tokens_uid_idx on public.device_tokens(uid);

grant select, insert, update, delete on public.device_tokens to authenticated;
alter table public.device_tokens enable row level security;

drop policy if exists dt_select on public.device_tokens;
drop policy if exists dt_insert on public.device_tokens;
drop policy if exists dt_update on public.device_tokens;
drop policy if exists dt_delete on public.device_tokens;
create policy dt_select on public.device_tokens for select to authenticated using (uid = auth.uid());
create policy dt_insert on public.device_tokens for insert to authenticated with check (uid = auth.uid());
create policy dt_update on public.device_tokens for update to authenticated using (uid = auth.uid()) with check (uid = auth.uid());
create policy dt_delete on public.device_tokens for delete to authenticated using (uid = auth.uid());

-- Drapeaux « déjà notifié » (envoi unique par seuil/événement).
alter table public.chains add column if not exists notified_70          boolean not null default false;
alter table public.chains add column if not exists notified_80          boolean not null default false;
alter table public.chains add column if not exists notified_90          boolean not null default false;
alter table public.chains add column if not exists notified_distributed boolean not null default false;

-- Rassemble les tokens des participants et poste à l'Edge Function (pg_net).
create or replace function private.notify_chain(p_chain_id uuid, p_event text, p_value int)
returns void
language plpgsql
security definer
set search_path = public, private, extensions
as $$
declare
  v_url    text;
  v_secret text;
  v_name   text;
  v_tokens jsonb;
begin
  select value into v_url    from private.config where key = 'notify_url';
  select value into v_secret from private.config where key = 'notify_secret';
  if v_url is null then return; end if;

  select name into v_name from public.chains where id = p_chain_id;

  select coalesce(jsonb_agg(jsonb_build_object(
           'token', t.token, 'platform', t.platform, 'locale', t.locale)), '[]'::jsonb)
    into v_tokens
  from public.device_tokens t
  join public.chain_participants p on p.uid = t.uid
  where p.chain_id = p_chain_id;

  if v_tokens = '[]'::jsonb then return; end if;   -- personne à notifier

  perform net.http_post(
    url     := v_url,
    headers := jsonb_build_object(
                 'Content-Type', 'application/json',
                 'x-notify-secret', coalesce(v_secret, '')),
    body    := jsonb_build_object(
                 'event', p_event, 'value', p_value,
                 'chainName', coalesce(v_name, ''), 'tokens', v_tokens)
  );
end;
$$;
revoke all on function private.notify_chain(uuid, text, int) from public;

-- Seuils 70/80/90 % (150 Tehilim → 105/120/135) à l'insertion d'une attribution.
create or replace function public.trg_assignment_thresholds()
returns trigger
language plpgsql
security definer
set search_path = public, private
as $$
declare v_count int;
begin
  select count(*) into v_count from public.chain_assignments where chain_id = NEW.chain_id;
  if v_count >= 105 then
    update public.chains set notified_70 = true where id = NEW.chain_id and notified_70 = false;
    if found then perform private.notify_chain(NEW.chain_id, 'threshold', 70); end if;
  end if;
  if v_count >= 120 then
    update public.chains set notified_80 = true where id = NEW.chain_id and notified_80 = false;
    if found then perform private.notify_chain(NEW.chain_id, 'threshold', 80); end if;
  end if;
  if v_count >= 135 then
    update public.chains set notified_90 = true where id = NEW.chain_id and notified_90 = false;
    if found then perform private.notify_chain(NEW.chain_id, 'threshold', 90); end if;
  end if;
  return NEW;
end;
$$;
revoke all on function public.trg_assignment_thresholds() from public;
drop trigger if exists assignment_thresholds on public.chain_assignments;
create trigger assignment_thresholds after insert on public.chain_assignments
  for each row execute function public.trg_assignment_thresholds();

-- Distribution (false → true) — BEFORE UPDATE : on marque NEW + on notifie.
create or replace function public.trg_chain_distributed()
returns trigger
language plpgsql
security definer
set search_path = public, private
as $$
begin
  if NEW.distributed = true and coalesce(OLD.distributed, false) = false then
    NEW.notified_distributed := true;
    perform private.notify_chain(NEW.id, 'distributed', null);
  end if;
  return NEW;
end;
$$;
revoke all on function public.trg_chain_distributed() from public;
drop trigger if exists chain_distributed on public.chains;
create trigger chain_distributed before update on public.chains
  for each row execute function public.trg_chain_distributed();

-- Suppression manuelle (BEFORE DELETE : participants encore présents). On
-- ignore l'expiration automatique (expires_at déjà passé → simple nettoyage).
create or replace function public.trg_chain_deleted()
returns trigger
language plpgsql
security definer
set search_path = public, private
as $$
begin
  if OLD.expires_at > now() then
    perform private.notify_chain(OLD.id, 'deleted', null);
  end if;
  return OLD;
end;
$$;
revoke all on function public.trg_chain_deleted() from public;
drop trigger if exists chain_deleted on public.chains;
create trigger chain_deleted before delete on public.chains
  for each row execute function public.trg_chain_deleted();
