-- Legacy/channel messages must be idempotent for the same reason as thread_messages.
-- The Android client retries POSTs after timeouts, so client_message_uuid must be
-- globally unique in the legacy table as well. Existing production data was
-- checked before adding this constraint: all rows have a UUID and there are no
-- duplicate UUID groups.

create unique index if not exists messages_client_message_uuid_uniq
  on public.messages (client_message_uuid);
