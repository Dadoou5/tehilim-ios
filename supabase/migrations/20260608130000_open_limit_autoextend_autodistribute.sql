-- Règles de cycle de vie des chaînes :
--   1) cap 50 participants/chaîne par défaut (pas de choix côté UI) ;
--   2) max 2 chaînes « sélection en cours » par créateur ;
--   3) à l'échéance de sélection, s'il reste des Tehilim : auto-prolongation +3 h
--      (une seule fois, avec notification) ;
--   4) ensuite (ou si le maître a oublié de distribuer) : distribution automatique.

alter table public.chains alter column participant_limit set default 50;
update public.chains set participant_limit = 50 where participant_limit is null;

alter table public.chains add column if not exists auto_extended boolean not null default false;

create or replace function public.create_chain(
  p_name               text,
  p_intention_type     text,
  p_intention_detail   text,
  p_creator_name       text,
  p_selection_deadline timestamptz,
  p_reading_deadline   timestamptz,
  p_expires_at         timestamptz,
  p_participant_limit  int default null
) returns uuid
language plpgsql
security invoker
set search_path = public
as $$
declare
  v_id uuid;
  v_open int;
begin
  -- Max 2 chaînes en cours de sélection par créateur.
  select count(*) into v_open
    from public.chains
   where creator_uid = auth.uid()
     and distributed = false
     and now() < selection_deadline;
  if v_open >= 2 then
    raise exception 'TOO_MANY_OPEN_CHAINS';
  end if;

  insert into public.chains (name, intention_type, intention_detail, creator_uid,
                             creator_name, selection_deadline, reading_deadline,
                             distributed, expires_at, participant_limit)
  values (p_name, p_intention_type, p_intention_detail, auth.uid(),
          p_creator_name, p_selection_deadline, p_reading_deadline, false,
          p_expires_at, 50)   -- cap fixe à 50
  returning id into v_id;

  insert into public.chain_participants (chain_id, uid, name, is_creator)
  values (v_id, auth.uid(), p_creator_name, true);

  return v_id;
end;
$$;

-- Cycle de vie automatique (pg_cron, toutes les 5 min).
create or replace function public.process_chain_lifecycle()
returns void
language plpgsql
security definer
set search_path = public, private
as $$
declare r record; v_remaining int;
begin
  for r in
    select id, auto_extended
    from public.chains
    where distributed = false and now() >= selection_deadline
  loop
    select 150 - count(*) into v_remaining from public.chain_assignments where chain_id = r.id;
    if v_remaining > 0 and r.auto_extended = false then
      -- Échéance atteinte, il reste des Tehilim → +3 h automatique (une seule fois).
      update public.chains
         set selection_deadline = selection_deadline + interval '3 hours',
             auto_extended = true,
             notified_selection_reminder = false,
             notified_final_reminder = false
       where id = r.id;
      perform private.notify_chain(r.id, 'auto_extended', v_remaining);
    else
      -- Déjà prolongée (le +3 h est écoulé) OU tout est pris (maître a oublié)
      -- → distribution automatique. Le trigger chain_distributed notifie.
      update public.chains set distributed = true where id = r.id;
    end if;
  end loop;
end;
$$;
revoke all on function public.process_chain_lifecycle() from public;

select cron.schedule('chain_lifecycle', '*/5 * * * *', 'select public.process_chain_lifecycle();');
