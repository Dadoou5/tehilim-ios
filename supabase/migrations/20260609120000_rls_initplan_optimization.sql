-- Perf RLS (advisor 0003_auth_rls_initplan) : auth.uid() était réévalué POUR
-- CHAQUE LIGNE dans les policies. On l'évalue une seule fois par requête via
-- (select auth.uid()). Logique de sécurité identique ; ALTER POLICY préserve
-- les rôles et la commande.
alter policy chains_insert on public.chains
  with check (creator_uid = (select auth.uid()));
alter policy chains_update on public.chains
  using (creator_uid = (select auth.uid()))
  with check (creator_uid = (select auth.uid()));
alter policy chains_delete on public.chains
  using (creator_uid = (select auth.uid()));

alter policy parts_insert on public.chain_participants
  with check ((uid = (select auth.uid()))
    and exists (select 1 from public.chains c
                where c.id = chain_participants.chain_id and c.distributed = false));
alter policy parts_update on public.chain_participants
  using (uid = (select auth.uid()))
  with check (uid = (select auth.uid()));
alter policy parts_delete on public.chain_participants
  using (uid = (select auth.uid()));

alter policy asg_insert on public.chain_assignments
  with check ((uid = (select auth.uid()))
    and exists (select 1 from public.chains c
                where c.id = chain_assignments.chain_id
                  and ((c.creator_uid = (select auth.uid()))
                       or (c.distributed = false and c.selection_deadline > now()))));
alter policy asg_delete on public.chain_assignments
  using ((uid = (select auth.uid()))
    or exists (select 1 from public.chains c
               where c.id = chain_assignments.chain_id and c.creator_uid = (select auth.uid())));

alter policy dt_select on public.device_tokens
  using (uid = (select auth.uid()));
alter policy dt_insert on public.device_tokens
  with check (uid = (select auth.uid()));
alter policy dt_update on public.device_tokens
  using (uid = (select auth.uid()))
  with check (uid = (select auth.uid()));
alter policy dt_delete on public.device_tokens
  using (uid = (select auth.uid()));
