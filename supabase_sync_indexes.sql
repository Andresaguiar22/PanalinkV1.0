-- SQL Script to optimize Supabase sync operations in thread_messages table.
-- Apply this script in your Supabase SQL Editor.

-- 1. Single column index to speed up raw queries filtered or ordered by updated_at
CREATE INDEX IF NOT EXISTS idx_thread_messages_updated_at 
ON thread_messages (updated_at ASC);

-- 2. Composite index to optimize incremental queries targeting a specific thread sorted by updated_at
CREATE INDEX IF NOT EXISTS idx_thread_messages_thread_id_updated_at 
ON thread_messages (thread_id, updated_at ASC);

-- 3. Composite index to optimize incremental queries targeting a specific chat/conversation sorted by updated_at
CREATE INDEX IF NOT EXISTS idx_thread_messages_chat_id_updated_at 
ON thread_messages (chat_id, updated_at ASC);
