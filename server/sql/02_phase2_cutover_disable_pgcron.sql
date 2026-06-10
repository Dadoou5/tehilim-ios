-- ============================================================================
-- PHASE 2 — Bascule du scheduling vers le VPS.
-- À appliquer MANUELLEMENT après avoir vérifié la PARITÉ en shadow :
--   - le process tehilim-scheduler tourne sur le VPS (DATABASE_URL configuré),
--   - ses logs montrent les 3 jobs exécutés sans erreur.
-- On DÉSACTIVE alors les jobs pg_cron équivalents pour éviter le double envoi.
-- (active=false conserve la définition → réactivation triviale.)
--
-- ⚠️ Ne pas démarrer tehilim-scheduler ET laisser pg_cron actif en même temps :
--    cela enverrait les rappels/lifecycle en double.
-- ============================================================================

update cron.job
   set active = false
 where jobname in ('chain_selection_reminders', 'chain_lifecycle', 'chain_cleanup_expired');

-- Vérification :
-- select jobid, jobname, schedule, active from cron.job order by jobid;

-- ----------------------------------------------------------------------------
-- ROLLBACK (réactiver pg_cron, et arrêter tehilim-scheduler côté VPS) :
-- update cron.job
--    set active = true
--  where jobname in ('chain_selection_reminders', 'chain_lifecycle', 'chain_cleanup_expired');
-- ----------------------------------------------------------------------------
