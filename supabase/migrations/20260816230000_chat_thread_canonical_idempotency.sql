-- Chat send hardening: canonical one-to-one identity + race-safe thread creation.
--
-- The client can resolve a DM by either participant ordering. The old
-- UNIQUE(user_a,user_b) constraint still allowed the reverse pair to exist,
-- which could produce two thread ids for the same conversation and make a
-- message appear to be sent to one thread while the reader is subscribed to
-- the other. Normalize existing duplicates before enforcing the unordered-pair
-- uniqueness rule.

DO $$
DECLARE
    duplicate RECORD;
BEGIN
    FOR duplicate IN
        SELECT
            LEAST(user_a, user_b) AS canonical_a,
            GREATEST(user_a, user_b) AS canonical_b,
            MIN(id) AS canonical_thread_id,
            ARRAY_AGG(id ORDER BY id) AS thread_ids
        FROM public.one_to_one_threads
        GROUP BY LEAST(user_a, user_b), GREATEST(user_a, user_b)
        HAVING COUNT(*) > 1
    LOOP
        -- Preserve messages from duplicate threads by moving them to the
        -- canonical thread before removing the duplicate thread rows.
        UPDATE public.thread_messages
        SET thread_id = duplicate.canonical_thread_id
        WHERE thread_id = ANY(duplicate.thread_ids)
          AND thread_id <> duplicate.canonical_thread_id;

        DELETE FROM public.one_to_one_threads
        WHERE id = ANY(duplicate.thread_ids)
          AND id <> duplicate.canonical_thread_id;
    END LOOP;
END $$;

DROP INDEX IF EXISTS one_to_one_threads_user_pair_uidx;
CREATE UNIQUE INDEX IF NOT EXISTS one_to_one_threads_canonical_pair_uidx
    ON public.one_to_one_threads (
        LEAST(user_a, user_b),
        GREATEST(user_a, user_b)
    );

-- Make thread creation race-safe. Two simultaneous sends for a new DM now
-- serialize on the same advisory lock and converge on one canonical thread.
CREATE OR REPLACE FUNCTION public.ensure_one_to_one_thread(
    p_user_a uuid,
    p_user_b uuid
)
RETURNS uuid
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE
    v_a uuid;
    v_b uuid;
    v_thread_id uuid;
    v_lock_key bigint;
BEGIN
    IF p_user_a IS NULL OR p_user_b IS NULL OR p_user_a = p_user_b THEN
        RAISE EXCEPTION 'Invalid one-to-one participants';
    END IF;

    v_a := LEAST(p_user_a, p_user_b);
    v_b := GREATEST(p_user_a, p_user_b);
    v_lock_key := hashtextextended(v_a::text || ':' || v_b::text, 0);

    PERFORM pg_advisory_xact_lock(v_lock_key);

    SELECT id
      INTO v_thread_id
      FROM public.one_to_one_threads
     WHERE user_a = v_a
       AND user_b = v_b
     LIMIT 1;

    IF v_thread_id IS NULL THEN
        INSERT INTO public.one_to_one_threads (user_a, user_b, created_at)
        VALUES (v_a, v_b, now())
        RETURNING id INTO v_thread_id;
    END IF;

    RETURN v_thread_id;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.ensure_one_to_one_thread(uuid, uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.ensure_one_to_one_thread(uuid, uuid) TO authenticated;

-- Keep the message idempotency guarantee explicit on both message stores.
CREATE UNIQUE INDEX IF NOT EXISTS thread_messages_client_message_uuid_uidx
    ON public.thread_messages (client_message_uuid)
    WHERE client_message_uuid IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS messages_client_message_uuid_uidx
    ON public.messages (client_message_uuid)
    WHERE client_message_uuid IS NOT NULL;
