-- PANALINK V2.0 — SUPABASE PUBLIC PROFILE SECURITY & SYNCHRONIZATION MIGRATION
--
-- Objective:
-- 1. Ensure `public.public_profiles` table exists with required public identity columns.
-- 2. Restrict `public.profiles` RLS so private fields remain protected.
-- 3. Configure RLS on `public_profiles` granting SELECT to `authenticated` and `anon` users,
--    and blocking INSERT/UPDATE/DELETE from client APIs.
-- 4. Secure SECURITY DEFINER trigger to automatically sync `profiles` -> `public_profiles` on INSERT/UPDATE/DELETE.

-- Step 1: Create `public_profiles` table if it doesn't exist
CREATE TABLE IF NOT EXISTS public.public_profiles (
    id UUID PRIMARY KEY REFERENCES public.profiles(id) ON DELETE CASCADE,
    display_name TEXT,
    first_name TEXT,
    last_name TEXT,
    avatar_url TEXT,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Step 2: Enable RLS on `public_profiles`
ALTER TABLE public.public_profiles ENABLE ROW LEVEL SECURITY;

-- Step 3: Grant SELECT to authenticated and anon users on `public_profiles`
DROP POLICY IF EXISTS "Public profiles read policy" ON public.public_profiles;
CREATE POLICY "Public profiles read policy" ON public.public_profiles
    FOR SELECT
    TO authenticated, anon
    USING (true);

-- Explicitly revoke write permissions from authenticated and anon roles
REVOKE INSERT, UPDATE, DELETE, TRUNCATE ON public.public_profiles FROM authenticated, anon;

-- Step 4: Create secure sync function from `profiles` to `public_profiles`
CREATE OR REPLACE FUNCTION public.sync_profile_to_public_profile()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
BEGIN
    IF (TG_OP = 'DELETE') THEN
        DELETE FROM public.public_profiles WHERE id = OLD.id;
        RETURN OLD;
    ELSIF (TG_OP = 'INSERT' OR TG_OP = 'UPDATE') THEN
        INSERT INTO public.public_profiles (
            id,
            display_name,
            first_name,
            last_name,
            avatar_url,
            updated_at
        ) VALUES (
            NEW.id,
            NEW.display_name,
            NEW.first_name,
            NEW.last_name,
            NEW.avatar_url,
            COALESCE(NEW.updated_at, NOW())
        )
        ON CONFLICT (id) DO UPDATE SET
            display_name = EXCLUDED.display_name,
            first_name = EXCLUDED.first_name,
            last_name = EXCLUDED.last_name,
            avatar_url = EXCLUDED.avatar_url,
            updated_at = EXCLUDED.updated_at;
        RETURN NEW;
    END IF;
    RETURN NULL;
END;
$$;

-- Step 5: Attach trigger to `profiles` table
DROP TRIGGER IF EXISTS trigger_sync_public_profile ON public.profiles;
CREATE TRIGGER trigger_sync_public_profile
    AFTER INSERT OR UPDATE OR DELETE ON public.profiles
    FOR EACH ROW
    EXECUTE FUNCTION public.sync_profile_to_public_profile();

-- Step 6: Initial backfill of existing `profiles` into `public_profiles`
INSERT INTO public.public_profiles (id, display_name, first_name, last_name, avatar_url, updated_at)
SELECT id, display_name, first_name, last_name, avatar_url, COALESCE(updated_at, NOW())
FROM public.profiles
ON CONFLICT (id) DO UPDATE SET
    display_name = EXCLUDED.display_name,
    first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name,
    avatar_url = EXCLUDED.avatar_url,
    updated_at = EXCLUDED.updated_at;
