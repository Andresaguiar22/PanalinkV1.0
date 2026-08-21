-- Push notifications for the friend request flow.
-- The app adds contacts by sending a friend request (send_friend_request_by_pin/qr),
-- but nothing notified the receiver: no DB trigger existed for friend_requests
-- (only thread_messages had fcm_notify_on_new_message), so the other device
-- never found out and the request stayed pending forever.

create or replace function public.fcm_notify_on_friend_request()
returns trigger
language plpgsql
security definer
set search_path = 'pg_catalog', 'public', 'social', 'auth', 'realtime', 'extensions', 'net', 'pg_temp'
as $$
declare
  v_sender_name text;
  v_project_ref text := 'tivqjfgjdxgzicrridaz';
  v_payload jsonb;
  v_edge_secret text;
begin
  v_edge_secret := current_setting('app.edge_secret', true);
  if v_edge_secret is null or v_edge_secret = '' then
    return new;
  end if;

  select display_name into v_sender_name
    from public.profiles
   where id = new.sender_id;

  v_sender_name := coalesce(v_sender_name, 'Alguien');
  v_payload := jsonb_build_object(
    'user_id', new.receiver_id,
    'title', v_sender_name,
    'body', v_sender_name || ' quiere ser tu Pana. Toca para aceptar la solicitud.'
  );

  begin
    perform net.http_post(
      url := 'https://' || v_project_ref || '.functions.supabase.co/send-push',
      headers := jsonb_build_object(
        'Content-Type', 'application/json',
        'x-internal-secret', v_edge_secret
      ),
      body := v_payload
    );
  exception when others then
    raise warning 'FCM notification failed for friend_request %, transaction continues: %', new.id, sqlerrm;
  end;

  return new;
end;
$$;

drop trigger if exists trg_fcm_notify_friend_request on public.friend_requests;
create trigger trg_fcm_notify_friend_request
after insert on public.friend_requests
for each row execute function public.fcm_notify_on_friend_request();

-- Notify the sender when their request is accepted, so the contact appears
-- on their device without reopening the app.

create or replace function public.fcm_notify_on_friend_request_accepted()
returns trigger
language plpgsql
security definer
set search_path = 'pg_catalog', 'public', 'social', 'auth', 'realtime', 'extensions', 'net', 'pg_temp'
as $$
declare
  v_receiver_name text;
  v_project_ref text := 'tivqjfgjdxgzicrridaz';
  v_payload jsonb;
  v_edge_secret text;
begin
  if new.status <> 'accepted' or old.status = 'accepted' then
    return new;
  end if;

  v_edge_secret := current_setting('app.edge_secret', true);
  if v_edge_secret is null or v_edge_secret = '' then
    return new;
  end if;

  select display_name into v_receiver_name
    from public.profiles
   where id = new.receiver_id;

  v_receiver_name := coalesce(v_receiver_name, 'Tu Pana');
  v_payload := jsonb_build_object(
    'user_id', new.sender_id,
    'title', v_receiver_name,
    'body', v_receiver_name || ' aceptó tu solicitud. ¡Ya son Panas!'
  );

  begin
    perform net.http_post(
      url := 'https://' || v_project_ref || '.functions.supabase.co/send-push',
      headers := jsonb_build_object(
        'Content-Type', 'application/json',
        'x-internal-secret', v_edge_secret
      ),
      body := v_payload
    );
  exception when others then
    raise warning 'FCM notification failed for accepted friend_request %, transaction continues: %', new.id, sqlerrm;
  end;

  return new;
end;
$$;

drop trigger if exists trg_fcm_notify_friend_request_accepted on public.friend_requests;
create trigger trg_fcm_notify_friend_request_accepted
after update of status on public.friend_requests
for each row execute function public.fcm_notify_on_friend_request_accepted();

-- Publish friend_requests over Realtime so the app refreshes pending
-- requests live without reopening the Contacts tab.
do $$
begin
  alter publication supabase_realtime add table public.friend_requests;
exception when duplicate_object then
  null;
end;
$$;
