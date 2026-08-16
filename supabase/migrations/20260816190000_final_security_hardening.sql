begin;

-- Final Panalink RLS / authorization hardening.

drop policy if exists story_views_select_open_by_story on social.story_views;
drop policy if exists story_views_insert_own on social.story_views;
create policy story_views_insert_own on social.story_views
for insert to authenticated
with check (viewer_id = auth.uid() and social.can_view_story(story_id));

drop policy if exists post_shares_select on public.post_shares;
create policy post_shares_select_owner_or_self on public.post_shares
for select to authenticated
using (
  user_id = auth.uid()
  or exists (select 1 from public.posts p where p.id = post_shares.post_id and p.user_id = auth.uid())
);

drop policy if exists chat_participants_insert_self_or_member on public.chat_participants;
drop policy if exists "chat_participants: insert self" on public.chat_participants;
create policy chat_participants_insert_self on public.chat_participants
for insert to authenticated
with check (user_id = auth.uid() and left_at is null and coalesce(role, 'member') = 'member');

drop policy if exists chat_participants_update_self on public.chat_participants;
drop policy if exists cp_update_own_row on public.chat_participants;
drop policy if exists "chat_participants: update self" on public.chat_participants;
create policy chat_participants_update_self on public.chat_participants
for update to authenticated
using (user_id = auth.uid())
with check (
  user_id = auth.uid()
  and role = (select cp.role from public.chat_participants cp where cp.chat_id = chat_participants.chat_id and cp.user_id = auth.uid() limit 1)
);

create or replace function public.guard_thread_message_update()
returns trigger
language plpgsql
security definer
set search_path = pg_catalog, public, pg_temp
as $$
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
  then raise exception 'immutable message fields cannot be modified';
  end if;

  if new.deleted_at is distinct from old.deleted_at or new.is_deleted is distinct from old.is_deleted then
    if new.text_content is distinct from old.text_content
       or new.edited_at is distinct from old.edited_at and old.deleted_at is not null
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
    raise exception 'message delivery state must be updated through RPC';
  end if;
  return new;
end;
$$;

drop trigger if exists trg_guard_thread_message_update on public.thread_messages;
create trigger trg_guard_thread_message_update
before update on public.thread_messages
for each row execute function public.guard_thread_message_update();

drop policy if exists reel_likes_insert_own on social.reel_likes;
create policy reel_likes_insert_own on social.reel_likes for insert to authenticated
with check (user_id = auth.uid() and social.can_view_reel(reel_id));
drop policy if exists reel_favorites_insert_own on social.reel_favorites;
create policy reel_favorites_insert_own on social.reel_favorites for insert to authenticated
with check (user_id = auth.uid() and social.can_view_reel(reel_id));
drop policy if exists reel_shares_insert_own on social.reel_shares;
create policy reel_shares_insert_own on social.reel_shares for insert to authenticated
with check (user_id = auth.uid() and social.can_view_reel(reel_id));
drop policy if exists reel_views_insert_own on social.reel_views;
create policy reel_views_insert_own on social.reel_views for insert to authenticated
with check (viewer_id = auth.uid() and social.can_view_reel(reel_id));
drop policy if exists reel_views_update_own on social.reel_views;
create policy reel_views_update_own on social.reel_views for update to authenticated
using (viewer_id = auth.uid() and social.can_view_reel(reel_id))
with check (viewer_id = auth.uid() and social.can_view_reel(reel_id));

delete from social.reel_likes l where not exists (select 1 from social.user_reels r where r.id = l.reel_id);
delete from social.reel_favorites f where not exists (select 1 from social.user_reels r where r.id = f.reel_id);
delete from social.reel_shares s where not exists (select 1 from social.user_reels r where r.id = s.reel_id);
delete from social.reel_views v where not exists (select 1 from social.user_reels r where r.id = v.reel_id);

alter table social.reel_likes add constraint reel_likes_reel_id_fkey foreign key (reel_id) references social.user_reels(id) on delete cascade;
alter table social.reel_favorites add constraint reel_favorites_reel_id_fkey foreign key (reel_id) references social.user_reels(id) on delete cascade;
alter table social.reel_shares add constraint reel_shares_reel_id_fkey foreign key (reel_id) references social.user_reels(id) on delete cascade;
alter table social.reel_views add constraint reel_views_reel_id_fkey foreign key (reel_id) references social.user_reels(id) on delete cascade;

revoke execute on function public.sync_profile_to_public_profile() from public, anon;
revoke execute on function public.update_post_shares_count() from public, anon;
revoke execute on function social.handle_reel_report() from anon;

-- Fix search_path for all application-owned functions without an explicit path.
do $$
declare r record;
begin
  for r in
    select n.nspname schema_name,p.proname,pg_get_function_identity_arguments(p.oid) args
    from pg_proc p join pg_namespace n on n.oid=p.pronamespace
    where n.nspname in ('public','social') and p.proconfig is null and p.probin is null and p.proname <> 'gen_random_bytes'
  loop
    execute format('alter function %I.%I(%s) set search_path = pg_catalog, public, social, auth, realtime, extensions, net, pg_temp',r.schema_name,r.proname,r.args);
  end loop;
end $$;

-- Internal helpers/predicates are not RPC entry points.
do $$
declare r record;
begin
  for r in
    select n.nspname schema_name,p.proname,pg_get_function_identity_arguments(p.oid) args
    from pg_proc p join pg_namespace n on n.oid=p.pronamespace
    where n.nspname in ('public','social') and p.probin is null
      and p.proname in ('guard_thread_message_update','block_identity_field_updates','block_contact_identifier_field_updates','tm_prevent_deleted_overwrite','trg_thread_messages_set_chat_id','trg_thread_messages_after_insert','trg_post_comments_after_insert','trg_post_likes_after_insert','trg_contacts_after_insert','trg_user_favorites_after_insert','trg_user_presence_set_timestamps','user_devices_protect_sensitive_fields','notifications_v2_restrict_updates','set_message_reactions_updated_at','thread_messages_unhide_on_insert','tm_set_edited_at_on_text_update','thread_messages_realtime_broadcast','notification_events_realtime_propagate','presence_realtime_broadcast_trigger','profiles_realtime_broadcast','user_media_statuses_realtime_broadcast','user_reels_broadcast_trigger','reel_comments_counters_trigger','reel_favorites_counters_trigger','reel_likes_counters_trigger','reel_shares_counters_trigger','story_counters_trigger','status_likes_counters_trigger','status_comments_counters_trigger','fn_reel_comments_set_user_text','fn_story_comments_set_user_text','fn_update_reel_comments_count','fn_update_reel_favorites_count','fn_update_reel_likes_count','fn_update_reel_shares_count','fn_update_reel_views_count','fn_update_story_comments_count','fn_update_story_favorites_count','fn_update_story_likes_count','fn_update_story_shares_count','reels_reaction_counters_update','stories_reaction_counters_update','recalc_status_likes_count','recalc_status_comments_count','can_view_reel','can_view_story','can_view_status')
  loop
    execute format('revoke execute on function %I.%I(%s) from public',r.schema_name,r.proname,r.args);
  end loop;
end $$;

commit;
