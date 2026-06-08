-- Limite optionnelle de participants par chaîne (option créateur).
-- null = illimité. Borne le fan-out Realtime → capacité plan gratuit.
alter table public.chains add column if not exists participant_limit int;

-- create_chain : ajout de p_participant_limit (on supprime l'ancienne signature
-- à 7 args pour éviter l'ambiguïté d'overload avec le paramètre par défaut).
drop function if exists public.create_chain(text, text, text, text, timestamptz, timestamptz, timestamptz);

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
begin
  insert into public.chains (name, intention_type, intention_detail, creator_uid,
                             creator_name, selection_deadline, reading_deadline,
                             distributed, expires_at, participant_limit)
  values (p_name, p_intention_type, p_intention_detail, auth.uid(),
          p_creator_name, p_selection_deadline, p_reading_deadline, false,
          p_expires_at, p_participant_limit)
  returning id into v_id;

  insert into public.chain_participants (chain_id, uid, name, is_creator)
  values (v_id, auth.uid(), p_creator_name, true);

  return v_id;
end;
$$;

-- Enforcement serveur : refuse un nouveau participant si la limite est atteinte
-- (laisse passer un participant déjà présent qui met à jour son nom).
create or replace function public.trg_enforce_participant_limit()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare v_limit int; v_count int;
begin
  select participant_limit into v_limit from public.chains where id = NEW.chain_id;
  if v_limit is not null
     and not exists (select 1 from public.chain_participants
                     where chain_id = NEW.chain_id and uid = NEW.uid) then
    select count(*) into v_count from public.chain_participants where chain_id = NEW.chain_id;
    if v_count >= v_limit then
      raise exception 'CHAIN_FULL';
    end if;
  end if;
  return NEW;
end;
$$;

drop trigger if exists trg_chain_participant_limit on public.chain_participants;
create trigger trg_chain_participant_limit
  before insert on public.chain_participants
  for each row execute function public.trg_enforce_participant_limit();
