-- Réinitialise les statistiques AVANT le test pour une attribution propre.
-- pg_stat_statements est installé dans le schéma `extensions` sur Supabase.
select extensions.pg_stat_statements_reset();
-- (les compteurs pg_stat_activity sont en temps réel, rien à reset)
select 'stats reset at ' || now() as info;
