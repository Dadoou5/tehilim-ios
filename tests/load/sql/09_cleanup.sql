-- ============================================================================
-- CLEANUP — supprime TOUTES les données de test. À lancer après le test.
-- La cascade (chains → participants + assignments) nettoie les tables filles.
-- Filtre strict sur le préfixe de tag pour ne JAMAIS toucher aux vraies chaînes.
-- ============================================================================

-- Aperçu avant suppression (sécurité : vérifier la liste).
select id, name, created_at from public.chains where name like 'LOADTEST_%' order by created_at;

-- Suppression (cascade ON DELETE → participants + assignments).
delete from public.chains where name like 'LOADTEST_%';

-- Contrôle : doit renvoyer 0.
select count(*) as remaining_test_chains from public.chains where name like 'LOADTEST_%';

-- NB : les utilisateurs anonymes créés dans auth.users par le test ne portent
-- aucune donnée applicative (aucune ligne après cleanup) et sont sans effet.
-- Leur purge éventuelle nécessite la service_role (admin) — hors périmètre MCP.
