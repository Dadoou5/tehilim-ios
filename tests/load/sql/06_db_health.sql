-- Santé globale : cache hit ratio, transactions, tailles, deadlocks.
select
  round(100.0 * sum(blks_hit) / nullif(sum(blks_hit) + sum(blks_read), 0), 2) as cache_hit_pct,
  sum(xact_commit)   as commits,
  sum(xact_rollback) as rollbacks,   -- les 23505 (PK) comptent ici
  sum(deadlocks)     as deadlocks,
  sum(tup_inserted)  as tup_inserted,
  sum(tup_fetched)   as tup_fetched
from pg_stat_database
where datname = current_database();

-- Activité par table du flux chaîne (lectures index vs seq scans, n_live/dead).
select
  relname,
  n_tup_ins        as inserts,
  n_tup_del        as deletes,
  seq_scan,
  idx_scan,
  n_live_tup       as live_rows,
  n_dead_tup       as dead_rows
from pg_stat_user_tables
where relname in ('chains', 'chain_participants', 'chain_assignments')
order by relname;

-- Taille des tables/index du flux.
select
  relname,
  pg_size_pretty(pg_total_relation_size(relid)) as total_size,
  pg_size_pretty(pg_indexes_size(relid))        as index_size
from pg_catalog.pg_statio_user_tables
where relname in ('chains', 'chain_participants', 'chain_assignments')
order by pg_total_relation_size(relid) desc;
