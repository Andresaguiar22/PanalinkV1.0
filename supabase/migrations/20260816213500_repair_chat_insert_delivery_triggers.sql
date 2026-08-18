-- Chat delivery root-cause repair.
-- 1) thread_messages INSERTs were rolled back by trg_thread_messages_after_insert:
--    bump_relationship() was invoker-security and hit RLS on user_relationship_scores.
-- 2) mark_thread_delivered/read RPCs were blocked by guard_thread_message_update(),
--    because the trigger had no way to distinguish the trusted RPC path.

create or replace function public.bump_relationship(
  p_user_id uuid,
  p_related_user_id uuid,
  p_delta_message bigint default 0,
  p_delta_comment bigint default 0,
  p_delta_like bigint default 0,
  p_delta_score numeric default 0,
  p_is_favorite boolean default null
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  if p_user_id is null or p_related_user_id is null then
    return;
  end if;

  insert into public.user_relationship_scores (
    user_id, related_user_id, interaction_score,
    message_count, comment_count, like_count,
    last_interaction, is_favorite
  )
  values (
    p_user_id,
    p_related_user_id,
    greatest(coalesce(p_delta_score, 0), 0),
    greatest(coalesce(p_delta_message, 0), 0),
    greatest(coalesce(p_delta_comment, 0), 0),
    greatest(coalesce(p_delta_like, 0), 0),
    now(),
    coalesce(p_is_favorite, false)
  )
  on conflict (user_id, related_user_id)
  do update set
    interaction_score = public.user_relationship_scores.interaction_score + coalesce(p_delta_score, 0),
    message_count = public.user_relationship_scores.message_count + coalesce(p_delta_message, 0),
    comment_count = public.user_relationship_scores.comment_count + coalesce(p_delta_comment, 0),
    like_count = public.user_relationship_scores.like_count + coalesce(p_delta_like, 0),
    last_interaction = now(),
    is_favorite = case
      when p_is_favorite is null then public.user_relationship_scores.is_favorite
      else p_is_favorite
    end,
    updated_at = now();
end;
$$;

revoke execute on function public.bump_relationship(uuid, uuid, bigint, bigint, bigint, numeric, boolean) from public, anon, authenticated;

create or replace function public.mark_thread_delivered(p_thread_id uuid)
returns integer
language plpgsql
security definer
set search_path = 'public', 'pg_temp'
as $$
declare
  result integer := 0;
begin
  perform set_config('app.message_state_rpc', '1', true);

  update public.thread_messages
  set delivered_at = coalesce(delivered_at, now())
  where thread_id = p_thread_id
    and receiver_id = auth.uid()
    and sender_id <> auth.uid()
    and deleted_at is null
    and delivered_at is null;

  get diagnostics result = row_count;
  return result;
end;
$$;

grant execute on function public.mark_thread_delivered(uuid) to authenticated;

create or replace function public.mark_thread_read(p_thread_id uuid)
returns integer
language plpgsql
security definer
set search_path = 'public', 'pg_temp'
as $$
declare
  result integer := 0;
begin
  perform set_config('app.message_state_rpc', '1', true);

  update public.thread_messages
  set read_at = coalesce(read_at, now()),
      delivered_at = coalesce(delivered_at, now()),
      seen_at = coalesce(seen_at, now())
  where thread_id = p_thread_id
    and receiver_id = auth.uid()
    and sender_id <> auth.uid()
    and deleted_at is null
    and (read_at is null or delivered_at is null or seen_at is null);

  get diagnostics result = row_count;
  return result;
end;
$$;

grant execute on function public.mark_thread_read(uuid) to authenticated;

create or replace function public.guard_thread_message_update()
returns trigger
language plpgsql
security definer
set search_path = 'pg_catalog', 'public', 'pg_temp'
as $$
declare
  state_rpc boolean := current_setting('app.message_state_rpc', true) = '1';
begin
  if new.sender_id is distinct from old.sender_id
     or new.receiver_id is distinct from old.receiver_id
     or new.thread_id is distinct from old.thread_id
     or new.chat_id is distinct from old.chat_id
     or new.message_type is distinct from old.message_type
     or new.media_url is distinct from old.media_url
     or new.thumbnail_url is distinct from old.thumbnail_url
     or new.media_mime is distinct from old.media_mime
     or new.created_at is distinct from old.created_at
     or new.voice_duration is distinct from old.voice_duration
     or new.file_size is distinct from old.file_size
     or new.file_name is distinct from old.file_name
     or new.reply_to is distinct from old.reply_to
     or new.forwarded is distinct from old.forwarded
     or new.status is distinct from old.status
     or new.text is distinct from old.text
     or new.width is distinct from old.width
     or new.height is distinct from old.height
     or new.location is distinct from old.location
     or new.contacts is distinct from old.contacts
     or new.document is distinct from old.document
     or new.sticker is distinct from old.sticker
     or new.file_mime is distinct from old.file_mime
     or new.duration is distinct from old.duration
     or new.message_metadata is distinct from old.message_metadata
     or new.delivered_at_v2 is distinct from old.delivered_at_v2
     or new.read_at_v2 is distinct from old.read_at_v2
     or new.client_message_uuid is distinct from old.client_message_uuid
     or new.type is distinct from old.type
     or new.media_type is distinct from old.media_type
     or new.media_name is distinct from old.media_name
     or new.media_size is distinct from old.media_size
     or new.media_urls is distinct from old.media_urls
     or new.audio_url is distinct from old.audio_url
     or new.sticker_id is distinct from old.sticker_id
     or new.metadata_base64 is distinct from old.metadata_base64
  then
    raise exception 'immutable message fields cannot be modified';
  end if;

  if new.deleted_at is distinct from old.deleted_at
     or new.is_deleted is distinct from old.is_deleted then
    if new.text_content is distinct from old.text_content
       or (new.edited_at is distinct from old.edited_at and old.deleted_at is not null)
       or new.read_at is distinct from old.read_at
       or new.delivered_at is distinct from old.delivered_at
       or new.seen_at is distinct from old.seen_at then
      raise exception 'soft delete may only modify deletion state';
    end if;
  elsif new.text_content is distinct from old.text_content then
    if old.deleted_at is not null
       or new.deleted_at is distinct from old.deleted_at
       or new.read_at is distinct from old.read_at
       or new.delivered_at is distinct from old.delivered_at
       or new.seen_at is distinct from old.seen_at then
      raise exception 'text edit may only modify text and edit metadata';
    end if;
  elsif new.read_at is distinct from old.read_at
        or new.delivered_at is distinct from old.delivered_at
        or new.seen_at is distinct from old.seen_at then
    if not state_rpc then
      raise exception 'message delivery state must be updated through RPC';
    end if;
  end if;

  return new;
end;
$$;
