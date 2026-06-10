-- ============================================================================
-- PHASE 3 — Triggers ADDITIFS qui publient les deltas chaîne sur le canal
-- Postgres `chain_delta` (consommé par le serveur WS du VPS via LISTEN).
--
-- 100 % NON DESTRUCTIF et sûr en dual-run : sans écouteur, pg_notify est un
-- no-op. N'affecte ni Supabase Realtime ni la logique métier existante.
-- Payload < 8 ko (largement) → limite NOTIFY respectée.
--
-- À appliquer manuellement (Supabase SQL editor / MCP) quand on prépare la
-- phase 3. Rollback en bas.
-- ============================================================================

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
  -- Ignore les UPDATE de chains non pertinents pour le client (ex. bump du
  -- compteur dénormalisé assigned_count) → pas de delta redondant.
  if TG_TABLE_NAME = 'chains' and TG_OP = 'UPDATE'
     and NEW.name               is not distinct from OLD.name
     and NEW.distributed        is not distinct from OLD.distributed
     and NEW.selection_deadline is not distinct from OLD.selection_deadline
     and NEW.reading_deadline   is not distinct from OLD.reading_deadline then
    return null;
  end if;

  -- chain_id selon la table
  if TG_TABLE_NAME = 'chains' then
    v_chain := coalesce(NEW.id, OLD.id);
  else
    v_chain := coalesce(NEW.chain_id, OLD.chain_id);
  end if;

  -- Colonnes minimales nécessaires au client (pas de surface inutile).
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
  return null; -- AFTER trigger
end;
$$;
revoke all on function public.trg_chain_delta() from public;

drop trigger if exists chain_delta_chains on public.chains;
create trigger chain_delta_chains after insert or update or delete on public.chains
  for each row execute function public.trg_chain_delta();

drop trigger if exists chain_delta_participants on public.chain_participants;
create trigger chain_delta_participants after insert or update or delete on public.chain_participants
  for each row execute function public.trg_chain_delta();

drop trigger if exists chain_delta_assignments on public.chain_assignments;
create trigger chain_delta_assignments after insert or update or delete on public.chain_assignments
  for each row execute function public.trg_chain_delta();

-- ----------------------------------------------------------------------------
-- ROLLBACK :
-- drop trigger if exists chain_delta_chains       on public.chains;
-- drop trigger if exists chain_delta_participants on public.chain_participants;
-- drop trigger if exists chain_delta_assignments  on public.chain_assignments;
-- drop function if exists public.trg_chain_delta();
-- ----------------------------------------------------------------------------
