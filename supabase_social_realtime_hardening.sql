-- PanaLink social realtime hardening
-- Applied to Supabase project tivqjfgjdxgzicrridaz.
-- Keeps the repository's SQL source aligned with the live database.

alter publication supabase_realtime add table social.story_likes;
alter publication supabase_realtime add table social.story_comments;
alter publication supabase_realtime add table social.story_favorites;
alter publication supabase_realtime add table social.story_shares;

alter table social.reel_likes replica identity full;
alter table social.reel_comments replica identity full;
alter table social.reel_favorites replica identity full;
alter table social.reel_shares replica identity full;
alter table social.story_likes replica identity full;
alter table social.story_comments replica identity full;
alter table social.story_favorites replica identity full;
alter table social.story_shares replica identity full;
