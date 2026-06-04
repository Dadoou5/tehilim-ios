-- ============================================================================
-- Tehilim — Chaîne : réarmement des seuils à la DÉSÉLECTION d'un Tehilim
-- ----------------------------------------------------------------------------
-- Les flags notified_70/80/90/complete sont « one-shot » : une fois la chaîne
-- montée à 100 %, ils restent tous true. Si un participant désélectionne ensuite
-- des Tehilim (DELETE sur chain_assignments — désélection perso ou retrait d'un
-- participant par le créateur), la chaîne redescend sous 150 mais les flags ne
-- bougent pas → aucune notif ne repart quand elle se re-remplit.
--
-- Correctif : trigger AFTER DELETE qui recalcule le compte et REARME chaque
-- palier repassé sous son seuil. L'expression `flag AND v_count >= seuil` ne fait
-- que remettre à false (jamais notifier) → le re-franchissement, lui, est géré
-- par le trigger d'INSERT existant qui renotifiera selon les règles.
-- ============================================================================

create or replace function public.trg_assignment_rearm_thresholds()
returns trigger
language plpgsql
security definer
set search_path = public, private
as $$
declare v_count int;
begin
  select count(*) into v_count from public.chain_assignments where chain_id = OLD.chain_id;
  -- Réarme uniquement les paliers repassés SOUS leur seuil (jamais de notif ici).
  -- NB : si la chaîne est en cours de suppression (cascade), la ligne chains a
  -- déjà disparu → l'UPDATE touche 0 ligne, sans effet.
  update public.chains set
    notified_70       = (notified_70       and v_count >= 105),
    notified_80       = (notified_80       and v_count >= 120),
    notified_90       = (notified_90       and v_count >= 135),
    notified_complete = (notified_complete and v_count >= 150)
  where id = OLD.chain_id
    and (notified_70 or notified_80 or notified_90 or notified_complete);
  return OLD;
end;
$$;
revoke all on function public.trg_assignment_rearm_thresholds() from public;
drop trigger if exists assignment_rearm_thresholds on public.chain_assignments;
create trigger assignment_rearm_thresholds after delete on public.chain_assignments
  for each row execute function public.trg_assignment_rearm_thresholds();
