-- ============================================================================
-- Tehilim — Chaîne v2 (Sprint 1) — backend
--   • notif « 100 % complétée » (event-driven, trigger d'attribution)
--   • notif « rappel à 80 % de la durée de sélection » (time-driven, pg_cron)
--   • retrait d'un participant par le créateur (RPC)
-- (Édition de chaîne + quitter = couverts par la RLS existante, pas de SQL.)
-- ============================================================================

alter table public.chains add column if not exists notified_complete            boolean not null default false;
alter table public.chains add column if not exists notified_selection_reminder  boolean not null default false;

-- Trigger d'attribution : seuils 70/80/90 % + 100 % complétée.
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
  if v_count >= 150 then
    update public.chains set notified_complete = true where id = NEW.chain_id and notified_complete = false;
    if found then perform private.notify_chain(NEW.chain_id, 'complete', null); end if;
  end if;
  return NEW;
end;
$$;

-- (Créateur) retire un participant : supprime ses attributions + son inscription.
create or replace function public.remove_participant(p_chain_id uuid, p_uid uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if not exists (select 1 from public.chains where id = p_chain_id and creator_uid = auth.uid()) then
    raise exception 'only the chain creator can remove participants';
  end if;
  delete from public.chain_assignments where chain_id = p_chain_id and uid = p_uid;
  delete from public.chain_participants  where chain_id = p_chain_id and uid = p_uid;
end;
$$;
revoke all on function public.remove_participant(uuid, uuid) from public;
grant execute on function public.remove_participant(uuid, uuid) to authenticated;

-- Rappel « 80 % de la durée de sélection écoulée » : nudge + nb de Tehilim restants.
create or replace function public.process_chain_reminders()
returns void
language plpgsql
security definer
set search_path = public, private
as $$
declare r record; v_remaining int;
begin
  for r in
    select id from public.chains
    where distributed = false
      and notified_selection_reminder = false
      and now() >= created_at + (selection_deadline - created_at) * 0.8
      and now() < selection_deadline
  loop
    update public.chains set notified_selection_reminder = true where id = r.id;
    select 150 - count(*) into v_remaining from public.chain_assignments where chain_id = r.id;
    if v_remaining > 0 then
      perform private.notify_chain(r.id, 'selection_reminder', v_remaining);
    end if;
  end loop;
end;
$$;
revoke all on function public.process_chain_reminders() from public;

-- pg_cron : exécute le rappel chaque heure (granularité suffisante pour le seuil 80 %).
create extension if not exists pg_cron;
select cron.schedule('chain_selection_reminders', '7 * * * *', 'select public.process_chain_reminders();');
