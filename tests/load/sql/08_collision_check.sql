-- ============================================================================
-- VÉRIFICATION DU TEST DE COLLISION (chaîne 'LOADTEST_collision_%').
-- Attendu strict : au plus 1 attribution par (chain_id, psalm_id), 0 doublon,
-- état final cohérent malgré N tentatives simultanées sur les mêmes psaumes.
-- ============================================================================
with col as (
  select id from public.chains where name like 'LOADTEST_collision_%'
)
select
  a.psalm_id,
  count(*)               as winners,          -- DOIT être 1 partout
  count(distinct a.uid)  as distinct_winners, -- DOIT être 1 partout
  bool_or(false)         as _                  -- placeholder
from public.chain_assignments a
join col on col.id = a.chain_id
group by a.psalm_id
order by a.psalm_id;

-- Verdict : 0 ligne = parfait (aucun psaume gagné deux fois).
with col as (select id from public.chains where name like 'LOADTEST_collision_%')
select a.chain_id, a.psalm_id, count(*) as winners
from public.chain_assignments a
join col on col.id = a.chain_id
group by a.chain_id, a.psalm_id
having count(*) > 1;

-- Combien de psaumes distincts ont été gagnés (= nb de gagnants uniques).
with col as (select id from public.chains where name like 'LOADTEST_collision_%')
select count(*) as psalms_won, count(distinct a.uid) as distinct_uids
from public.chain_assignments a
join col on col.id = a.chain_id;
