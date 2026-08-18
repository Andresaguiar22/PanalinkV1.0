-- Normalize historical server rows that already have authoritative delivery/read timestamps.
-- Do not fabricate delivery or read timestamps; the status trigger derives the
-- state from existing timestamps.

select set_config('app.message_state_rpc', '1', true);

update public.thread_messages
set delivered_at = delivered_at
where status = 'pending'::public.message_status_type
  and deleted_at is null
  and delivered_at is not null;

update public.thread_messages
set seen_at = seen_at
where status = 'pending'::public.message_status_type
  and deleted_at is null
  and seen_at is not null;
