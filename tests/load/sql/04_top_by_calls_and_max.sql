-- (a) Top requêtes par NOMBRE D'APPELS (volume / chattiness).
select
  calls,
  round(mean_exec_time::numeric, 3)  as mean_ms,
  round(total_exec_time::numeric, 1) as total_ms,
  left(regexp_replace(query, '\s+', ' ', 'g'), 160) as query
from extensions.pg_stat_statements
where dbid = (select oid from pg_database where datname = current_database())
order by calls desc
limit 25;

-- (b) Top requêtes par TEMPS D'EXÉCUTION MAX (pires pics / latence queue).
select
  round(max_exec_time::numeric, 3)   as max_ms,
  round(mean_exec_time::numeric, 3)  as mean_ms,
  calls,
  left(regexp_replace(query, '\s+', ' ', 'g'), 160) as query
from extensions.pg_stat_statements
where dbid = (select oid from pg_database where datname = current_database())
order by max_exec_time desc
limit 25;
