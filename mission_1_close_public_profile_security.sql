-- PANALINK — MISIÓN 1 CORRECCIÓN FINAL
-- Migración: mission_1_close_public_profile_security

-- 1. Cerrar ejecución pública de la función de sincronización
-- Se revoca el permiso EXECUTE de los roles de cliente y de PUBLIC.
-- La función permanece SECURITY DEFINER para ser ejecutada por el trigger del sistema.
REVOKE EXECUTE ON FUNCTION public.sync_profile_to_public_profile() FROM anon;
REVOKE EXECUTE ON FUNCTION public.sync_profile_to_public_profile() FROM authenticated;
REVOKE EXECUTE ON FUNCTION public.sync_profile_to_public_profile() FROM PUBLIC;

-- 2. Endurecimiento de la función con search_path seguro y referencias explícitas
CREATE OR REPLACE FUNCTION public.sync_profile_to_public_profile()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
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
            NEW.updated_at
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

-- 3. Asegurar search_path seguro en otras funciones críticas
ALTER FUNCTION public.set_updated_at() SET search_path = pg_catalog, public;
ALTER FUNCTION public.handle_new_user() SET search_path = pg_catalog, public;

-- 4. Reconciliación de datos existentes (Sincronización profiles -> public_profiles)
-- Garantiza que public_profiles refleje fielmente los campos públicos de profiles, incluyendo updated_at.
INSERT INTO public.public_profiles (id, display_name, first_name, last_name, avatar_url, updated_at)
SELECT id, display_name, first_name, last_name, avatar_url, updated_at
FROM public.profiles
ON CONFLICT (id) DO UPDATE SET
    display_name = EXCLUDED.display_name,
    first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name,
    avatar_url = EXCLUDED.avatar_url,
    updated_at = EXCLUDED.updated_at;

-- 5. Endurecimiento de RLS en profiles (Hacerla PRIVADA)
-- Revocamos la lectura global que existía anteriormente.
DROP POLICY IF EXISTS "Allow authenticated users to read all profiles" ON public.profiles;
DROP POLICY IF EXISTS "Profiles are viewable by users who created them." ON public.profiles;

CREATE POLICY "Profiles are viewable by users who created them."
ON public.profiles FOR SELECT
TO authenticated
USING (auth.uid() = id);

-- 6. Garantizar Grants mínimos en public_profiles
-- Solo SELECT para usuarios autenticados.
REVOKE ALL ON public.public_profiles FROM anon, authenticated, PUBLIC;
GRANT SELECT ON public.public_profiles TO authenticated;

-- 7. Asegurar que el trigger de updated_at en profiles esté activo
DROP TRIGGER IF EXISTS trg_profiles_updated_at ON public.profiles;
CREATE TRIGGER trg_profiles_updated_at
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW
    EXECUTE FUNCTION public.set_updated_at();

-- 8. Verificación de integridad: FK con ON DELETE CASCADE ya existe en la definición de la tabla.
-- (Verificado en el esquema: id uuid primary key references public.profiles(id) on delete cascade)
