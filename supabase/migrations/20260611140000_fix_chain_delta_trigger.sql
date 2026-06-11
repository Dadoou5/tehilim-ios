-- FIX URGENT : trg_chain_delta référençait NEW.distributed (champ de `chains`)
-- dans une expression évaluée aussi pour participants/assignments → 42703
-- « record new has no field distributed » → TOUTES les écritures chaîne
-- échouaient (création/join/sélection) entre l'application de
-- 20260611120000_realtime_delta_triggers et ce fix. Réécriture : NEW/OLD ne
-- sont plus référencés que dans des blocs IF propres à leur table ET leur op.
-- Vérifié : cycle de vie complet (insert chains/participants/assignments,
-- update distribute, delete assignment, delete chains) → OK.

create or replace function public.trg_chain_delta()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_chain uuid;
  v_row   jsonb := null;
  v_old   jsonb := null;
begin
  -- chain_id selon table + op (jamais NEW sur DELETE, jamais OLD sur INSERT)
  if TG_OP = 'DELETE' then
    if TG_TABLE_NAME = 'chains' then v_chain := OLD.id; else v_chain := OLD.chain_id; end if;
  else
    if TG_TABLE_NAME = 'chains' then v_chain := NEW.id; else v_chain := NEW.chain_id; end if;
  end if;

  if TG_TABLE_NAME = 'chain_assignments' then
    if TG_OP in ('INSERT', 'UPDATE') then
      v_row := jsonb_build_object('psalm_id', NEW.psalm_id, 'uid', NEW.uid,
                                  'name', NEW.name, 'by_creator', NEW.by_creator);
    end if;
    if TG_OP in ('UPDATE', 'DELETE') then
      v_old := jsonb_build_object('psalm_id', OLD.psalm_id, 'uid', OLD.uid, 'name', OLD.name);
    end if;

  elsif TG_TABLE_NAME = 'chain_participants' then
    if TG_OP in ('INSERT', 'UPDATE') then
      v_row := jsonb_build_object('uid', NEW.uid, 'name', NEW.name, 'is_creator', NEW.is_creator);
    end if;
    if TG_OP in ('UPDATE', 'DELETE') then
      v_old := jsonb_build_object('uid', OLD.uid, 'name', OLD.name);
    end if;

  else -- chains
    if TG_OP = 'UPDATE' then
      -- Garde anti-bruit (bump assigned_count / flags notified_*) : pas de delta
      -- si aucun champ pertinent pour le client n'a changé.
      if NEW.name is not distinct from OLD.name
         and NEW.distributed is not distinct from OLD.distributed
         and NEW.selection_deadline is not distinct from OLD.selection_deadline
         and NEW.reading_deadline is not distinct from OLD.reading_deadline then
        return null;
      end if;
    end if;
    if TG_OP in ('INSERT', 'UPDATE') then
      v_row := jsonb_build_object('id', NEW.id, 'name', NEW.name, 'distributed', NEW.distributed,
                                  'selection_deadline', NEW.selection_deadline,
                                  'reading_deadline', NEW.reading_deadline);
    end if;
    if TG_OP in ('UPDATE', 'DELETE') then
      v_old := jsonb_build_object('id', OLD.id);
    end if;
  end if;

  perform pg_notify(
    'chain_delta',
    jsonb_build_object('chain_id', v_chain, 'table', TG_TABLE_NAME, 'op', TG_OP,
                       'row', v_row, 'old', v_old)::text
  );
  return null;
end;
$$;
