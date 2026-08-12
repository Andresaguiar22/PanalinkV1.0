# PanaLink — Auditoría Integral Inicial

Fecha: 2026-08-11
Rama de auditoría: `audit/full-app-2026-08-11`
Base: `main` @ `496f9e41bc7395fb38079a3c281adadd15c86251`

## Alcance

Auditoría integral del repositorio completo: Android/Compose, Room, Supabase/PostgreSQL, Realtime, repositories, WorkManager, Feed/Muro, Reels, Stories/States, identidad, multimedia/CDN, notificaciones, WebRTC, servidor Node y tests.

## Hallazgos iniciales confirmados

### 1. Repositorio con proyecto anidado adicional

Existe `app/applet/` con su propio `.github/` y `app/`, además del módulo Android real `app/`. `settings.gradle.kts` del proyecto raíz únicamente incluye `:app`, por lo que `app/applet` no forma parte del build raíz. Debe tratarse como posible copia/artefacto histórico hasta comprobar su procedencia y contenido antes de eliminarlo.

### 2. Fuente de verdad de Reels en cliente

`StatesRepository` obtiene Reels mediante `SupabaseApiService.getUserReels()` usando el esquema `social`, no mediante `get_reels_feed_v2`. El RPC `social.get_reels_feed` y `social.get_reels_feed_v2` existen en la base de datos, pero el cliente auditado no los utiliza para el feed principal. Esto confirma la existencia de dos caminos backend posibles; hay que decidir cuál queda como único contrato canónico sin romper compatibilidad.

### 3. Duplicación de RPC de interacción de Reels

En Supabase existen simultáneamente `set_reel_like` / `set_reel_favorite` y `toggle_reel_like` / `toggle_reel_favorite`. El cliente actual utiliza los RPC `set_*` desde `SocialSyncWorker`. Los `toggle_*` son una arquitectura paralela que debe considerarse legado hasta verificar dependencias.

### 4. Contadores de Reels tienen triggers activos

Las tablas `social.reel_likes`, `reel_comments`, `reel_favorites`, `reel_shares` y `reel_views` tienen triggers de actualización de contadores. Esto es correcto como fuente de verdad backend, pero debe mantenerse un único mecanismo de mutación para evitar dobles actualizaciones si algún cliente usa simultáneamente RPCs y actualizaciones manuales.

### 5. Divergencia entre SQL versionado y base real

El `supabase_schema.sql` versionado en el repositorio no contiene la totalidad de las tablas actuales de `social`, Reels ni todas las tablas actuales de publicaciones. La base real sí contiene `social.user_reels`, `social.reel_likes`, `social.reel_comments`, `social.reel_favorites`, `social.reel_shares`, `social.reel_views`, además de `public.posts`, `post_likes`, `post_comments` y `post_shares`. Esto constituye una divergencia importante entre infraestructura declarada en Git y estado real de Supabase.

### 6. Feed/Muro y Reels están físicamente separados

El cliente utiliza `FeedRepositoryImpl` y `public.posts` para el Muro, mientras `StatesRepository` utiliza `social.user_reels` para Reels. No se observa en esta primera pasada una mezcla directa de tablas entre ambos módulos.

### 7. RLS de Reels requiere revisión de modelo de privacidad

La política actual `social.reels_select_authenticated` permite SELECT sobre `social.user_reels` con `USING (true)`. Las tablas de interacciones de Reels restringen sus SELECT mediante `social.can_view_reel(reel_id)`, pero `can_view_reel()` actualmente comprueba solamente la existencia del Reel. Debe verificarse que esto coincida con el modelo de privacidad previsto para PanaLink y no convierta todo Reel en contenido global por defecto.

### 8. Feed tiene persistencia local y cola offline

`FeedRepositoryImpl` actualiza Room inmediatamente para likes, shares y comentarios y encola `PendingSocialAction` para `SocialSyncWorker`. Esto es consistente con el patrón Offline-First. La auditoría siguiente debe comprobar confirmación remota, rollback/reconciliación y Realtime para el Muro con el mismo nivel de rigor aplicado a Reels.

## Próxima fase

1. Cruzar todos los contratos Android ↔ Supabase por dominio.
2. Auditar Realtime tabla por tabla.
3. Auditar RLS y funciones SECURITY DEFINER.
4. Auditar Room/DAO/Entity contra las columnas reales de Supabase.
5. Auditar duplicaciones de repositorios, RPCs y modelos.
6. Auditar multimedia/CDN y rendimiento de reproducción.
7. Auditar Workers y recuperación offline.
8. Auditar Feed/Muro completo.
9. Auditar Reels/Stories completo.
10. Auditar Chat, identidad, notificaciones y llamadas.
11. Ejecutar/validar tests disponibles y revisar cobertura de regresión.
12. Consolidar cambios solamente después de identificar la arquitectura canónica.

## Regla de seguridad

No eliminar componentes aparentemente duplicados hasta demostrar mediante referencias de código, build y dependencias de Supabase que no son utilizados. La consolidación se realizará en esta rama aislada y no sobre `main`.
