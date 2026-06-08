-- ============================================================================
-- VÉRIFICATION DE COHÉRENCE MÉTIER (source de vérité = la base).
-- Cible : les chaînes de test (name like 'LOADTEST_%'). Adapter le tag si besoin.
-- ============================================================================

-- (0) Doublons interdits : au plus 1 attribution par (chain_id, psalm_id).
--     La PRIMARY KEY le garantit ; on le PROUVE quand même. Attendu : 0 ligne.
select chain_id, psalm_id, count(*) as n
from public.chain_assignments
group by chain_id, psalm_id
having count(*) > 1;

-- (1) Psaumes hors bornes 1..150 (contrainte CHECK). Attendu : 0 ligne.
select chain_id, psalm_id
from public.chain_assignments
where psalm_id < 1 or psalm_id > 150;

-- (2) Couverture + fenêtre temporelle par chaîne de test.
--     covered = nb de Tehilim réservés (max 150) ; secs_to_cover = temps écoulé
--     entre la 1re et la dernière réservation (≈ temps pour parcourir les 150).
select
  c.id,
  c.name,
  count(a.psalm_id)                                   as covered,
  150 - count(a.psalm_id)                             as remaining,
  count(distinct a.uid)                               as distinct_readers,
  min(a.assigned_at)                                  as first_pick,
  max(a.assigned_at)                                  as last_pick,
  round(extract(epoch from (max(a.assigned_at) - min(a.assigned_at)))::numeric, 1)
                                                       as secs_to_cover
from public.chains c
left join public.chain_assignments a on a.chain_id = c.id
where c.name like 'LOADTEST_%'
group by c.id, c.name
order by c.name;

-- (3) Sur-couverture impossible : aucune chaîne ne doit dépasser 150. Attendu : 0.
select c.id, c.name, count(a.psalm_id) as covered
from public.chains c
join public.chain_assignments a on a.chain_id = c.id
where c.name like 'LOADTEST_%'
group by c.id, c.name
having count(a.psalm_id) > 150;

-- (4) Synthèse globale des chaînes de test.
select
  count(distinct c.id)                       as test_chains,
  count(a.*)                                 as total_assignments,
  count(distinct a.uid)                      as distinct_readers,
  count(distinct (a.chain_id, a.psalm_id))   as distinct_slots  -- = total si 0 doublon
from public.chains c
left join public.chain_assignments a on a.chain_id = c.id
where c.name like 'LOADTEST_%';
