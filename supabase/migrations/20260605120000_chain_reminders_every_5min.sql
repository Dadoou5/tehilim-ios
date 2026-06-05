-- Le rappel « 20 % restant » (selection_reminder à 80 %) + la « dernière chance »
-- (95 %) dépendaient d'un cron horaire : pour des durées de sélection courtes, la
-- fenêtre 80→100 % ne contenait souvent aucun tick → rappel manqué.
-- On passe à toutes les 5 minutes (process_chain_reminders est une requête légère
-- et idempotente grâce aux flags notified_*).
select cron.schedule('chain_selection_reminders', '*/5 * * * *', 'select public.process_chain_reminders();');
