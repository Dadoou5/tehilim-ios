-- ============================================================================
-- Tehilim — Chaîne : interdire le retrait d'un participant après distribution
-- ----------------------------------------------------------------------------
-- Règle métier : une fois la chaîne distribuée (lecture figée), le maître ne
-- peut plus retirer de participant. L'UI masque déjà le bouton ; on l'impose
-- aussi côté serveur (défense en profondeur).
-- ============================================================================

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
  if exists (select 1 from public.chains where id = p_chain_id and distributed) then
    raise exception 'cannot remove participants once the chain is distributed';
  end if;
  delete from public.chain_assignments where chain_id = p_chain_id and uid = p_uid;
  delete from public.chain_participants  where chain_id = p_chain_id and uid = p_uid;
end;
$$;
revoke all on function public.remove_participant(uuid, uuid) from public;
grant execute on function public.remove_participant(uuid, uuid) to authenticated;
