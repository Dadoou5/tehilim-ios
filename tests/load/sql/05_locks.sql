-- Détection des VERROUS BLOQUANTS (à échantillonner pendant le plateau).
-- Sur ce schéma, la contention attendue est sur la PK (chain_id, psalm_id) :
-- des INSERT concurrents sur le même couple se sérialisent brièvement puis l'un
-- échoue en 23505. On veut vérifier qu'aucun blocage long ne s'installe.
select
  blocked.pid                       as blocked_pid,
  left(blocked.query, 80)           as blocked_query,
  blocking.pid                      as blocking_pid,
  left(blocking.query, 80)          as blocking_query,
  blocked.wait_event_type,
  blocked.wait_event,
  round(extract(epoch from (now() - blocked.query_start))::numeric, 3) as blocked_for_s
from pg_stat_activity blocked
join pg_stat_activity blocking
  on blocking.pid = any(pg_blocking_pids(blocked.pid))
where blocked.datname = current_database()
order by blocked_for_s desc;

-- Vue agrégée des locks en attente par type/mode.
select
  locktype,
  mode,
  granted,
  count(*) as n
from pg_locks
where not granted
group by locktype, mode, granted
order by n desc;
