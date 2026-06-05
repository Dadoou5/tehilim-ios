-- Nettoyage quotidien des chaînes expirées (expires_at = fin de lecture + 7 jours).
-- Double la sécurité du cron GitHub (keep-alive) : la suppression a lieu même si
-- l'Action GitHub est désactivée (inactivité du repo) ou en échec.
-- cleanup_expired_chains() est SECURITY DEFINER + garde `expires_at < now()`.
select cron.schedule('chain_cleanup_expired', '17 3 * * *', 'select public.cleanup_expired_chains();');
