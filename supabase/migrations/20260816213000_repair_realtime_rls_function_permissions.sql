-- Realtime evaluates RLS policies while applying WAL changes.
-- These helpers are referenced by social RLS policies, so authenticated users
-- must be allowed to execute them. Without EXECUTE, Realtime replication can
-- fail globally (including thread_messages), even though Broadcast still works.

GRANT EXECUTE ON FUNCTION social.can_view_reel(uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION social.can_view_story(uuid) TO authenticated;
