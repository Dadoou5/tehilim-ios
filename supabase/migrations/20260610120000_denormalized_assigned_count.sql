-- ============================================================================
-- Optimisation : compteur dénormalisé chains.assigned_count
-- ----------------------------------------------------------------------------
-- Remplace le `SELECT count(*) FROM chain_assignments WHERE chain_id = …`
-- exécuté À CHAQUE sélection (coût O(n), cumul quadratique par chaîne — cf.
-- LOAD_TEST_REPORT §8.2) par un compteur maintenu en +1/-1 dans les triggers
-- (coût O(1)). Les crons lisent la colonne au lieu de recompter.
--
-- Comportement métier INCHANGÉ : mêmes seuils (105/120/135/150), mêmes notifs.
-- Non destructif. Idempotent (IF NOT EXISTS / OR REPLACE).
--
-- Sécurité concurrence : l'+1 verrouille brièvement la ligne `chains` (≤150
-- inserts réussis/chaîne ; les perdants échouent sur la PK AVANT le trigger).
-- Sur DELETE en cascade (suppression de chaîne), l'UPDATE matche 0 ligne (la
-- chaîne est déjà supprimée) → no-op sûr, comme le trigger actuel.
-- ============================================================================

-- 1) Colonne + backfill ------------------------------------------------------
alter table public.chains add column if not exists assigned_count int not null default 0;

update public.chains c
   set assigned_count = (select count(*) from public.chain_assignments a where a.chain_id = c.id);

-- 2) AFTER INSERT : +1 + récupération en une écriture (remplace count(*)) -----
create or replace function public.trg_assignment_thresholds()
returns trigger
language plpgsql
security definer
set search_path to 'public', 'private'
as $function$
declare v_count int;
begin
  update public.chains set assigned_count = assigned_count + 1
   where id = NEW.chain_id
   returning assigned_count into v_count;

  if v_count >= 105 then
    update public.chains set notified_70 = true where id = NEW.chain_id and notified_70 = false;
    if found then perform private.notify_chain(NEW.chain_id, 'threshold', 70); end if;
  end if;
  if v_count >= 120 then
    update public.chains set notified_80 = true where id = NEW.chain_id and notified_80 = false;
    if found then perform private.notify_chain(NEW.chain_id, 'threshold', 80); end if;
  end if;
  if v_count >= 135 then
    update public.chains set notified_90 = true where id = NEW.chain_id and notified_90 = false;
    if found then perform private.notify_chain(NEW.chain_id, 'threshold', 90); end if;
  end if;
  if v_count >= 150 then
    update public.chains set notified_complete = true where id = NEW.chain_id and notified_complete = false;
    if found then
      perform private.notify_chain(NEW.chain_id, 'complete', null);
      perform private.notify_chain_creator(NEW.chain_id, 'distribute_prompt', null, 3000);
    end if;
  end if;
  return NEW;
end;
$function$;

-- 3) AFTER DELETE : -1 + ré-armement des flags en un seul UPDATE --------------
-- (les RHS référencent la valeur AVANT update ⇒ `assigned_count - 1` = nouveau total)
create or replace function public.trg_assignment_rearm_thresholds()
returns trigger
language plpgsql
security definer
set search_path to 'public', 'private'
as $function$
begin
  update public.chains set
    assigned_count    = assigned_count - 1,
    notified_70       = (notified_70       and assigned_count - 1 >= 105),
    notified_80       = (notified_80       and assigned_count - 1 >= 120),
    notified_90       = (notified_90       and assigned_count - 1 >= 135),
    notified_complete = (notified_complete and assigned_count - 1 >= 150)
  where id = OLD.chain_id;
  return OLD;
end;
$function$;

-- 4) Crons : lecture de la colonne au lieu de count(*) -----------------------
create or replace function public.process_chain_lifecycle()
returns void
language plpgsql
security definer
set search_path to 'public', 'private'
as $function$
declare r record; v_remaining int;
begin
  for r in
    select id, auto_extended, assigned_count
    from public.chains
    where distributed = false and now() >= selection_deadline
  loop
    v_remaining := 150 - r.assigned_count;
    if v_remaining > 0 and r.auto_extended = false then
      update public.chains
         set selection_deadline = selection_deadline + interval '3 hours',
             auto_extended = true,
             notified_selection_reminder = false,
             notified_final_reminder = false
       where id = r.id;
      perform private.notify_chain(r.id, 'auto_extended', v_remaining);
    else
      update public.chains set distributed = true where id = r.id;
    end if;
  end loop;
end;
$function$;

create or replace function public.process_chain_reminders()
returns void
language plpgsql
security definer
set search_path to 'public', 'private'
as $function$
declare r record; v_remaining int;
begin
  for r in
    select id, assigned_count from public.chains
    where distributed = false
      and notified_selection_reminder = false
      and now() >= created_at + (selection_deadline - created_at) * 0.8
      and now() < selection_deadline
  loop
    update public.chains set notified_selection_reminder = true where id = r.id;
    v_remaining := 150 - r.assigned_count;
    if v_remaining > 0 then
      perform private.notify_chain(r.id, 'selection_reminder', v_remaining);
    end if;
  end loop;

  for r in
    select id, assigned_count from public.chains
    where distributed = false
      and notified_final_reminder = false
      and now() >= created_at + (selection_deadline - created_at) * 0.95
      and now() < selection_deadline
  loop
    update public.chains set notified_final_reminder = true where id = r.id;
    v_remaining := 150 - r.assigned_count;
    if v_remaining > 0 then
      perform private.notify_chain(r.id, 'final_reminder', v_remaining);
    end if;
  end loop;
end;
$function$;

-- ----------------------------------------------------------------------------
-- ROLLBACK : restaurer les 4 fonctions depuis les migrations d'origine
--   (push_notifications, distribute_prompt_creator, rearm_thresholds_on_deselect,
--    chain_reminders_every_5min, open_limit_autoextend_autodistribute) et, si
--   souhaité : alter table public.chains drop column assigned_count;
-- La colonne peut rester sans risque même après rollback des fonctions.
-- ----------------------------------------------------------------------------
