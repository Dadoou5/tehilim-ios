-- Top requêtes par TEMPS CUMULÉ (le vrai coût agrégé sur le test).
-- À exécuter APRÈS le test (les stats ont été reset avant via 01_reset_stats.sql).
select
  calls,
  round(total_exec_time::numeric, 1)           as total_ms,
  round(mean_exec_time::numeric, 3)            as mean_ms,
  round(max_exec_time::numeric, 3)            as max_ms,
  round((100 * total_exec_time / nullif(sum(total_exec_time) over (), 0))::numeric, 1) as pct_total,
  rows,
  left(regexp_replace(query, '\s+', ' ', 'g'), 160) as query
from extensions.pg_stat_statements
where dbid = (select oid from pg_database where datname = current_database())
order by total_exec_time desc
limit 25;
