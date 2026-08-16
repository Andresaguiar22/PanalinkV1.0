-- Chat delivery hardening.
-- The thread_messages INSERT policy checks membership in one_to_one_threads.
-- Participant SELECT access is therefore required for that authorization path.
create policy one_to_one_threads_select_participant
on public.one_to_one_threads
for select
to authenticated
using (auth.uid() = user_a or auth.uid() = user_b);

create policy one_to_one_threads_insert_participant
on public.one_to_one_threads
for insert
to authenticated
with check (auth.uid() = user_a or auth.uid() = user_b);

-- Mobile retries are keyed by client_message_uuid and must be idempotent.
create unique index if not exists thread_messages_client_message_uuid_uidx
on public.thread_messages (client_message_uuid);

-- Client edits only send text_content. The server supplies the edit marker.
create or replace function public.set_thread_message_edit_timestamp()
returns trigger
language plpgsql
security invoker
set search_path = public
as $$
begin
  if new.text_content is distinct from old.text_content then
    new.edited_at := coalesce(new.edited_at, now());
    new.is_edited := true;
    new.updated_at := now();
  end if;
  return new;
end;
$$;

drop trigger if exists trg_thread_message_edit_timestamp on public.thread_messages;
create trigger trg_thread_message_edit_timestamp
before update on public.thread_messages
for each row execute function public.set_thread_message_edit_timestamp();

revoke execute on function public.mark_thread_delivered(uuid) from anon;
grant execute on function public.mark_thread_delivered(uuid) to authenticated;
revoke execute on function public.mark_thread_read(uuid) from anon;
grant execute on function public.mark_thread_read(uuid) to authenticated;
