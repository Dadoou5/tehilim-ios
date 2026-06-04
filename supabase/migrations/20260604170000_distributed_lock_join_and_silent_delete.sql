-- ============================================================================
-- Tehilim — Chaîne distribuée : verrou inscriptions + suppression silencieuse
-- ----------------------------------------------------------------------------
-- 1) Plus personne ne peut REJOINDRE une chaîne distribuée (RLS parts_insert).
-- 2) Si le créateur SUPPRIME une chaîne déjà distribuée, on n'envoie PAS la
--    notif « supprimée » (la lecture est lancée, l'info n'a plus d'intérêt et
--    éviterait une notif anxiogène). Auto-expiration : déjà silencieuse.
-- (Le blocage des invitations est purement UI : inviter = partager un lien.)
-- ============================================================================

-- 1) Rejoindre : seulement tant que la chaîne n'est pas distribuée.
--    L'upsert du créateur déjà inscrit passe par la policy UPDATE (inchangée).
drop policy if exists parts_insert on public.chain_participants;
create policy parts_insert on public.chain_participants for insert to authenticated
  with check (
    uid = auth.uid()
    and exists (
      select 1 from public.chains c
      where c.id = chain_id and c.distributed = false
    )
  );

-- 2) Suppression manuelle : notif « supprimée » seulement si NON distribuée.
create or replace function public.trg_chain_deleted()
returns trigger
language plpgsql
security definer
set search_path = public, private
as $$
begin
  -- expires_at > now() ⇒ suppression manuelle (et non nettoyage d'expiration).
  -- distributed = false ⇒ on ne notifie pas la suppression d'une chaîne lancée.
  if OLD.expires_at > now() and not OLD.distributed then
    perform private.notify_chain(OLD.id, 'deleted', null);
  end if;
  return OLD;
end;
$$;
