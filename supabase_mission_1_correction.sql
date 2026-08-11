-- PANALINK — MISIÓN 1-CORRECCIÓN
-- MIGRACIÓN SQL FINAL (Aplicada para endurecimiento y coherencia)

-- 1. Endurecimiento de la tabla public.profiles
-- Agregamos columnas faltantes si no existen y aseguramos updated_at
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS first_name TEXT;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS last_name TEXT;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

-- 2. Trigger para actualizar updated_at automáticamente en profiles
CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp;

DROP TRIGGER IF EXISTS trg_profiles_updated_at ON public.profiles;
CREATE TRIGGER trg_profiles_updated_at
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW
    EXECUTE FUNCTION public.set_updated_at();

-- 3. Endurecimiento de la tabla public.public_profiles
-- Garantizar que existan todas las columnas de identidad pública
ALTER TABLE public.public_profiles ADD COLUMN IF NOT EXISTS first_name TEXT;
ALTER TABLE public.public_profiles ADD COLUMN IF NOT EXISTS last_name TEXT;

-- 4. Función de sincronización segura (profiles -> public_profiles)
-- Corregida para usar NEW.updated_at (coherencia de timestamps)
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
            NEW.updated_at -- Sincronización exacta del timestamp
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

-- 5. Revocar ejecución pública y de roles de cliente para la RPC de sincronización
REVOKE EXECUTE ON FUNCTION public.sync_profile_to_public_profile() FROM PUBLIC, anon, authenticated;

-- 6. Configuración de Grants mínimos para public_profiles
-- Solo lectura para usuarios autenticados.
REVOKE ALL ON public.public_profiles FROM anon, authenticated;
GRANT SELECT ON public.public_profiles TO authenticated;

-- 7. RLS final en public_profiles
ALTER TABLE public.public_profiles ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Public profiles read policy" ON public.public_profiles;
CREATE POLICY "Public profiles read policy" ON public.public_profiles
    FOR SELECT
    TO authenticated
    USING (true);

-- 8. Endurecimiento de funciones existentes (search_path)
ALTER FUNCTION public.handle_new_user() SET search_path = public, pg_temp;

-- 9. Backfill para corregir posibles desincronizaciones de updated_at
UPDATE public.public_profiles pp
SET 
    display_name = p.display_name,
    first_name = p.first_name,
    last_name = p.last_name,
    avatar_url = p.avatar_url,
    updated_at = p.updated_at
FROM public.profiles p
WHERE pp.id = p.id;
