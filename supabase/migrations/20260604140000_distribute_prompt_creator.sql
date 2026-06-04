-- ============================================================================
-- Tehilim — Chaîne : invitation à DISTRIBUER (créateur seul), 3 s après le 100 %
-- ----------------------------------------------------------------------------
-- Quand une chaîne atteint 100 %, en plus de la notif « complétée » envoyée à
-- TOUS les participants, on envoie une notif au CRÉATEUR seul l'invitant à
-- distribuer la chaîne — décalée de 3 s pour ne pas se superposer au 100 %.
--
-- Délai : pg_cron est trop grossier (1 min) et pg_net n'a pas de délai. On poste
-- donc un 2ᵉ appel à l'Edge Function avec un champ `delayMs` ; l'Edge Function
-- attend ce délai avant d'envoyer (cf. supabase/functions/notify/index.ts).
-- ============================================================================

-- Variante de notify_chain ciblant uniquement le CRÉATEUR, avec délai d'envoi.
create or replace function private.notify_chain_creator(
  p_chain_id uuid, p_event text, p_value int, p_delay_ms int)
returns void
language plpgsql
security definer
set search_path = public, private, extensions
as $$
declare
  v_url    text;
  v_secret text;
  v_name   text;
  v_tokens jsonb;
begin
  select value into v_url    from private.config where key = 'notify_url';
  select value into v_secret from private.config where key = 'notify_secret';
  if v_url is null then return; end if;

  select name into v_name from public.chains where id = p_chain_id;

  -- Tokens du créateur uniquement (device_tokens ⋈ chains.creator_uid).
  select coalesce(jsonb_agg(jsonb_build_object(
           'token', t.token, 'platform', t.platform, 'locale', t.locale)), '[]'::jsonb)
    into v_tokens
  from public.device_tokens t
  join public.chains c on c.id = p_chain_id and c.creator_uid = t.uid;

  if v_tokens = '[]'::jsonb then return; end if;   -- créateur sans appareil

  perform net.http_post(
    url     := v_url,
    headers := jsonb_build_object(
                 'Content-Type', 'application/json',
                 'x-notify-secret', coalesce(v_secret, '')),
    body    := jsonb_build_object(
                 'event', p_event, 'value', p_value,
                 'chainName', coalesce(v_name, ''), 'tokens', v_tokens,
                 'delayMs', p_delay_ms)
  );
end;
$$;
revoke all on function private.notify_chain_creator(uuid, text, int, int) from public;

-- Trigger d'attribution : 70/80/90 % + 100 % complétée + invitation à distribuer.
create or replace function public.trg_assignment_thresholds()
returns trigger
language plpgsql
security definer
set search_path = public, private
as $$
declare v_count int;
begin
  select count(*) into v_count from public.chain_assignments where chain_id = NEW.chain_id;
  if v_count >= 105 then
    update public.chains set notified_70 = true where id = NEW.chain_id and notified_70 = false;
    if found then perform private.notify_chain(NEW.chain_id, 'threshold', 70); end if;
  end if;
  if v_count >= 120 then
    update public.chains set notified_80 = true where id = NEW.chain_id and notified_80 = false;
    if found then perform private.notify_chain(NEW.chain_id, 'threshold', 80); end if;
  end if;
  if v_count >= 135 then
    update public.chains set notified_90 = true where id = NEW.chain_id and notified_90 = false;
    if found then perform private.notify_chain(NEW.chain_id, 'threshold', 90); end if;
  end if;
  if v_count >= 150 then
    update public.chains set notified_complete = true where id = NEW.chain_id and notified_complete = false;
    if found then
      perform private.notify_chain(NEW.chain_id, 'complete', null);
      -- 3 s après le 100 %, on invite le créateur seul à distribuer la chaîne.
      perform private.notify_chain_creator(NEW.chain_id, 'distribute_prompt', null, 3000);
    end if;
  end if;
  return NEW;
end;
$$;
