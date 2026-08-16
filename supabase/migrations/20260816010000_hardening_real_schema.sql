begin;

revoke execute on function public.run_notification_backend_intelligence_smoke_tests() from public, anon, authenticated;

create or replace function social.can_view_reel(reel_id uuid)
returns boolean language sql stable security definer set search_path = ''
as $$
  select exists (
    select 1 from social.user_reels ur
    where ur.id = can_view_reel.reel_id
      and ur.moderation_status = 'visible'
      and auth.uid() is not null
  );
$$;

create or replace function social.can_view_story(p_story_id uuid)
returns boolean language sql stable security definer set search_path = 'social', 'public', 'auth'
as $$
  select exists (
    select 1 from social.user_stories us
    where us.id = p_story_id
      and (
        us.author_id = auth.uid()
        or (
          us.expires_at > now()
          and exists (
            select 1 from public.contacts c
            where c.owner_user_id = auth.uid()
              and c.contact_user_id = us.author_id
          )
        )
      )
  );
$$;

grant execute on function social.can_view_reel(uuid) to authenticated;
grant execute on function social.can_view_story(uuid) to authenticated;

create or replace function social.set_reel_like(p_reel_id uuid, p_liked boolean)
returns jsonb language plpgsql security definer set search_path = 'social', 'public', 'pg_temp'
as $$
declare v_count integer;
begin
  if auth.uid() is null then raise exception 'not authenticated' using errcode='42501'; end if;
  if not social.can_view_reel(p_reel_id) then raise exception 'reel not available' using errcode='42501'; end if;
  if p_liked then
    insert into social.reel_likes(reel_id,user_id) values(p_reel_id,auth.uid()) on conflict(reel_id,user_id) do nothing;
  else
    delete from social.reel_likes where reel_id=p_reel_id and user_id=auth.uid();
  end if;
  select coalesce(likes_count,0) into v_count from social.user_reels where id=p_reel_id;
  return jsonb_build_object('liked',p_liked,'likes_count',coalesce(v_count,0));
end; $$;

create or replace function social.set_reel_favorite(p_reel_id uuid, p_favorited boolean)
returns jsonb language plpgsql security definer set search_path = 'social', 'public', 'pg_temp'
as $$
declare v_count integer;
begin
  if auth.uid() is null then raise exception 'not authenticated' using errcode='42501'; end if;
  if not social.can_view_reel(p_reel_id) then raise exception 'reel not available' using errcode='42501'; end if;
  if p_favorited then
    insert into social.reel_favorites(reel_id,user_id) values(p_reel_id,auth.uid()) on conflict(reel_id,user_id) do nothing;
  else
    delete from social.reel_favorites where reel_id=p_reel_id and user_id=auth.uid();
  end if;
  select coalesce(favorites_count,0) into v_count from social.user_reels where id=p_reel_id;
  return jsonb_build_object('favorited',p_favorited,'favorites_count',coalesce(v_count,0));
end; $$;

create or replace function social.set_story_like(p_story_id uuid, p_liked boolean)
returns jsonb language plpgsql security definer set search_path = 'social', 'public', 'pg_temp'
as $$
declare v_count integer;
begin
  if auth.uid() is null then raise exception 'not authenticated' using errcode='42501'; end if;
  if not social.can_view_story(p_story_id) then raise exception 'story not available' using errcode='42501'; end if;
  if p_liked then
    insert into social.story_likes(story_id,user_id) values(p_story_id,auth.uid()) on conflict(story_id,user_id) do nothing;
  else
    delete from social.story_likes where story_id=p_story_id and user_id=auth.uid();
  end if;
  select coalesce(likes_count,0) into v_count from social.user_stories where id=p_story_id;
  return jsonb_build_object('liked',p_liked,'likes_count',coalesce(v_count,0));
end; $$;

create or replace function social.set_story_favorite(p_story_id uuid, p_favorited boolean)
returns jsonb language plpgsql security definer set search_path = 'social', 'public', 'pg_temp'
as $$
declare v_count integer;
begin
  if auth.uid() is null then raise exception 'not authenticated' using errcode='42501'; end if;
  if not social.can_view_story(p_story_id) then raise exception 'story not available' using errcode='42501'; end if;
  if p_favorited then
    insert into social.story_favorites(story_id,user_id) values(p_story_id,auth.uid()) on conflict(story_id,user_id) do nothing;
  else
    delete from social.story_favorites where story_id=p_story_id and user_id=auth.uid();
  end if;
  select coalesce(favorites_count,0) into v_count from social.user_stories where id=p_story_id;
  return jsonb_build_object('favorited',p_favorited,'favorites_count',coalesce(v_count,0));
end; $$;

do $$ declare r record; begin
  for r in select p.oid from pg_proc p join pg_namespace n on n.oid=p.pronamespace where n.nspname='social' and p.proname in ('toggle_reel_like','toggle_story_like','toggle_reel_favorite','toggle_story_favorite') loop
    execute format('revoke execute on function %s from public, anon, authenticated', r.oid::regprocedure);
  end loop;
end $$;

alter view public.chat_members set (security_invoker = true);
revoke all on public.chat_members from public, anon;
grant select, insert, update, delete on public.chat_members to authenticated;

commit;
