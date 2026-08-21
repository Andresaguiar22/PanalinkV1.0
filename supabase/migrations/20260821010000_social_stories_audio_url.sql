-- Stories/Reels background audio support.
-- The Android client sends audio_url when a story/reel has background audio;
-- the column was missing and every insert failed with 42703.

alter table social.user_stories
    add column if not exists audio_url text;

alter table social.user_reels
    add column if not exists audio_url text;
