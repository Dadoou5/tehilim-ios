-- État des connexions / sessions EN TEMPS RÉEL (à échantillonner pendant le test).
-- Sature-t-on les 60 connexions ? Combien actives vs idle / idle in transaction ?
select
  count(*)                                              as total_conns,
  count(*) filter (where state = 'active')              as active,
  count(*) filter (where state = 'idle')                as idle,
  count(*) filter (where state = 'idle in transaction') as idle_in_tx,
  count(*) filter (where wait_event_type = 'Lock')      as waiting_on_lock,
  max(extract(epoch from (now() - query_start)) )
      filter (where state = 'active')                   as longest_active_s,
  current_setting('max_connections')                    as max_connections
from pg_stat_activity
where datname = current_database();

-- Détail des sessions actives les plus anciennes (top 15).
select
  pid,
  usename,
  application_name,
  state,
  wait_event_type,
  wait_event,
  round(extract(epoch from (now() - query_start))::numeric, 2) as active_s,
  left(regexp_replace(query, '\s+', ' ', 'g'), 120)            as query
from pg_stat_activity
where datname = current_database()
  and state <> 'idle'
  and pid <> pg_backend_pid()
order by query_start asc
limit 15;
