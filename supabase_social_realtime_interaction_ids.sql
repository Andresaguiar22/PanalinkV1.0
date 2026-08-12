-- PanaLink Turbo: stable realtime transport IDs for social interactions
-- Keeps existing business primary keys and adds immutable UUID identifiers
-- used by Android Realtime idempotency.

begin;

create extension if not exists pgcrypto;

alter table social.reel_likes add column if not exists id uuid default gen_random_uuid();
alter table social.reel_favorites add column if not exists id uuid default gen_random_uuid();
alter table social.story_likes add column if not exists id uuid default gen_random_uuid();
alter table social.story_favorites add column if not exists id uuid default gen_random_uuid();

update social.reel_likes set id = gen_random_uuid() where id is null;
update social.reel_favorites set id = gen_random_uuid() where id is null;
update social.story_likes set id = gen_random_uuid() where id is null;
update social.story_favorites set id = gen_random_uuid() where id is null;

alter table social.reel_likes alter column id set not null;
alter table social.reel_favorites alter column id set not null;
alter table social.story_likes alter column id set not null;
alter table social.story_favorites alter column id set not null;

create unique index if not exists reel_likes_realtime_id_uidx on social.reel_likes(id);
create unique index if not exists reel_favorites_realtime_id_uidx on social.reel_favorites(id);
create unique index if not exists story_likes_realtime_id_uidx on social.story_likes(id);
create unique index if not exists story_favorites_realtime_id_uidx on social.story_favorites(id);

commit;
