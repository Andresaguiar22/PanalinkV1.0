-- Push notification delivery is a side effect and must never roll back a chat message.
-- Also use the actual thread_messages.text_content column.

create or replace function public.fcm_notify_on_new_message()
returns trigger
language plpgsql
security definer
set search_path = 'pg_catalog', 'public', 'social', 'auth', 'realtime', 'extensions', 'net', 'pg_temp'
as $$
declare
  v_recipient_id uuid;
  v_sender_name text;
  v_project_ref text := 'tivqjfgjdxgzicrridaz';
  v_payload jsonb;
  v_edge_secret text;
begin
  v_edge_secret := current_setting('app.edge_secret', true);
  if v_edge_secret is null or v_edge_secret = '' then
    return new;
  end if;

  select case when user_a = new.sender_id then user_b else user_a end
    into v_recipient_id
    from public.one_to_one_threads
   where id = new.thread_id;

  select display_name into v_sender_name
    from public.profiles
   where id = new.sender_id;

  v_sender_name := coalesce(v_sender_name, 'Mensaje nuevo');
  v_payload := jsonb_build_object(
    'user_id', v_recipient_id,
    'title', v_sender_name,
    'body', coalesce(new.text_content, '')
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
    raise warning 'FCM notification failed for thread message %, transaction continues: %', new.id, sqlerrm;
  end;

  return new;
end;
$$;
