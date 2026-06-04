-- ============================================================================
-- Tehilim — Chaîne : attribution « en masse » (m'attribuer les Tehilim restants)
-- ----------------------------------------------------------------------------
-- Problème : `assign_remaining` insère tous les Tehilim manquants en UNE seule
-- instruction (INSERT … generate_series(1,150)). Le trigger `assignment_thresholds`
-- (AFTER INSERT FOR EACH ROW) voit, dès sa première exécution, count(*) = 150 :
-- il franchit d'un coup 105/120/135/150 et envoie une RAFALE de notifs
-- 70 % + 80 % + 90 % + 100 % aux participants.
--
-- Comportement voulu : quand on saute directement à 100 %, n'envoyer QUE la
-- notif « complétée ». On marque donc les paliers intermédiaires comme déjà
-- notifiés AVANT l'insert → le trigger les saute et ne déclenche que `complete`.
-- ============================================================================

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

  -- Attribution en masse = on saute droit à 100 %. On neutralise les paliers
  -- 70/80/90 AVANT l'insert pour que le trigger ne déclenche QUE la notif
  -- « complétée » (sinon rafale 70 % + 80 % + 90 % + 100 % d'un seul coup).
  update public.chains
     set notified_70 = true, notified_80 = true, notified_90 = true
   where id = p_chain_id;

  insert into public.chain_assignments (chain_id, psalm_id, uid, name, by_creator)
  select p_chain_id, gs, auth.uid(), p_name, true
  from   generate_series(1, 150) as gs
  on conflict (chain_id, psalm_id) do nothing;
end;
$$;

revoke all on function public.assign_remaining(uuid,text) from public;
grant execute on function public.assign_remaining(uuid,text) to authenticated;
