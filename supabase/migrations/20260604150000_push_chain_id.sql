-- ============================================================================
-- Tehilim — Chaîne : faire transiter `chainId` dans le push (deep-link au tap)
-- ----------------------------------------------------------------------------
-- Toutes les notifs de chaîne doivent, au tap, ouvrir l'écran de la chaîne
-- concernée. On ajoute donc `chainId` au corps envoyé à l'Edge Function, qui le
-- relaie dans la payload APNs (clé custom) et FCM (data) → iOS/Android routent.
-- ============================================================================

-- notify_chain : notif à TOUS les participants — + chainId.
create or replace function private.notify_chain(p_chain_id uuid, p_event text, p_value int)
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

  select coalesce(jsonb_agg(jsonb_build_object(
           'token', t.token, 'platform', t.platform, 'locale', t.locale)), '[]'::jsonb)
    into v_tokens
  from public.device_tokens t
  join public.chain_participants p on p.uid = t.uid
  where p.chain_id = p_chain_id;

  if v_tokens = '[]'::jsonb then return; end if;   -- personne à notifier

  perform net.http_post(
    url     := v_url,
    headers := jsonb_build_object(
                 'Content-Type', 'application/json',
                 'x-notify-secret', coalesce(v_secret, '')),
    body    := jsonb_build_object(
                 'event', p_event, 'value', p_value,
                 'chainName', coalesce(v_name, ''),
                 'chainId', p_chain_id, 'tokens', v_tokens)
  );
end;
$$;
revoke all on function private.notify_chain(uuid, text, int) from public;

-- notify_chain_creator : notif au CRÉATEUR seul (avec délai) — + chainId.
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

  select coalesce(jsonb_agg(jsonb_build_object(
           'token', t.token, 'platform', t.platform, 'locale', t.locale)), '[]'::jsonb)
    into v_tokens
  from public.device_tokens t
  join public.chains c on c.id = p_chain_id and c.creator_uid = t.uid;

  if v_tokens = '[]'::jsonb then return; end if;

  perform net.http_post(
    url     := v_url,
    headers := jsonb_build_object(
                 'Content-Type', 'application/json',
                 'x-notify-secret', coalesce(v_secret, '')),
    body    := jsonb_build_object(
                 'event', p_event, 'value', p_value,
                 'chainName', coalesce(v_name, ''),
                 'chainId', p_chain_id, 'tokens', v_tokens,
                 'delayMs', p_delay_ms)
  );
end;
$$;
revoke all on function private.notify_chain_creator(uuid, text, int, int) from public;
