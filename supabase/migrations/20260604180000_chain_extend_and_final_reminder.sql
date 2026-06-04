-- ============================================================================
-- Tehilim — Chaîne : gestion de l'incomplétude à l'échéance
--   • 2ᵉ rappel « dernière chance » (~95 % de la durée de sélection écoulée,
--     s'il reste des Tehilim libres) — distinct du rappel à 80 %.
--   • RPC extend_chain_selection : le créateur repousse selection_deadline,
--     réarme les deux rappels et re-notifie les participants (« prolongée »).
-- ============================================================================

alter table public.chains
  add column if not exists notified_final_reminder boolean not null default false;

-- Rappels temporels (pg_cron horaire) :
--   • 80 % du temps écoulé  → 'selection_reminder' (nudge + nb restants)
--   • 95 % du temps écoulé  → 'final_reminder'     (dernière chance + nb restants)
-- Chacun ne part qu'une fois (flag), uniquement s'il reste des Tehilim libres.
create or replace function public.process_chain_reminders()
returns void
language plpgsql
security definer
set search_path = public, private
as $$
declare r record; v_remaining int;
begin
  -- Rappel à 80 % de la durée.
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

  -- Rappel « dernière chance » à 95 % de la durée (s'il reste des libres).
  for r in
    select id from public.chains
    where distributed = false
      and notified_final_reminder = false
      and now() >= created_at + (selection_deadline - created_at) * 0.95
      and now() < selection_deadline
  loop
    update public.chains set notified_final_reminder = true where id = r.id;
    select 150 - count(*) into v_remaining from public.chain_assignments where chain_id = r.id;
    if v_remaining > 0 then
      perform private.notify_chain(r.id, 'final_reminder', v_remaining);
    end if;
  end loop;
end;
$$;
revoke all on function public.process_chain_reminders() from public;

-- (Créateur) prolonge la sélection : repousse l'échéance, réarme les rappels,
-- et notifie les participants que la chaîne est prolongée.
create or replace function public.extend_chain_selection(p_chain_id uuid, p_new_deadline timestamptz)
returns void
language plpgsql
security definer
set search_path = public, private
as $$
declare v_old timestamptz;
begin
  select selection_deadline into v_old
    from public.chains
   where id = p_chain_id and creator_uid = auth.uid() and distributed = false
   for update;
  if not found then
    raise exception 'only the creator can extend a non-distributed chain';
  end if;
  if p_new_deadline <= now() then
    raise exception 'new deadline must be in the future';
  end if;
  if p_new_deadline <= v_old then
    raise exception 'new deadline must be later than the current one';
  end if;

  update public.chains
     set selection_deadline          = p_new_deadline,
         notified_selection_reminder = false,
         notified_final_reminder     = false
   where id = p_chain_id;

  perform private.notify_chain(p_chain_id, 'selection_extended', null);
end;
$$;
revoke all on function public.extend_chain_selection(uuid, timestamptz) from public;
grant execute on function public.extend_chain_selection(uuid, timestamptz) to authenticated;
